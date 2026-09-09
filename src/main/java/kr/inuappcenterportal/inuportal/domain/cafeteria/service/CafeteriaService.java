package kr.inuappcenterportal.inuportal.domain.cafeteria.service;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.InucoopMenuParser;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopMenuRow;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopWeeklyMenu;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.jsoup.Jsoup;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CafeteriaService {

    private final RedisService redisService;
    private final InucoopMenuParser inucoopMenuParser;

    private static final String BASE_URL = "https://www.inucoop.com/main.php?mkey=2&w=2&l=";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; inu-portal-server/1.0)";
    private static final int TIMEOUT_MILLIS = 10_000;

    private static final int DAYS_OF_WEEK = 7;
    private static final int BREAKFAST = 1;
    private static final int LUNCH = 2;
    private static final int DINNER = 3;
    private static final String NOT_OPERATED = "-";

    /** 생협 식단표의 끼니 행 하나를 어떤 식당의 어떤 끼니 슬롯으로 저장할지. */
    private record Corner(String rowLabel, String cafeteria, int slot) {}

    /**
     * 생협 주간식단 페이지(l 파라미터) 하나와 그 안의 끼니 행 매핑.
     * fixedLunchMenu 는 주간식단 없이 고정 메뉴판으로 운영하는 식당의 중식 메뉴다.
     */
    private record MenuPage(int pageNo, String name, List<Corner> corners, String fixedLunchMenu) {
        private MenuPage(int pageNo, String name, List<Corner> corners) {
            this(pageNo, name, corners, null);
        }
    }

    /** 27호관식당은 주간식단을 올리지 않고 상시 메뉴로 운영한다. */
    private static final String CAFETERIA_27_FIXED_MENU = """
            [비빔밥·돈가스]
            제육야채비빔밥 8,500원(구성원 7,500원)
            주꾸미비빔밥 8,500원(구성원 7,500원)
            육회비빔밥 8,500원(구성원 7,500원)
            돈까스 10,900원(구성원 8,900원/조합원 6,900원)

            [국밥]
            순대국밥(다대기O) 7,500원(구성원 6,500원)
            얼큰국밥(얼큰다대기O) 7,500원(구성원 6,500원)
            수육국밥(다대기O) 7,500원(구성원 6,500원)""";

    private static final List<MenuPage> MENU_PAGES = List.of(
            new MenuPage(1, "학생식당", List.of(
                    // 기존 API 호환을 위해 학생식당 = 1코너(백반)을 그대로 유지한다.
                    new Corner("중식(백반)", "학생식당", LUNCH),
                    new Corner("석식", "학생식당", DINNER),
                    new Corner("중식(백반)", "학생식당 1코너(백반)", LUNCH),
                    new Corner("석식", "학생식당 1코너(백반)", DINNER),
                    new Corner("중식(일품)", "학생식당 2코너(일품)", LUNCH),
                    new Corner("국밥", "학생식당 국밥", LUNCH),
                    new Corner("4코너(뒤쪽)", "학생식당 4코너(일품)", LUNCH),
                    new Corner("5코너(뒤쪽)", "학생식당 5코너(고급일품)", LUNCH)
            )),
            new MenuPage(2, "2호관(교직원)식당", List.of(
                    new Corner("중식", "2호관(교직원)식당", LUNCH),
                    new Corner("석식", "2호관(교직원)식당", DINNER)
            )),
            new MenuPage(3, "제1기숙사식당", List.of(
                    new Corner("조식", "제1기숙사식당", BREAKFAST),
                    new Corner("중식", "제1기숙사식당", LUNCH),
                    new Corner("석식", "제1기숙사식당", DINNER)
            )),
            // 27호관식당은 상시 메뉴로 운영한다. 주간식단이 다시 올라오면 A코너 행이 이를 덮어쓰고,
            // B코너 행은 매핑이 없어 경고로 드러난다.
            new MenuPage(4, "27호관식당", List.of(
                    new Corner("A코너 중식", "27호관식당", LUNCH)
            ), CAFETERIA_27_FIXED_MENU),
            new MenuPage(5, "사범대식당", List.of(
                    new Corner("중식", "사범대식당", LUNCH),
                    new Corner("석식", "사범대식당", DINNER)
            ))
    );

    @Async("crawlerExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void initCafeteria() {
        crawlCafeteria();
    }

    @Scheduled(cron = "0 10 0 ? * MON-SAT")
    @SchedulerLock(
            name = "cafeteria-menu",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    public void jobCafeteria() {
        crawlCafeteria();
    }

    public List<String> getCafeteria(String cafeteria, int day) {
        if (day == 0) {
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            day = dayOfWeek.getValue();
        }
        List<String> menu = new ArrayList<>();
        for (int slot = BREAKFAST; slot <= DINNER; slot++) {
            menu.add(redisService.getMeal(cafeteria, day, slot));
        }
        return menu;
    }

    public void crawlCafeteria() {
        for (MenuPage page : MENU_PAGES) {
            try {
                crawlMenuPage(page);
            } catch (IOException | RuntimeException exception) {
                log.warn("식단 크롤링 실패 : {} - {}", page.name(), exception.getMessage());
            }
        }
    }

    private void crawlMenuPage(MenuPage page) throws IOException {
        InucoopWeeklyMenu weeklyMenu = inucoopMenuParser.parse(
                Jsoup.connect(BASE_URL + page.pageNo())
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MILLIS)
                        .get()
        );

        if (weeklyMenu.rows().isEmpty() && page.fixedLunchMenu() != null) {
            storeFixedMenu(page);
            return;
        }

        for (String cafeteria : cafeteriasOf(page)) {
            for (int slot = BREAKFAST; slot <= DINNER; slot++) {
                storeSlot(page, weeklyMenu, cafeteria, slot);
            }
        }
        log.info("{} 저장 완료. 주간={}, 끼니={}", page.name(), weeklyMenu.weekRange(), weeklyMenu.labels());
        warnUnmappedRows(page, weeklyMenu);
    }

    /** 식단표에 새 코너가 생기면 매핑이 없어 조용히 누락된다. 로그로 드러낸다. */
    private void warnUnmappedRows(MenuPage page, InucoopWeeklyMenu weeklyMenu) {
        List<String> mapped = page.corners().stream().map(Corner::rowLabel).toList();
        List<String> unmapped = weeklyMenu.labels().stream()
                .filter(label -> !mapped.contains(label))
                .toList();
        if (!unmapped.isEmpty()) {
            log.warn("매핑되지 않은 식단표 끼니가 있습니다. 식당={}, 끼니={}", page.name(), unmapped);
        }
    }

    /** 상시 메뉴 식당은 평일 중식만 운영한다. */
    private void storeFixedMenu(MenuPage page) {
        storeEveryDay(page.name(), BREAKFAST, NOT_OPERATED);
        storeEveryDay(page.name(), DINNER, NOT_OPERATED);
        for (int day = 1; day <= DAYS_OF_WEEK; day++) {
            boolean weekend = day >= DayOfWeek.SATURDAY.getValue();
            redisService.storeMeal(page.name(), day, LUNCH, weekend ? InucoopMenuParser.CLOSED_MENU : page.fixedLunchMenu());
        }
        log.info("{} 상시 메뉴 저장 완료.", page.name());
    }

    private Set<String> cafeteriasOf(MenuPage page) {
        return page.corners().stream()
                .map(Corner::cafeteria)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 매핑이 없는 끼니는 미운영("-"), 매핑은 있으나 식단표에 행이 없으면 휴무로 저장한다. */
    private void storeSlot(MenuPage page, InucoopWeeklyMenu weeklyMenu, String cafeteria, int slot) {
        Optional<Corner> corner = page.corners().stream()
                .filter(candidate -> candidate.cafeteria().equals(cafeteria) && candidate.slot() == slot)
                .findFirst();
        if (corner.isEmpty()) {
            storeEveryDay(cafeteria, slot, NOT_OPERATED);
            return;
        }
        Optional<InucoopMenuRow> row = weeklyMenu.findRow(corner.get().rowLabel());
        if (row.isEmpty()) {
            log.warn("식단표에서 끼니 행을 찾지 못했습니다. cafeteria={}, 행={}", cafeteria, corner.get().rowLabel());
            storeEveryDay(cafeteria, slot, InucoopMenuParser.CLOSED_MENU);
            return;
        }
        for (int day = 1; day <= DAYS_OF_WEEK; day++) {
            redisService.storeMeal(cafeteria, day, slot, row.get().menuOf(day));
        }
    }

    private void storeEveryDay(String cafeteria, int slot, String menu) {
        for (int day = 1; day <= DAYS_OF_WEEK; day++) {
            redisService.storeMeal(cafeteria, day, slot, menu);
        }
    }
}
