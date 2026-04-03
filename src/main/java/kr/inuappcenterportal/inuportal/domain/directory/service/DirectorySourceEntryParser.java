package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class DirectorySourceEntryParser {

    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");
    private static final List<String> POSITION_LABELS = List.of("직책/직급", "직책", "직위", "직급", "구분");
    private static final List<String> PHONE_LABELS = List.of("전화번호", "전화", "연락처", "전화번호(연구실)", "tel", "phone");
    private static final List<String> EMAIL_LABELS = List.of("이메일", "e-mail", "email");
    private static final List<String> HOMEPAGE_LABELS = List.of("홈페이지", "homepage", "home");

    private DirectorySourceEntryParser() {
    }

    static List<DirectoryEntry> parseDocument(Document document, DirectorySource source,
                                              LocalDateTime syncedAt, String pageUrl) {
        List<DirectoryEntry> entries = parseK2WebProfileCards(document, source, syncedAt, pageUrl);
        if (!entries.isEmpty()) {
            return entries;
        }

        entries = parseBoardBoxCards(document, source, syncedAt, pageUrl);
        if (!entries.isEmpty()) {
            return entries;
        }

        entries = parseProfessorWrapCards(document, source, syncedAt, pageUrl);
        if (!entries.isEmpty()) {
            return entries;
        }

        return parseGenericContactTables(document, source, syncedAt, pageUrl);
    }

    private static List<DirectoryEntry> parseK2WebProfileCards(Document document, DirectorySource source,
                                                               LocalDateTime syncedAt, String pageUrl) {
        Elements profileItems = document.select("li._prFlLi");
        if (profileItems.isEmpty()) {
            return List.of();
        }

        List<DirectoryEntry> entries = new ArrayList<>();
        int displayOrder = 0;
        for (Element item : profileItems) {
            Map<String, String> fields = extractDlFields(item.select(".artclInfo dl"));
            String name = firstText(item, ".con-top strong", ".txtBox strong", ".con strong");
            String position = firstNonBlank(
                    getFieldValue(fields, POSITION_LABELS),
                    firstText(item, ".prof-rank p", ".prof-rank", ".title sub", ".title small")
            );
            String phoneNumber = getFieldValue(fields, PHONE_LABELS);
            String email = getFieldValue(fields, EMAIL_LABELS);
            String homepage = getFieldValue(fields, HOMEPAGE_LABELS);
            String duties = buildDuties(fields, homepage);
            String profileUrl = absoluteUrl(item, ".btn-detail a[href], a._fnctProfl[href]", pageUrl);

            DirectoryEntry entry = createEntry(
                    source,
                    name,
                    position,
                    duties,
                    phoneNumber,
                    email,
                    profileUrl,
                    null,
                    ++displayOrder,
                    syncedAt
            );
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private static List<DirectoryEntry> parseBoardBoxCards(Document document, DirectorySource source,
                                                           LocalDateTime syncedAt, String pageUrl) {
        Elements cardItems = document.select(".list-box-row-item");
        if (cardItems.isEmpty()) {
            return List.of();
        }

        List<DirectoryEntry> entries = new ArrayList<>();
        int displayOrder = 0;
        for (Element item : cardItems) {
            Map<String, String> fields = extractListFields(item.select(".content li"));
            String name = ownText(item.selectFirst(".title h4, h4"));
            String position = firstNonBlank(
                    ownText(item.selectFirst(".title sub, .title small")),
                    getFieldValue(fields, POSITION_LABELS)
            );
            String phoneNumber = getFieldValue(fields, PHONE_LABELS);
            String email = getFieldValue(fields, EMAIL_LABELS);
            String homepage = getFieldValue(fields, HOMEPAGE_LABELS);
            String duties = buildDuties(fields, homepage);
            String profileUrl = absoluteUrl(item, "a.box[href], a[href]", pageUrl);

            DirectoryEntry entry = createEntry(
                    source,
                    name,
                    position,
                    duties,
                    phoneNumber,
                    email,
                    profileUrl,
                    null,
                    ++displayOrder,
                    syncedAt
            );
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private static List<DirectoryEntry> parseProfessorWrapCards(Document document, DirectorySource source,
                                                                LocalDateTime syncedAt, String pageUrl) {
        Elements cardItems = document.select(".prof_wrap li");
        if (cardItems.isEmpty()) {
            return List.of();
        }

        List<DirectoryEntry> entries = new ArrayList<>();
        int displayOrder = 0;
        for (Element item : cardItems) {
            Map<String, String> fields = extractDlFields(item.select(".prof_txt dl"));
            String name = ownText(item.selectFirst(".prof_name h4, h4"));
            String position = firstNonBlank(
                    ownText(item.selectFirst(".prof_name h4 small, .prof_name small")),
                    getFieldValue(fields, POSITION_LABELS)
            );
            String phoneNumber = getFieldValue(fields, PHONE_LABELS);
            String email = getFieldValue(fields, EMAIL_LABELS);
            String homepage = getFieldValue(fields, HOMEPAGE_LABELS);
            String duties = buildDuties(fields, homepage);
            String profileUrl = absoluteUrl(item, ".prof_hp a[href], > a[href], a[href]", pageUrl);
            String detailAffiliation = firstText(item, ".prof_name p span", ".prof_name p");

            DirectoryEntry entry = createEntry(
                    source,
                    name,
                    position,
                    duties,
                    phoneNumber,
                    email,
                    profileUrl,
                    detailAffiliation,
                    ++displayOrder,
                    syncedAt
            );
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private static List<DirectoryEntry> parseGenericContactTables(Document document, DirectorySource source,
                                                                  LocalDateTime syncedAt, String pageUrl) {
        List<DirectoryEntry> entries = new ArrayList<>();
        int displayOrder = 0;

        for (Element table : document.select("table")) {
            TableMapping mapping = TableMapping.from(table);
            if (!mapping.isParsable()) {
                continue;
            }

            for (Element row : mapping.rows()) {
                List<String> values = row.select("th, td").stream()
                        .map(Element::text)
                        .map(DirectorySourceEntryParser::normalizeSingleLine)
                        .toList();

                if (values.isEmpty()) {
                    continue;
                }

                String name = mapping.value(values, TableColumn.NAME);
                String position = mapping.value(values, TableColumn.POSITION);
                String duties = mapping.value(values, TableColumn.DUTIES);
                String phoneNumber = mapping.value(values, TableColumn.PHONE);
                String email = mapping.value(values, TableColumn.EMAIL);
                String detailAffiliation = mapping.value(values, TableColumn.DEPARTMENT);

                DirectoryEntry entry = createEntry(
                        source,
                        name,
                        position,
                        duties,
                        phoneNumber,
                        email,
                        pageUrl,
                        detailAffiliation,
                        ++displayOrder,
                        syncedAt
                );
                if (entry != null) {
                    entries.add(entry);
                }
            }

            if (!entries.isEmpty()) {
                return entries;
            }
        }

        return entries;
    }

    private static DirectoryEntry createEntry(DirectorySource source, String name, String position, String duties,
                                              String phoneNumber, String email, String profileUrl,
                                              String detailAffiliationOverride, int displayOrder,
                                              LocalDateTime syncedAt) {
        String normalizedName = normalizeSingleLine(name);
        String normalizedPosition = normalizeSingleLine(position);
        String normalizedDuties = normalizeMultiline(duties);
        String normalizedPhone = normalizeSingleLine(phoneNumber);
        String normalizedEmail = normalizeSingleLine(email);
        String normalizedProfileUrl = normalizeUrl(profileUrl);

        if (!hasMeaningfulValue(normalizedName, normalizedPosition, normalizedDuties, normalizedPhone, normalizedEmail)) {
            return null;
        }

        return DirectoryEntry.create(
                source.getCategory(),
                resolveAffiliation(source),
                resolveDetailAffiliation(source, detailAffiliationOverride),
                normalizedName,
                normalizedPosition,
                normalizedDuties,
                normalizedPhone,
                DirectoryParser.normalizePhoneNumber(normalizedPhone),
                normalizedEmail,
                normalizedProfileUrl,
                displayOrder,
                syncedAt
        );
    }

    private static String resolveAffiliation(DirectorySource source) {
        return normalizeSingleLine(firstNonBlank(source.getParentName(), source.getSourceName()));
    }

    private static String resolveDetailAffiliation(DirectorySource source, String override) {
        if (StringUtils.hasText(override)) {
            return normalizeSingleLine(override);
        }
        if (StringUtils.hasText(source.getSectionName())) {
            return normalizeSingleLine(source.getSectionName() + " / " + source.getSourceName());
        }
        return normalizeSingleLine(source.getSourceName());
    }

    private static Map<String, String> extractDlFields(Elements dls) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Element dl : dls) {
            Element dt = dl.selectFirst("dt");
            Element dd = dl.selectFirst("dd");
            String label = normalizeLabel(dt == null ? null : dt.text());
            if (!StringUtils.hasText(label) && dt != null) {
                label = inferIconLabel(dt);
            }
            String value = normalizeSingleLine(dd == null ? null : dd.text());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            fields.put(label, value);
        }
        return fields;
    }

    private static Map<String, String> extractListFields(Elements items) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Element item : items) {
            String label = normalizeLabel(firstNonBlank(
                    item.select("b, strong, .label").stream().findFirst().map(Element::text).orElse(null),
                    ownText(item)
            ));
            String value = normalizeSingleLine(firstNonBlank(
                    item.select("p, dd, .value").stream().findFirst().map(Element::text).orElse(null),
                    item.text()
            ));

            if (!StringUtils.hasText(value)) {
                continue;
            }

            if (StringUtils.hasText(label) && value.startsWith(label)) {
                value = normalizeSingleLine(value.substring(label.length()));
            }
            fields.put(label, value);
        }
        return fields;
    }

    private static String inferIconLabel(Element dt) {
        String className = dt.className() + " " + dt.select("[class]").stream()
                .map(Element::className)
                .collect(Collectors.joining(" "));
        String lowerClassName = className.toLowerCase(Locale.ROOT);

        if (lowerClassName.contains("phone")) {
            return "전화번호";
        }
        if (lowerClassName.contains("envelope") || lowerClassName.contains("mail")) {
            return "이메일";
        }
        if (lowerClassName.contains("home")) {
            return "홈페이지";
        }
        if (lowerClassName.contains("chalkboard")) {
            return "담당과목";
        }
        if (lowerClassName.contains("pen-nib") || lowerClassName.contains("pencil")) {
            return "전공분야";
        }
        return "";
    }

    private static String buildDuties(Map<String, String> fields, String homepage) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String label = entry.getKey();
            String value = entry.getValue();

            if (!StringUtils.hasText(value)
                    || matchesAnyLabel(label, POSITION_LABELS)
                    || matchesAnyLabel(label, PHONE_LABELS)
                    || matchesAnyLabel(label, EMAIL_LABELS)
                    || matchesAnyLabel(label, HOMEPAGE_LABELS)) {
                continue;
            }

            if (StringUtils.hasText(label)) {
                lines.add(label + ": " + value);
            } else {
                lines.add(value);
            }
        }

        if (StringUtils.hasText(homepage) && !"-".equals(homepage.trim())) {
            lines.add("홈페이지: " + normalizeSingleLine(homepage));
        }

        return lines.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private static boolean matchesAnyLabel(String candidate, List<String> labels) {
        String normalized = normalizeLabel(candidate);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        return labels.stream().anyMatch(label -> normalizeLabel(label).equalsIgnoreCase(normalized));
    }

    private static String getFieldValue(Map<String, String> fields, List<String> labels) {
        return fields.entrySet().stream()
                .filter(entry -> matchesAnyLabel(entry.getKey(), labels))
                .map(Map.Entry::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private static String absoluteUrl(Element root, String selector, String fallbackUrl) {
        if (root == null) {
            return normalizeUrl(fallbackUrl);
        }
        Element anchor = root.selectFirst(selector);
        if (anchor == null) {
            return normalizeUrl(fallbackUrl);
        }
        String href = anchor.absUrl("href");
        if (!StringUtils.hasText(href)) {
            href = anchor.attr("href");
        }
        return normalizeUrl(StringUtils.hasText(href) ? href : fallbackUrl);
    }

    private static String firstText(Element root, String... selectors) {
        if (root == null) {
            return "";
        }
        return Arrays.stream(selectors)
                .map(root::selectFirst)
                .map(DirectorySourceEntryParser::ownText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private static String ownText(Element element) {
        if (element == null) {
            return "";
        }
        String ownText = normalizeSingleLine(element.ownText());
        if (StringUtils.hasText(ownText)) {
            return ownText;
        }
        return normalizeSingleLine(element.text());
    }

    private static boolean hasMeaningfulValue(String... values) {
        return Arrays.stream(values).anyMatch(StringUtils::hasText);
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private static String normalizeLabel(String value) {
        return normalizeSingleLine(value)
                .replace(":", "")
                .replace("：", "")
                .trim();
    }

    private static String normalizeUrl(String value) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        return normalized;
    }

    private static String normalizeSingleLine(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return MULTI_WHITESPACE.matcher(value.replace('\u00A0', ' ').trim()).replaceAll(" ");
    }

    private static String normalizeMultiline(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Arrays.stream(value.replace("\r", "").split("\n"))
                .map(DirectorySourceEntryParser::normalizeSingleLine)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private enum TableColumn {
        NAME,
        POSITION,
        DUTIES,
        PHONE,
        EMAIL,
        DEPARTMENT
    }

    private record TableMapping(Map<TableColumn, Integer> columnIndexes, List<Element> rows) {

        private boolean isParsable() {
            return !rows.isEmpty() && columnIndexes.keySet().stream()
                    .anyMatch(column -> column == TableColumn.NAME || column == TableColumn.PHONE
                            || column == TableColumn.EMAIL || column == TableColumn.POSITION || column == TableColumn.DUTIES);
        }

        private String value(List<String> values, TableColumn column) {
            Integer index = columnIndexes.get(column);
            if (index == null || index < 0 || index >= values.size()) {
                return "";
            }
            return values.get(index);
        }

        private static TableMapping from(Element table) {
            Elements headerCells = table.select("thead tr:first-child th, thead tr:first-child td");
            Elements rowElements;
            if (!headerCells.isEmpty()) {
                rowElements = table.select("tbody tr");
                if (rowElements.isEmpty()) {
                    rowElements = table.select("tr:gt(0)");
                }
            } else {
                Element headerRow = table.selectFirst("tr");
                if (headerRow == null) {
                    return new TableMapping(Map.of(), List.of());
                }
                headerCells = headerRow.select("th, td");
                rowElements = table.select("tr:gt(0)");
            }

            List<String> headers = headerCells.stream()
                    .map(Element::text)
                    .map(DirectorySourceEntryParser::normalizeLabel)
                    .toList();
            Map<TableColumn, Integer> columnIndexes = new LinkedHashMap<>();
            putIfFound(columnIndexes, TableColumn.NAME, headers, "이름", "성명", "교수명", "교원명");
            putIfFound(columnIndexes, TableColumn.POSITION, headers, "직위", "직책", "직급", "구분");
            putIfFound(columnIndexes, TableColumn.DUTIES, headers, "담당업무", "업무", "전공", "전공분야", "연구분야");
            putIfFound(columnIndexes, TableColumn.PHONE, headers, "전화번호", "연락처", "전화", "내선");
            putIfFound(columnIndexes, TableColumn.EMAIL, headers, "이메일", "email", "e-mail");
            putIfFound(columnIndexes, TableColumn.DEPARTMENT, headers, "소속", "학과", "부서", "전공");

            return new TableMapping(columnIndexes, rowElements.stream()
                    .filter(row -> !row.select("td, th").isEmpty())
                    .toList());
        }

        private static void putIfFound(Map<TableColumn, Integer> columnIndexes, TableColumn column,
                                       List<String> headers, String... candidates) {
            for (String candidate : candidates) {
                int index = headers.indexOf(normalizeLabel(candidate));
                if (index >= 0) {
                    columnIndexes.put(column, index);
                    return;
                }
            }
        }
    }
}
