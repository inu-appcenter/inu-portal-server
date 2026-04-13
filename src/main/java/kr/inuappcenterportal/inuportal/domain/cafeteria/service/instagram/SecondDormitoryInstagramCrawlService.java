package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.SecondDormitoryInstagramPythonRunner.PythonRunResult;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.SecondDormitoryDailyMenu;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecondDormitoryInstagramCrawlService {

    private static final String CAFETERIA_NAME = "2기숙사 식당";
    private static final String DEFAULT_MENU = "업데이트 전";
    private static final String BREAKFAST_NOT_AVAILABLE = "-";
    private static final String WEEK_META_KEY = "cafeteria:second-dormitory:week";

    private final RedisService redisService;
    private final SecondDormitoryInstagramParser parser;
    private final SecondDormitoryInstagramProperties properties;
    private final SecondDormitoryInstagramPythonRunner pythonRunner;
    private final SecondDormitoryInstagramOutputReader outputReader;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void initSecondDormitoryInstagramCafeteria() {
        refreshMenus("startup");
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.morning-cron:0 0 9 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshMorningMenus() {
        refreshMenus("morning");
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.afternoon-cron:0 0 17 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshAfternoonMenus() {
        refreshMenus("afternoon");
    }

    public void refreshMenus(String trigger) {
        LocalDate today = null;
        List<InstagramMenuPost> posts = List.of();
        Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus = Map.of();
        PythonRunResult runResult = null;

        if (!properties.isEnabled()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("2기숙사 식당 인스타 메뉴가 이미 실행 중입니다. trigger={}", trigger);
            return;
        }

        try {
            today = LocalDate.now(properties.resolveZoneId());
            runResult = pythonRunner.runScraper();
            logPythonRunResult(runResult);

            if (!runResult.isSuccess()) {
                throw new IllegalStateException("Python scraper exited with code " + runResult.exitCode());
            }

            posts = outputReader.readHistoryPosts(runResult.historyFile());
            logCollectedPosts(posts, runResult.historyFile().toString());

            weeklyMenus = parser.parseWeeklyMenus(posts, today, properties.resolveZoneId());
            logParsedWeeklyMenus(today, posts, weeklyMenus);

            if (weeklyMenus.isEmpty()) {
                log.warn("2기숙사 식당 현재 주차 메뉴를 찾지 못했습니다. trigger={}", trigger);
                return;
            }

            try {
                ensureWeekInitialized(today);
                saveWeeklyMenus(weeklyMenus);
                logTodayMenuStatus(today, weeklyMenus);
                log.info("2기숙사 식당 메뉴 저장 완료. trigger={}, savedDates={}", trigger, weeklyMenus.keySet());
            } catch (Exception e) {
                logFailureContext(trigger, today, posts, weeklyMenus, runResult);
                log.warn("2기숙사 식당 인스타 메뉴 저장 실패. trigger={}, message={}", trigger, e.getMessage(), e);
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logFailureContext(trigger, today, posts, weeklyMenus, runResult);
            log.warn("2기숙사 식당 Python 실행 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } catch (Exception e) {
            logFailureContext(trigger, today, posts, weeklyMenus, runResult);
            log.warn("2기숙사 식당 인스타 메뉴 크롤링 또는 파싱 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } finally {
            cleanupUnusedGeneratedFiles(runResult);
            running.set(false);
        }
    }

    private void logPythonRunResult(PythonRunResult runResult) {
        log.info(
                "2기숙사 식당 Python 실행 완료. exitCode={}, historyFile={}",
                runResult.exitCode(),
                runResult.historyFile()
        );

        if (runResult.stdout() != null && !runResult.stdout().isBlank()) {
            log.info("2기숙사 식당 Python stdout={}", sanitizeForLog(runResult.stdout()));
        }
        if (runResult.stderr() != null && !runResult.stderr().isBlank()) {
            log.warn("2기숙사 식당 Python stderr={}", sanitizeForLog(runResult.stderr()));
        }
    }

    private void ensureWeekInitialized(LocalDate today) {
        String currentWeek = today.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                + String.format("%02d", today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        String savedWeek = redisService.getValue(WEEK_META_KEY);
        if (currentWeek.equals(savedWeek)) {
            return;
        }

        log.info(
                "2기숙사 식당 주간 초기화 시작. savedWeek={}, currentWeek={}, defaultLunchDinner={}, breakfast={}",
                savedWeek,
                currentWeek,
                DEFAULT_MENU,
                BREAKFAST_NOT_AVAILABLE
        );

        for (int day = 1; day <= 7; day++) {
            storeMealWithLog(day, 1, "조식", BREAKFAST_NOT_AVAILABLE);
            storeMealWithLog(day, 2, "중식", DEFAULT_MENU);
            storeMealWithLog(day, 3, "석식", DEFAULT_MENU);
        }

        redisService.storeValue(WEEK_META_KEY, currentWeek);
        log.info("2기숙사 식당 주간 초기화 완료. currentWeek={}", currentWeek);
    }

    private void saveWeeklyMenus(Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus) {
        for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
            saveDailyMenu(entry.getKey(), entry.getValue());
        }
    }

    private void saveDailyMenu(LocalDate menuDate, SecondDormitoryDailyMenu menu) {
        int day = menuDate.getDayOfWeek().getValue();
        log.info(
                "2기숙사 식당 Redis 저장 준비. menuDate={}, day={}, lunchMenu={}, dinnerMenu={}",
                menuDate,
                day,
                sanitizeForLog(menu.lunchMenu()),
                sanitizeForLog(menu.dinnerMenu())
        );

        storeMealWithLog(day, 1, "조식", BREAKFAST_NOT_AVAILABLE);
        if (menu.hasLunch()) {
            storeMealWithLog(day, 2, "중식", menu.lunchMenu());
        }
        if (menu.hasDinner()) {
            storeMealWithLog(day, 3, "석식", menu.dinnerMenu());
        }
    }

    private void storeMealWithLog(int day, int slot, String mealType, String value) {
        log.info(
                "2기숙사 식당 Redis 저장 시도. day={}, slot={}, mealType={}, value={}",
                day,
                slot,
                mealType,
                sanitizeForLog(value)
        );
        try {
            redisService.storeMeal(CAFETERIA_NAME, day, slot, value);
            log.info(
                    "2기숙사 식당 Redis 저장 성공. day={}, slot={}, mealType={}, value={}",
                    day,
                    slot,
                    mealType,
                    sanitizeForLog(value)
            );
        } catch (Exception e) {
            log.warn(
                    "2기숙사 식당 Redis 저장 실패. day={}, slot={}, mealType={}, value={}, message={}",
                    day,
                    slot,
                    mealType,
                    sanitizeForLog(value),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    private void logCollectedPosts(List<InstagramMenuPost> posts, String historyFilePath) {
        log.info("2기숙사 식당 게시물 수집 완료. count={}, historyFile={}", posts.size(), historyFilePath);
        for (InstagramMenuPost post : posts) {
            log.info(
                    "2기숙사 식당 수집 게시물. postUrl={}, publishedAt={}, caption={}",
                    post.postUrl(),
                    post.publishedAt(),
                    sanitizeForLog(post.caption())
            );
        }
    }

    private void logParsedWeeklyMenus(
            LocalDate today,
            List<InstagramMenuPost> posts,
            Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus
    ) {
        SecondDormitoryDailyMenu todayMenu = weeklyMenus.getOrDefault(today, SecondDormitoryDailyMenu.empty());
        long todayPostCount = posts.stream()
                .filter(post -> post.publishedAt() != null)
                .filter(post -> post.publishedAt().atZoneSameInstant(properties.resolveZoneId()).toLocalDate().equals(today))
                .count();

        log.info(
                "2기숙사 식당 오늘 메뉴 파싱 완료. date={}, todayPostCount={}, lunchMenu={}, dinnerMenu={}",
                today,
                todayPostCount,
                sanitizeForLog(todayMenu.lunchMenu()),
                sanitizeForLog(todayMenu.dinnerMenu())
        );

        for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
            log.info(
                    "2기숙사 식당 날짜별 메뉴 파싱 완료. menuDate={}, lunchMenu={}, dinnerMenu={}",
                    entry.getKey(),
                    sanitizeForLog(entry.getValue().lunchMenu()),
                    sanitizeForLog(entry.getValue().dinnerMenu())
            );
        }
    }

    private void logTodayMenuStatus(LocalDate today, Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus) {
        SecondDormitoryDailyMenu todayMenu = weeklyMenus.getOrDefault(today, SecondDormitoryDailyMenu.empty());
        if (!todayMenu.hasAnyMenu()) {
            log.warn("2기숙사 식당 오늘 메뉴는 아직 없지만, 같은 주차의 이전 게시글은 저장했습니다. date={}", today);
        }
    }

    private void logFailureContext(
            String trigger,
            LocalDate today,
            List<InstagramMenuPost> posts,
            Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus,
            PythonRunResult runResult
    ) {
        log.warn(
                "2기숙사 식당 실패 컨텍스트. trigger={}, date={}, collectedPostCount={}, parsedDates={}",
                trigger,
                today,
                posts == null ? 0 : posts.size(),
                weeklyMenus == null ? List.of() : weeklyMenus.keySet()
        );

        if (runResult != null) {
            log.warn(
                    "2기숙사 식당 Python 실행 컨텍스트. exitCode={}, historyFile={}, stdout={}, stderr={}",
                    runResult.exitCode(),
                    runResult.historyFile(),
                    sanitizeForLog(runResult.stdout()),
                    sanitizeForLog(runResult.stderr())
            );
        }

        if (weeklyMenus != null && !weeklyMenus.isEmpty()) {
            for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
                log.warn(
                        "2기숙사 식당 실패 시 파싱 메뉴. menuDate={}, lunchMenu={}, dinnerMenu={}",
                        entry.getKey(),
                        sanitizeForLog(entry.getValue().lunchMenu()),
                        sanitizeForLog(entry.getValue().dinnerMenu())
                );
            }
        }

        if (posts == null || posts.isEmpty()) {
            return;
        }

        for (InstagramMenuPost post : posts) {
            log.warn(
                    "2기숙사 식당 실패 시 수집 게시물. postUrl={}, publishedAt={}, caption={}",
                    post.postUrl(),
                    post.publishedAt(),
                    sanitizeForLog(post.caption())
            );
        }
    }

    private void cleanupUnusedGeneratedFiles(PythonRunResult runResult) {
        if (runResult == null) {
            return;
        }

        Path historyDir = runResult.historyFile().getParent();
        if (historyDir == null) {
            return;
        }

        Path debugCaptionFile = historyDir.resolve("debug_caption.json");
        deleteIfExists(debugCaptionFile, "debug caption output");
    }

    private void deleteIfExists(Path path, String label) {
        try {
            if (Files.deleteIfExists(path)) {
                log.info("2기숙사 식당 사용하지 않는 Python 생성 파일 삭제 완료. label={}, path={}", label, path);
            }
        } catch (IOException e) {
            log.warn(
                    "2기숙사 식당 사용하지 않는 Python 생성 파일 삭제 실패. label={}, path={}, message={}",
                    label,
                    path,
                    e.getMessage()
            );
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "\\r").replace("\n", "\\n").trim();
    }
}
