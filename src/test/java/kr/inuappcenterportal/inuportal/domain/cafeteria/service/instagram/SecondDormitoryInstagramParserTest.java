package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.SecondDormitoryDailyMenu;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

class SecondDormitoryInstagramParserTest {

    private final SecondDormitoryInstagramParser parser = new SecondDormitoryInstagramParser();
    private final ZoneId zoneId = ZoneId.of("Asia/Seoul");

    @Test
    void parseTodayMergesLunchAndDinnerPosts() {
        LocalDate today = LocalDate.of(2026, 4, 13);
        List<InstagramMenuPost> posts = List.of(
                new InstagramMenuPost(
                        "https://example.com/lunch",
                        "오늘 점심\n떡갈비\n샐러드\n추가밥 있습니다^^",
                        OffsetDateTime.parse("2026-04-13T02:15:00Z")
                ),
                new InstagramMenuPost(
                        "https://example.com/dinner",
                        "오늘 저녁\n제육볶음\n미역국\n좋은 하루 보내세요!",
                        OffsetDateTime.parse("2026-04-13T09:30:00Z")
                )
        );

        SecondDormitoryDailyMenu menu = parser.parseToday(posts, today, zoneId);

        Assertions.assertEquals("떡갈비 샐러드", menu.lunchMenu());
        Assertions.assertEquals("제육볶음 미역국", menu.dinnerMenu());
    }

    @Test
    void parseTodayFallsBackToPostTimeWhenMealHeaderIsMissing() {
        LocalDate today = LocalDate.of(2026, 4, 13);
        List<InstagramMenuPost> posts = List.of(
                new InstagramMenuPost(
                        "https://example.com/lunch",
                        "오늘 메뉴\n김치볶음밥\n유산균음료",
                        OffsetDateTime.parse("2026-04-13T03:00:00Z")
                )
        );

        SecondDormitoryDailyMenu menu = parser.parseToday(posts, today, zoneId);

        Assertions.assertEquals("김치볶음밥 유산균음료", menu.lunchMenu());
        Assertions.assertNull(menu.dinnerMenu());
    }

    @Test
    void parseTodayMarksClosedMeal() {
        LocalDate today = LocalDate.of(2026, 4, 13);
        List<InstagramMenuPost> posts = List.of(
                new InstagramMenuPost(
                        "https://example.com/dinner",
                        "오늘 저녁\n석식 쉽니다",
                        OffsetDateTime.parse("2026-04-13T10:00:00Z")
                )
        );

        SecondDormitoryDailyMenu menu = parser.parseToday(posts, today, zoneId);

        Assertions.assertNull(menu.lunchMenu());
        Assertions.assertEquals("오늘은 쉽니다", menu.dinnerMenu());
    }

    @Test
    void parseWeeklyMenusKeepsOnlyCurrentWeekDates() {
        LocalDate referenceDate = LocalDate.of(2026, 4, 14);
        List<InstagramMenuPost> posts = List.of(
                new InstagramMenuPost(
                        "https://example.com/monday-lunch",
                        "오늘 점심\n치킨마요덮밥\n유부장국",
                        OffsetDateTime.parse("2026-04-13T01:32:13Z")
                ),
                new InstagramMenuPost(
                        "https://example.com/monday-dinner",
                        "오늘 저녁\n로제크림스파게티\n크림스프",
                        OffsetDateTime.parse("2026-04-13T07:48:59Z")
                ),
                new InstagramMenuPost(
                        "https://example.com/friday-lunch",
                        "오늘 점심\n제육볶음\n콜라",
                        OffsetDateTime.parse("2026-04-09T02:03:36Z")
                )
        );

        Map<LocalDate, SecondDormitoryDailyMenu> menus = parser.parseWeeklyMenus(posts, referenceDate, zoneId);

        Assertions.assertEquals(1, menus.size());
        Assertions.assertTrue(menus.containsKey(LocalDate.of(2026, 4, 13)));
        Assertions.assertEquals("치킨마요덮밥 유부장국", menus.get(LocalDate.of(2026, 4, 13)).lunchMenu());
        Assertions.assertEquals("로제크림스파게티 크림스프", menus.get(LocalDate.of(2026, 4, 13)).dinnerMenu());
    }
}
