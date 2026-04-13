package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.SecondDormitoryDailyMenu;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SecondDormitoryInstagramParser {

    private static final String CLOSED_MENU = "오늘은 쉽니다";
    private static final LocalTime DINNER_FALLBACK_TIME = LocalTime.of(15, 0);

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern DATE_LINE_PATTERN = Pattern.compile(
            "^(?:\\d{1,2}[./-]\\d{1,2}(?:[./-]\\d{1,2})?|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})(?:\\([^)]*\\))?$"
    );

    private static final List<String> LUNCH_KEYWORDS = List.of("중식", "점심");
    private static final List<String> DINNER_KEYWORDS = List.of("석식", "저녁");
    private static final List<String> CLOSED_KEYWORDS = List.of("휴무", "운영없음", "운영 안", "없습니다", "쉽니다");
    private static final List<String> NON_MENU_PREFIXES = List.of(
            "#",
            "원산지",
            "알레르기",
            "좋은 하루",
            "맛있게",
            "문의",
            "식단표",
            "추가밥",
            "추가 반찬",
            "준비한 수량"
    );

    public SecondDormitoryDailyMenu parseToday(List<InstagramMenuPost> posts, LocalDate today, ZoneId zoneId) {
        return parseWeeklyMenus(posts, today, zoneId).getOrDefault(today, SecondDormitoryDailyMenu.empty());
    }

    public Map<LocalDate, SecondDormitoryDailyMenu> parseWeeklyMenus(
            List<InstagramMenuPost> posts,
            LocalDate referenceDate,
            ZoneId zoneId
    ) {
        Map<LocalDate, List<InstagramMenuPost>> postsByDate = posts.stream()
                .filter(post -> post.publishedAt() != null)
                .filter(post -> isSameWeek(referenceDate, post.publishedAt().atZoneSameInstant(zoneId).toLocalDate()))
                .sorted(Comparator.comparing(InstagramMenuPost::publishedAt))
                .collect(Collectors.groupingBy(
                        post -> post.publishedAt().atZoneSameInstant(zoneId).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<LocalDate, SecondDormitoryDailyMenu> parsedMenus = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, List<InstagramMenuPost>> entry : postsByDate.entrySet()) {
            String lunchMenu = null;
            String dinnerMenu = null;

            for (InstagramMenuPost post : entry.getValue()) {
                ParsedSections sections = parseCaption(post.caption(), post.publishedAt().atZoneSameInstant(zoneId).toLocalTime());
                if (sections.lunchMenu() != null) {
                    lunchMenu = sections.lunchMenu();
                }
                if (sections.dinnerMenu() != null) {
                    dinnerMenu = sections.dinnerMenu();
                }
            }

            SecondDormitoryDailyMenu menu = new SecondDormitoryDailyMenu(lunchMenu, dinnerMenu);
            if (menu.hasAnyMenu()) {
                parsedMenus.put(entry.getKey(), menu);
            }
        }

        return parsedMenus;
    }

    private boolean isSameWeek(LocalDate referenceDate, LocalDate candidateDate) {
        return referenceDate.get(IsoFields.WEEK_BASED_YEAR) == candidateDate.get(IsoFields.WEEK_BASED_YEAR)
                && referenceDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == candidateDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private ParsedSections parseCaption(String caption, LocalTime publishedTime) {
        List<String> lines = normalizeLines(caption);
        if (lines.isEmpty()) {
            return ParsedSections.empty();
        }

        int lunchHeaderIndex = findMealHeaderIndex(lines, MealType.LUNCH);
        int dinnerHeaderIndex = findMealHeaderIndex(lines, MealType.DINNER);

        if (lunchHeaderIndex >= 0 && dinnerHeaderIndex >= 0) {
            if (lunchHeaderIndex < dinnerHeaderIndex) {
                return new ParsedSections(
                        extractMenu(lines.subList(lunchHeaderIndex + 1, dinnerHeaderIndex)),
                        extractMenu(lines.subList(dinnerHeaderIndex + 1, lines.size()))
                );
            }

            return new ParsedSections(
                    extractMenu(lines.subList(lunchHeaderIndex + 1, lines.size())),
                    extractMenu(lines.subList(dinnerHeaderIndex + 1, lunchHeaderIndex))
            );
        }

        if (lunchHeaderIndex >= 0) {
            return new ParsedSections(extractMenu(lines.subList(lunchHeaderIndex + 1, lines.size())), null);
        }

        if (dinnerHeaderIndex >= 0) {
            return new ParsedSections(null, extractMenu(lines.subList(dinnerHeaderIndex + 1, lines.size())));
        }

        String menu = extractMenu(trimLeadingTitleLines(lines));
        if (menu == null) {
            return ParsedSections.empty();
        }

        if (publishedTime.isBefore(DINNER_FALLBACK_TIME)) {
            return new ParsedSections(menu, null);
        }

        return new ParsedSections(null, menu);
    }

    private List<String> normalizeLines(String caption) {
        if (caption == null || caption.isBlank()) {
            return List.of();
        }

        String normalized = caption.replace("\r", "").trim();
        String[] rawLines = normalized.split("\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = normalizeLine(rawLine);
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private int findMealHeaderIndex(List<String> lines, MealType mealType) {
        for (int i = 0; i < lines.size(); i++) {
            if (isMealHeader(lines.get(i), mealType)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> trimLeadingTitleLines(List<String> lines) {
        int start = 0;
        while (start < lines.size()) {
            String compact = compactLine(lines.get(start));
            if (compact.isBlank() || DATE_LINE_PATTERN.matcher(compact).matches() || isGenericTitleLine(compact)) {
                start++;
                continue;
            }
            break;
        }
        return lines.subList(start, lines.size());
    }

    private String extractMenu(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }

        List<String> menuLines = new ArrayList<>();
        for (String rawLine : lines) {
            String compact = compactLine(rawLine);
            if (compact.isBlank() || DATE_LINE_PATTERN.matcher(compact).matches()) {
                continue;
            }

            if (isClosedLine(compact)) {
                return CLOSED_MENU;
            }

            if (isGenericTitleLine(compact) || isMealHeader(compact, MealType.LUNCH) || isMealHeader(compact, MealType.DINNER)) {
                if (!menuLines.isEmpty()) {
                    break;
                }
                continue;
            }

            if (isNonMenuLine(compact)) {
                if (!menuLines.isEmpty()) {
                    break;
                }
                continue;
            }

            menuLines.add(compact);
        }

        if (menuLines.isEmpty()) {
            return null;
        }
        return String.join(" ", menuLines);
    }

    private boolean isMealHeader(String line, MealType mealType) {
        String compact = compactLine(line);
        if (compact.isBlank()) {
            return false;
        }

        List<String> keywords = mealType == MealType.LUNCH ? LUNCH_KEYWORDS : DINNER_KEYWORDS;
        boolean hasKeyword = keywords.stream().anyMatch(compact::contains);
        if (!hasKeyword) {
            return false;
        }

        return compact.length() <= 16 || compact.startsWith("오늘") || compact.startsWith("금일");
    }

    private boolean isGenericTitleLine(String line) {
        String compact = compactLine(line);
        if (compact.isBlank()) {
            return false;
        }

        boolean containsMealKeyword = LUNCH_KEYWORDS.stream().anyMatch(compact::contains)
                || DINNER_KEYWORDS.stream().anyMatch(compact::contains);
        return compact.length() <= 16 && (containsMealKeyword || compact.contains("메뉴"));
    }

    private boolean isClosedLine(String line) {
        String compact = compactLine(line);
        return CLOSED_KEYWORDS.stream().anyMatch(compact::contains);
    }

    private boolean isNonMenuLine(String line) {
        String compact = compactLine(line);
        if (compact.isBlank()) {
            return true;
        }

        String lowerCase = compact.toLowerCase(Locale.ROOT);
        if (lowerCase.startsWith("#")) {
            return true;
        }

        return NON_MENU_PREFIXES.stream().anyMatch(compact::startsWith) || compact.contains("^^");
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }

        String normalized = line.replace('\u00A0', ' ').trim();
        normalized = normalized.replaceFirst("^[\\-·•◈]+\\s*", "");
        normalized = MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ");
        return normalized.trim();
    }

    private String compactLine(String line) {
        return normalizeLine(line);
    }

    private enum MealType {
        LUNCH,
        DINNER
    }

    private record ParsedSections(String lunchMenu, String dinnerMenu) {
        private static ParsedSections empty() {
            return new ParsedSections(null, null);
        }
    }
}
