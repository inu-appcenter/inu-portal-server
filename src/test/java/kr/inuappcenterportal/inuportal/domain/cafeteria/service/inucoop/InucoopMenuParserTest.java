package kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopMenuRow;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopWeeklyMenu;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class InucoopMenuParserTest {

    private static final int MONDAY = 1;
    private static final int TUESDAY = 2;
    private static final int THURSDAY = 4;
    private static final int FRIDAY = 5;
    private static final int SATURDAY = 6;
    private static final int SUNDAY = 7;

    private final InucoopMenuParser parser = new InucoopMenuParser();

    @Test
    void parseReadsEveryCornerOfStudentCafeteria() throws IOException {
        InucoopWeeklyMenu weeklyMenu = parser.parse(readFixture("student.html"));

        Assertions.assertEquals("2026-09-07 ~ 2026-09-13", weeklyMenu.weekRange());
        Assertions.assertEquals(
                java.util.List.of("중식(백반)", "중식(일품)", "석식", "국밥", "4코너(뒤쪽)", "5코너(뒤쪽)"),
                weeklyMenu.labels()
        );

        InucoopMenuRow lunch = weeklyMenu.findRow("중식(백반)").orElseThrow();
        Assertions.assertEquals(7, lunch.menus().size());
        Assertions.assertTrue(lunch.menuOf(MONDAY).startsWith("참치김치찌개"));
        Assertions.assertFalse(lunch.menuOf(MONDAY).contains("\""));
    }

    @Test
    void parseMergesQuotedMultilineItems() throws IOException {
        InucoopMenuRow gukbap = parser.parse(readFixture("student.html")).findRow("국밥").orElseThrow();

        Assertions.assertEquals(
                """
                (pork)수육국밥 / 순대국밥 / 얼큰국밥
                양파초절임 / 배추김치
                7,500(구성원 6,500원)
                1153kcal 1210kcal 1233kcal""",
                gukbap.menuOf(MONDAY)
        );
    }

    @Test
    void parseSplitsChoiceBlocksOfBuilding2() throws IOException {
        InucoopMenuRow lunch = parser.parse(readFixture("building2.html")).findRow("중식").orElseThrow();

        Assertions.assertEquals(
                """
                [선택1] 제육볶음(pork), 콩나물국
                [선택2] 닭칼국수

                [공통]
                해물완자전*케찹
                스모크햄마늘종볶음
                고사리나물
                배추김치
                쌀밥
                토핑)요거트(블루베리/시리얼/아몬드)
                8,000원(구성원 7,000원)
                1,522/1,871kcal""",
                lunch.menuOf(MONDAY)
        );
    }

    @Test
    void parseKeepsMultilineMainMenuAsSingleChoice() throws IOException {
        InucoopMenuRow lunch = parser.parse(readFixture("building2.html")).findRow("중식").orElseThrow();

        Assertions.assertTrue(
                lunch.menuOf(TUESDAY).startsWith("[선택1] 토핑)일식카레라이스 (비엔나/미니해쉬), 유부우동국물\n[선택2] 삼겹살김치찌개(pork)\n"),
                lunch.menuOf(TUESDAY)
        );
    }

    @Test
    void parseMarksDaysWithoutMenuAsClosed() throws IOException {
        InucoopMenuRow dinner = parser.parse(readFixture("building2.html")).findRow("석식").orElseThrow();

        Assertions.assertEquals(InucoopMenuParser.CLOSED_MENU, dinner.menuOf(SATURDAY));
    }

    @Test
    void parseReturnsNoRowsWhenWeeklyMenuIsNotRegistered() throws IOException {
        InucoopWeeklyMenu weeklyMenu = parser.parse(readFixture("building27.html"));

        Assertions.assertEquals("2026-09-07 ~ 2026-09-13", weeklyMenu.weekRange());
        Assertions.assertTrue(weeklyMenu.rows().isEmpty());
    }

    private Document readFixture(String fileName) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("inucoop/" + fileName)) {
            Assertions.assertNotNull(stream, fileName + " 픽스처가 없습니다.");
            return Jsoup.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
    @Test
    void parseSplitsChoiceEvenWhenMainMenuIsBrokenIntoExtraLines() throws IOException {
        // 08/31 주 목요일: "돼지고기감자 / 짜글이덮밥(pork)"이 두 줄로 끊겨 있고 칼로리는 두 개다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2-20260831.html")).findRow("중식").orElseThrow();

        Assertions.assertTrue(
                lunch.menuOf(THURSDAY).startsWith("""
                        [선택1] 냉모밀*새우튀김
                        [선택2] 돼지고기감자 짜글이덮밥(pork), 시래기된장국
                        """),
                lunch.menuOf(THURSDAY)
        );
    }

    @Test
    void parseSplitsChoiceWhenChoiceSideIsAboveBlankLine() throws IOException {
        // 08/24 주 목요일: 빈 줄 위에 (선택2) 반찬이 섞여 있다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2-20260824.html")).findRow("중식").orElseThrow();

        Assertions.assertTrue(
                lunch.menuOf(THURSDAY).startsWith("""
                        [선택1] 나주곰탕&소면
                        [선택2] 오징어순대볶음(pork), 온육수
                        """),
                lunch.menuOf(THURSDAY)
        );
    }

    @Test
    void parseSplitsChoiceWhenCellHasNoBlankLine() throws IOException {
        // 07/06 주 화요일: 메인과 반찬 사이에 빈 줄이 아예 없다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2-20260706.html")).findRow("중식").orElseThrow();

        Assertions.assertTrue(
                lunch.menuOf(TUESDAY).startsWith("""
                        [선택1] 우사태메추리알조림, 호박새우젓국
                        [선택2] 냉모밀*새우튀김
                        """),
                lunch.menuOf(TUESDAY)
        );
    }

    @Test
    void parseMergesParenthesisOnlyTailLineIntoMainMenu() throws IOException {
        // 08/10 주 금요일: "삼겹살김치구이" 다음 줄에 "(pork)"만 있다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2-20260810.html")).findRow("중식").orElseThrow();

        Assertions.assertTrue(
                lunch.menuOf(FRIDAY).startsWith("""
                        [선택1] 말복)장각삼계탕
                        [선택2] 삼겹살김치구이 (pork), 우동국물
                        """),
                lunch.menuOf(FRIDAY)
        );
    }

    @Test
    void parseReadsSemesterCornersOf27() throws IOException {
        InucoopWeeklyMenu weeklyMenu = parser.parse(readFixture("building27-20260601.html"));

        Assertions.assertEquals(java.util.List.of("A코너 중식", "B코너 중식"), weeklyMenu.labels());
        Assertions.assertNotEquals(
                InucoopMenuParser.CLOSED_MENU,
                weeklyMenu.findRow("B코너 중식").orElseThrow().menuOf(MONDAY)
        );
    }

    @Test
    void parseKeepsMondayToSundayOrder() throws IOException {
        // 토·일은 휴점이라 항상 마지막 두 칸이어야 한다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2.html")).findRow("중식").orElseThrow();

        Assertions.assertNotEquals(InucoopMenuParser.CLOSED_MENU, lunch.menuOf(MONDAY));
        Assertions.assertEquals(InucoopMenuParser.CLOSED_MENU, lunch.menuOf(SATURDAY));
        Assertions.assertEquals(InucoopMenuParser.CLOSED_MENU, lunch.menuOf(SUNDAY));
    }
    @Test
    void parseSplitsChoiceWithoutMarkerWhenCaloriesAreListedPerChoice() throws IOException {
        // 08/31 주 월요일: (선택N) 표기가 없고 "1,371/1,559kcal"만으로 택1임을 알 수 있다.
        InucoopMenuRow lunch = parser.parse(readFixture("building2-20260831.html")).findRow("중식").orElseThrow();

        Assertions.assertEquals(
                """
                [선택1] 마파두부덮밥(pork)
                [선택2] 돈육폭찹스테이크(pork)

                [공통]
                맑은무국
                목화솜탕수육(pork)
                굴소스가지볶음
                푸실리오리엔탈샐러드
                배추김치
                쌀밥
                아이스티
                8,000원(구성원 7,000원)
                1,371/1,559kcal""",
                lunch.menuOf(MONDAY)
        );
    }

    @Test
    void parseKeepsSingleMenuCellUnsplit() throws IOException {
        // 칼로리가 하나뿐인 석식 칸은 선택 서식을 적용하지 않는다.
        InucoopMenuRow dinner = parser.parse(readFixture("building2.html")).findRow("석식").orElseThrow();

        Assertions.assertFalse(dinner.menuOf(MONDAY).contains("[선택"), dinner.menuOf(MONDAY));
        Assertions.assertTrue(dinner.menuOf(MONDAY).startsWith("고추짜장밥(pork)"), dinner.menuOf(MONDAY));
    }
    @Test
    void parseSplitsChoiceWithoutMarkerAndWithoutBlankLine() {
        // 마커도 빈 줄도 없이 칼로리만 두 개인 칸: 묶은 항목 앞 두 개를 메인으로 본다.
        String cell = "제육볶음(pork)<br />토핑)일식카레라이스<br />(비엔나/미니해쉬)<br />"
                + "맑은무국<br />배추김치<br />쌀밥<br />8,000원(구성원 7,000원)<br /> 1,371/1,559kcal <br />";

        InucoopMenuRow lunch = parser.parse(Jsoup.parse(tableWith(cell))).findRow("중식").orElseThrow();

        Assertions.assertEquals(
                """
                [선택1] 제육볶음(pork)
                [선택2] 토핑)일식카레라이스 (비엔나/미니해쉬)

                [공통]
                맑은무국
                배추김치
                쌀밥
                8,000원(구성원 7,000원)
                1,371/1,559kcal""",
                lunch.menuOf(MONDAY)
        );
    }

    private String tableWith(String mondayCell) {
        StringBuilder table = new StringBuilder("<div id=\"detail_left\"><table><tr>");
        for (String day : java.util.List.of("월", "화", "수", "목", "금", "토", "일")) {
            table.append("<td class='din_mn'>").append(day).append(" <span>(09/07)</span></td>");
        }
        table.append("</tr><tr><td class=\"corn_nm\">중식<br><span class='menuTime'>11:30~13:30</span></td>");
        table.append("<td class='din_list'>").append(mondayCell).append("</td>");
        for (int day = 2; day <= 7; day++) {
            table.append("<td class='din_list'>❝오늘 등록된 메뉴가 없습니다.❞</td>");
        }
        return table.append("</tr></table></div>").toString();
    }
    @Test
    void parseTreatsInvisibleWhitespaceLineAsSeparator() {
        // 구분 빈 줄이 일반 공백이 아니라 전각 공백/nbsp 인 경우
        String cell = "제육볶음(pork)<br />닭칼국수<br />\u3000<br />"
                + "맑은무국<br />배추김치<br />8,000원(구성원 7,000원)<br /> 1,371/1,559kcal <br />";

        InucoopMenuRow lunch = parser.parse(Jsoup.parse(tableWith(cell))).findRow("중식").orElseThrow();

        Assertions.assertTrue(lunch.menuOf(MONDAY).startsWith("""
                [선택1] 제육볶음(pork)
                [선택2] 닭칼국수
                """), lunch.menuOf(MONDAY));
        Assertions.assertFalse(lunch.menuOf(MONDAY).contains("\u3000"), lunch.menuOf(MONDAY));
    }

    @Test
    void parseReadsRowLabelWrappedInChildTag() {
        // 끼니 이름이 태그 안에 들어가도 읽어야 한다. 못 읽으면 그 식당 전체가 조용히 휴무가 된다.
        String table = tableWith("김치찌개<br />쌀밥<br />").replace(
                "<td class=\"corn_nm\">중식<br>", "<td class=\"corn_nm\"><span>중식</span><br>");

        InucoopWeeklyMenu weeklyMenu = parser.parse(Jsoup.parse(table));

        Assertions.assertEquals(java.util.List.of("중식"), weeklyMenu.labels());
    }
}
