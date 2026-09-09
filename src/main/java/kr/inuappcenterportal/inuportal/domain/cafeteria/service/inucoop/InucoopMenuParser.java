package kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopMenuRow;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.inucoop.dto.InucoopWeeklyMenu;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 인천대 생활협동조합 주간식단표(https://www.inucoop.com/main.php?mkey=2&w=2) HTML 파서.
 * 페이지가 서버 사이드 렌더링이라 브라우저 없이 정적 파싱만으로 충분하다.
 */
@Slf4j
@Component
public class InucoopMenuParser {

    public static final String CLOSED_MENU = "오늘은 쉽니다";

    private static final int DAYS_OF_WEEK = 7;
    private static final String LINE_BREAK_TOKEN = "|~br~|";
    private static final String NO_MENU_MARK = "등록된 메뉴가 없습니다";
    private static final List<String> DAY_NAMES = List.of("월", "화", "수", "목", "금", "토", "일");

    private static final Pattern CHOICE_MARKER = Pattern.compile("\\(\\s*선택\\s*(\\d+)\\s*\\)");
    private static final Pattern KCAL_LINE = Pattern.compile("^([\\d,]+(?:\\s*/\\s*[\\d,]+)*)\\s*kcal", Pattern.CASE_INSENSITIVE);
    /** nbsp, 제로폭공백, 전각공백처럼 눈에 안 보이는 공백. */
    private static final Pattern BLANK_CHARACTERS = Pattern.compile("[\\u00A0\\u200B\\u200C\\u200D\\u3000\\uFEFF]");
    private static final Pattern WEEK_RANGE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s*~\\s*\\d{4}-\\d{2}-\\d{2}");

    public InucoopWeeklyMenu parse(Document document) {
        Element table = document.selectFirst("div#detail_left table");
        if (table == null) {
            log.warn("생협 식단표 테이블을 찾지 못했습니다.");
            return InucoopWeeklyMenu.empty();
        }

        List<Integer> dayOrder = readDayOrder(table);
        List<InucoopMenuRow> rows = new ArrayList<>();
        for (Element row : table.select("tr")) {
            Element label = row.selectFirst("td.corn_nm");
            if (label == null) {
                continue;
            }
            Elements cells = row.select("td.din_list, td.din_lists");
            if (cells.size() != DAYS_OF_WEEK) {
                log.warn("생협 식단표 요일 칸 수가 예상과 다릅니다. 끼니={}, 칸={}", label.ownText(), cells.size());
                continue;
            }
            String[] menus = new String[DAYS_OF_WEEK];
            for (int index = 0; index < DAYS_OF_WEEK; index++) {
                menus[dayOrder.get(index) - 1] = readMenu(cells.get(index));
            }
            rows.add(new InucoopMenuRow(readLabel(label), List.of(menus)));
        }
        return new InucoopWeeklyMenu(extractWeekRange(table), rows);
    }

    /** 끼니 이름은 보통 직접 텍스트지만, 태그로 감싸이면 운영시간 앞의 첫 줄을 쓴다. */
    private String readLabel(Element cell) {
        String label = cell.ownText().trim();
        if (!label.isEmpty()) {
            return label;
        }
        return toLines(cell).stream()
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse("");
    }

    /** 요일 칸이 월~일 순서인지 헤더로 확인한다. 헤더를 읽지 못하면 표시된 순서를 그대로 믿는다. */
    private List<Integer> readDayOrder(Element table) {
        List<Integer> order = table.select("td.din_mn, td.din_mns").stream()
                .map(header -> DAY_NAMES.indexOf(header.ownText().trim()) + 1)
                .filter(day -> day > 0)
                .toList();
        if (order.size() != DAYS_OF_WEEK || order.stream().distinct().count() != DAYS_OF_WEEK) {
            if (!order.isEmpty()) {
                log.warn("생협 식단표 요일 헤더를 읽지 못했습니다. header={}", order);
            }
            return List.of(1, 2, 3, 4, 5, 6, 7);
        }
        return order;
    }

    private String extractWeekRange(Element table) {
        Matcher matcher = WEEK_RANGE.matcher(table.text());
        return matcher.find() ? matcher.group() : null;
    }

    private String readMenu(Element cell) {
        List<String> lines = toLines(cell);
        if (lines.stream().anyMatch(line -> line.contains(NO_MENU_MARK))) {
            return CLOSED_MENU;
        }
        return joinLines(formatChoices(lines));
    }

    private List<String> toLines(Element cell) {
        Element copy = cell.clone();
        copy.select("br").before(LINE_BREAK_TOKEN);
        return Arrays.stream(copy.text().split(Pattern.quote(LINE_BREAK_TOKEN), -1))
                .map(line -> BLANK_CHARACTERS.matcher(line).replaceAll(" ").trim())
                .toList();
    }

    /**
     * 2호관식당처럼 메인 메뉴를 택1 하는 칸의 서식.
     * 메인 메뉴와 반찬의 경계는 첫 (선택N) 줄이고, 마커가 없으면 빈 줄이 경계가 된다.
     * 반찬 중 (선택N)이 붙은 것만 해당 선택 전용이고 나머지는 공통이다.
     */
    private List<String> formatChoices(List<String> lines) {
        int choiceCount = countOfChoices(lines);
        int boundary = indexOfFirstChoiceMarker(lines);
        if (boundary < 0) {
            // 마커 없이 "1,371/1,559kcal"처럼 칼로리만 여러 개인 칸도 택1 메뉴다.
            boundary = choiceCount >= 2 ? indexOfSeparator(lines) : -1;
        }
        List<String> mains = boundary > 0 ? mergeIntoChoices(groupItems(lines.subList(0, boundary)), choiceCount) : List.of();
        if (choiceCount >= 2 && mains.size() != choiceCount) {
            // 경계를 못 잡았으면 줄을 묶은 뒤 앞에서부터 선택지 수만큼을 메인 메뉴로 본다.
            GroupedItems grouped = groupItems(lines);
            if (grouped.items().size() <= choiceCount) {
                return lines;
            }
            mains = grouped.items().subList(0, choiceCount);
            boundary = grouped.endLines().get(choiceCount - 1) + 1;
        }
        if (boundary <= 0 || mains.size() < 2) {
            return lines;
        }

        Map<Integer, List<String>> sidesByChoice = new LinkedHashMap<>();
        List<String> common = new ArrayList<>();
        for (String line : lines.subList(boundary, lines.size())) {
            if (line.isEmpty() && common.isEmpty()) {
                continue;
            }
            Matcher matcher = CHOICE_MARKER.matcher(line);
            if (!matcher.find()) {
                common.add(line);
                continue;
            }
            int choice = Integer.parseInt(matcher.group(1));
            if (choice < 1 || choice > mains.size()) {
                common.add(line);
                continue;
            }
            sidesByChoice.computeIfAbsent(choice, key -> new ArrayList<>()).add(matcher.replaceAll("").trim());
        }

        List<String> formatted = new ArrayList<>();
        for (int choice = 1; choice <= mains.size(); choice++) {
            List<String> menu = new ArrayList<>();
            menu.add(mains.get(choice - 1));
            menu.addAll(sidesByChoice.getOrDefault(choice, List.of()));
            formatted.add("[선택%d] %s".formatted(choice, String.join(", ", menu)));
        }
        formatted.add("");
        formatted.add("[공통]");
        formatted.addAll(common);
        return formatted;
    }

    /** 앞뒤로 메뉴가 있는 빈 줄. 칸 끝의 빈 줄은 경계가 아니다. */
    private int indexOfSeparator(List<String> lines) {
        boolean started = false;
        for (int index = 0; index < lines.size(); index++) {
            if (!lines.get(index).isEmpty()) {
                started = true;
                continue;
            }
            if (started && lines.subList(index, lines.size()).stream().anyMatch(line -> !line.isEmpty())) {
                return index;
            }
        }
        return -1;
    }

    private int indexOfFirstChoiceMarker(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (CHOICE_MARKER.matcher(lines.get(index)).find()) {
                return index;
            }
        }
        return -1;
    }

    /** 선택지 수는 "1,443/1,288kcal"처럼 칼로리를 "/"로 나눠 적은 개수로 알 수 있다. */
    private int countOfChoices(List<String> lines) {
        int count = 0;
        for (String line : lines) {
            Matcher matcher = KCAL_LINE.matcher(line);
            if (matcher.find()) {
                count = matcher.group(1).split("/").length;
            }
        }
        return count;
    }

    /** 묶인 항목과, 각 항목이 원본에서 끝나는 줄 번호. */
    private record GroupedItems(List<String> items, List<Integer> endLines) {}

    /**
     * 메인 메뉴 한 개가 여러 줄로 끊겨 있는 경우를 한 항목으로 묶는다.
     * 원본이 엑셀에서 옮겨진 탓에 큰따옴표로 묶인 항목, 여는 따옴표가 유실된 항목,
     * "(pork)"처럼 괄호로 시작하는 꼬리줄이 섞여 있다.
     */
    private GroupedItems groupItems(List<String> lines) {
        List<String> items = new ArrayList<>();
        List<Integer> endLines = new ArrayList<>();
        boolean quoted = false;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isEmpty()) {
                continue;
            }
            boolean unbalanced = line.chars().filter(character -> character == '"').count() % 2 == 1;
            if (quoted) {
                appendToLast(items, line);
                quoted = !unbalanced;
            } else if (items.isEmpty()) {
                items.add(line);
                endLines.add(index);
                quoted = unbalanced;
            } else if ((unbalanced && line.endsWith("\"")) || line.startsWith("(")) {
                appendToLast(items, line);
            } else {
                items.add(line);
                endLines.add(index);
                quoted = unbalanced;
            }
            endLines.set(items.size() - 1, index);
        }
        return new GroupedItems(items, endLines);
    }

    /** 줄바꿈 위치를 알 수 없는 나머지는 선택지 수에 맞춰 뒤에서부터 붙인다. */
    private List<String> mergeIntoChoices(GroupedItems grouped, int choiceCount) {
        List<String> items = new ArrayList<>(grouped.items());
        while (choiceCount >= 2 && items.size() > choiceCount) {
            appendToLast(items, items.remove(items.size() - 1));
        }
        return items;
    }

    private void appendToLast(List<String> items, String line) {
        items.set(items.size() - 1, items.get(items.size() - 1) + " " + line);
    }

    private String joinLines(List<String> lines) {
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String menu = line.replace("\"", "").replace("\\", "").trim();
            if (menu.isEmpty() && (cleaned.isEmpty() || cleaned.get(cleaned.size() - 1).isEmpty())) {
                continue;
            }
            cleaned.add(menu);
        }
        while (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).isEmpty()) {
            cleaned.remove(cleaned.size() - 1);
        }
        return cleaned.isEmpty() ? CLOSED_MENU : String.join("\n", cleaned);
    }
}
