package kr.inuappcenterportal.inuportal.domain.course.crawler.base;

import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CurriculumItemDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
/**
 *  각 학과의 홈페이지에 존재하는 교육과정 페이지 크롤링 및 파싱
 */
public class CurriculumParser {

    public List<CurriculumItemDto> parse(Document document) {
        List<CurriculumItemDto> result = new ArrayList<>();

        for (Element table : document.select("table")) {
            result.addAll(parseTable(table));
        }

        return result;
    }

    private List<CurriculumItemDto> parseTable(Element table) {
        List<List<String>> matrix = normalizeTable(table);

        if (matrix.isEmpty()) {
            return List.of();
        }

        ColumnIndexes indexes = resolveColumnIndexes(table, matrix);

        if (indexes.titleIndex() < 0) {
            return List.of();
        }

        String sectionGrade = findGradeFromSection(table);
        List<CurriculumItemDto> result = new ArrayList<>();

        for (int i = indexes.firstDataRowIndex(); i < matrix.size(); i++) {
            List<String> row = matrix.get(i);

            String targetGrade = normalizeGrade(get(row, indexes.gradeIndex()));
            String targetTerm = normalizeTerm(get(row, indexes.termIndex()));

            GradeTerm gradeTerm = parseGradeTerm(get(row, indexes.termIndex()));
            if (isBlank(targetGrade) && !isBlank(gradeTerm.grade())) {
                targetGrade = gradeTerm.grade();
            }
            if (!isBlank(gradeTerm.term())) {
                targetTerm = gradeTerm.term();
            }
            if (isBlank(targetGrade)) {
                targetGrade = sectionGrade;
            }

            String completionDivision = normalizeDivision(get(row, indexes.divisionIndex()));
            String title = normalizeTitle(get(row, indexes.titleIndex()));
            Integer credit = normalizeCredit(get(row, indexes.creditIndex()));

            if (isBlank(title) || isLikelyNonCourseRow(title)) {
                continue;
            }

            result.add(new CurriculumItemDto(
                    targetGrade,
                    targetTerm,
                    completionDivision,
                    title,
                    credit
            ));
        }

        return result;
    }

    private ColumnIndexes resolveColumnIndexes(Element table, List<List<String>> matrix) {
        int headerRowIndex = findHeaderRowIndex(matrix);

        if (headerRowIndex >= 0) {
            List<String> headers = matrix.get(headerRowIndex);

            return new ColumnIndexes(
                    findGradeHeaderIndex(headers),
                    findHeaderIndex(headers, "학기", "편성학기", "개설학기", "수강대상학기"),
                    findHeaderIndex(headers, "이수구분", "구분", "이수영역", "영역"),
                    findTitleHeaderIndex(headers),
                    findHeaderIndex(headers, "학점", "학점수"),
                    headerRowIndex + 1
            );
        }

        ColumnIndexes captionIndexes = resolveColumnIndexesFromCaption(table);
        if (captionIndexes.titleIndex() >= 0) {
            return captionIndexes;
        }

        return resolveFallbackColumnIndexes(table, matrix);
    }

    private ColumnIndexes resolveColumnIndexesFromCaption(Element table) {
        String caption = clean(table.selectFirst("caption") == null ? "" : table.selectFirst("caption").text());
        if (isBlank(caption)) {
            return new ColumnIndexes(-1, -1, -1, -1, -1, 0);
        }

        List<String> fields = new ArrayList<>();
        String normalizedCaption = caption
                .replace("-", ",")
                .replace("，", ",");

        for (String token : normalizedCaption.split(",")) {
            String field = clean(token);
            if (!field.isBlank()) {
                fields.add(field);
            }
        }

        if (fields.isEmpty()) {
            return new ColumnIndexes(-1, -1, -1, -1, -1, 0);
        }

        fields = dropCaptionPrefix(fields);

        int gradeIndex = findGradeHeaderIndex(fields);
        int termIndex = findHeaderIndex(fields, "학기", "편성학기", "개설학기", "수강대상학기");
        int divisionIndex = findHeaderIndex(fields, "이수구분", "구분", "이수영역", "영역");
        int titleIndex = findTitleHeaderIndex(fields);
        int creditIndex = findHeaderIndex(fields, "학점", "학점수");

        return new ColumnIndexes(gradeIndex, termIndex, divisionIndex, titleIndex, creditIndex, 0);
    }

    private List<String> dropCaptionPrefix(List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            String field = compact(fields.get(i));

            if (field.contains("학년")
                    || field.contains("학기")
                    || field.contains("교과목")
                    || field.contains("과목명")) {
                return fields.subList(i, fields.size());
            }
        }

        return fields;
    }

    private ColumnIndexes resolveFallbackColumnIndexes(Element table, List<List<String>> matrix) {
        String tableText = compact(table.text());

        if (!tableText.contains("교과목")
                && !tableText.contains("과목명")
                && !tableText.contains("이수구분")
                && !tableText.contains("학점")) {
            return new ColumnIndexes(-1, -1, -1, -1, -1, 0);
        }

        boolean hasFiveColumnRow = matrix.stream()
                .anyMatch(row -> row.size() >= 5);

        if (hasFiveColumnRow) {
            return new ColumnIndexes(0, 1, 2, 3, 4, 0);
        }

        return new ColumnIndexes(-1, -1, -1, -1, -1, 0);
    }

    private List<List<String>> normalizeTable(Element table) {
        Elements rows = table.select("tr");
        List<List<String>> matrix = new ArrayList<>();
        List<RowspanCell> activeRowspans = new ArrayList<>();

        for (Element row : rows) {
            List<String> normalizedRow = new ArrayList<>();
            List<RowspanCell> nextRowspans = new ArrayList<>();
            int columnIndex = 0;

            for (Element cell : row.select("th, td")) {
                columnIndex = fillActiveRowspans(normalizedRow, activeRowspans, nextRowspans, columnIndex);

                String text = clean(cell.text());
                int rowspan = parseSpan(cell.attr("rowspan"));
                int colspan = parseSpan(cell.attr("colspan"));

                for (int i = 0; i < colspan; i++) {
                    ensureSize(normalizedRow, columnIndex);
                    normalizedRow.set(columnIndex, text);

                    if (rowspan > 1) {
                        nextRowspans.add(new RowspanCell(columnIndex, rowspan - 1, text));
                    }

                    columnIndex++;
                }
            }

            fillRemainingRowspans(normalizedRow, activeRowspans, nextRowspans, columnIndex);
            matrix.add(normalizedRow);
            activeRowspans = nextRowspans;
        }

        return matrix;
    }

    private int fillActiveRowspans(
            List<String> row,
            List<RowspanCell> activeRowspans,
            List<RowspanCell> nextRowspans,
            int columnIndex
    ) {
        RowspanCell rowspanCell;

        while ((rowspanCell = findRowspanCell(activeRowspans, columnIndex)) != null) {
            ensureSize(row, columnIndex);
            row.set(columnIndex, rowspanCell.value());

            if (rowspanCell.remainingRows() > 1) {
                nextRowspans.add(new RowspanCell(
                        columnIndex,
                        rowspanCell.remainingRows() - 1,
                        rowspanCell.value()
                ));
            }

            columnIndex++;
        }

        return columnIndex;
    }

    private void fillRemainingRowspans(
            List<String> row,
            List<RowspanCell> activeRowspans,
            List<RowspanCell> nextRowspans,
            int columnIndex
    ) {
        int lastColumnIndex = activeRowspans.stream()
                .mapToInt(RowspanCell::columnIndex)
                .max()
                .orElse(columnIndex - 1);

        while (columnIndex <= lastColumnIndex) {
            columnIndex = fillActiveRowspans(row, activeRowspans, nextRowspans, columnIndex);
            columnIndex++;
        }
    }

    private RowspanCell findRowspanCell(List<RowspanCell> activeRowspans, int columnIndex) {
        for (RowspanCell rowspanCell : activeRowspans) {
            if (rowspanCell.columnIndex() == columnIndex) {
                return rowspanCell;
            }
        }

        return null;
    }

    private int findHeaderRowIndex(List<List<String>> matrix) {
        for (int i = 0; i < matrix.size(); i++) {
            List<String> row = matrix.get(i);

            boolean hasTitle = findTitleHeaderIndex(row) >= 0;
            boolean hasTerm = findHeaderIndex(row, "학기", "편성학기", "개설학기", "수강대상학기") >= 0;
            boolean hasCredit = findHeaderIndex(row, "학점", "학점수") >= 0;
            boolean hasDivision = findHeaderIndex(row, "이수구분", "구분", "이수영역", "영역") >= 0;

            if (hasTitle && (hasTerm || hasCredit || hasDivision)) {
                return i;
            }
        }

        return -1;
    }

    private int findHeaderIndex(List<String> headers, String... candidates) {
        for (int i = 0; i < headers.size(); i++) {
            String header = compact(headers.get(i));

            for (String candidate : candidates) {
                if (header.contains(compact(candidate))) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int findGradeHeaderIndex(List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String header = compact(headers.get(i));

            if (header.equals("학년")
                    || header.equals("이수학년")
                    || header.equals("수강대상학년")) {
                return i;
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            String header = compact(headers.get(i));

            if (header.contains("입학")
                    || header.contains("년도")
                    || header.contains("연도")) {
                continue;
            }

            if (header.contains("학년")) {
                return i;
            }
        }

        return -1;
    }

    private int findTitleHeaderIndex(List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String header = compact(headers.get(i));

            if (header.contains("코드")) {
                continue;
            }

            if (header.contains("교과목명")
                    || header.contains("과목명")
                    || header.equals("교과목")) {
                return i;
            }
        }

        return -1;
    }

    private String findGradeFromSection(Element table) {
        Element current = table;

        while (current != null) {
            String grade = findGradeFromElementId(current);
            if (!grade.isBlank()) {
                return grade;
            }

            grade = findGradeFromHeading(current);
            if (!grade.isBlank()) {
                return grade;
            }

            String id = current.id();

            if (!id.isBlank()) {
                grade = findGradeAnchorText(current.ownerDocument(), id);
                if (!grade.isBlank()) {
                    return grade;
                }
            }

            current = current.parent();
        }

        return "";
    }

    private String findGradeFromHeading(Element element) {
        Element heading = element.selectFirst("> h2");

        if (heading != null) {
            String grade = parseGradeHeadingText(heading.text());
            if (!grade.isBlank()) {
                return grade;
            }
        }

        Element sibling = element.previousElementSibling();
        while (sibling != null) {
            heading = sibling.selectFirst("h2");

            if (heading != null) {
                String grade = parseGradeHeadingText(heading.text());
                if (!grade.isBlank()) {
                    return grade;
                }
            }

            sibling = sibling.previousElementSibling();
        }

        return "";
    }

    private String parseGradeHeadingText(String value) {
        String grade = normalizeGrade(value);

        if (grade.matches("\\d학년") || grade.equals("공통")) {
            return grade;
        }

        return "";
    }

    private String findGradeFromElementId(Element element) {
        String id = element.id();

        if (id.matches("curr-0[1-4]")) {
            return id.substring(id.length() - 1) + "학년";
        }

        return "";
    }

    private String findGradeAnchorText(Document document, String id) {
        return document.select("a[href]").stream()
                .filter(anchor -> anchor.attr("href").equals("#" + id))
                .map(anchor -> normalizeGrade(anchor.text()))
                .filter(value -> value.matches("\\d학년") || value.equals("공통"))
                .findFirst()
                .orElse("");
    }

    private GradeTerm parseGradeTerm(String value) {
        value = compact(value);

        if (value.matches("\\d-\\d")) {
            String[] parts = value.split("-");
            return new GradeTerm(parts[0] + "학년", parts[1] + "학기");
        }

        if (value.matches("\\d학년-\\d학기")) {
            String[] parts = value.split("-");
            return new GradeTerm(parts[0], parts[1]);
        }

        return new GradeTerm("", "");
    }

    private String get(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }

        return row.get(index);
    }

    private String normalizeTitle(String value) {
        return clean(value);
    }

    private String normalizeGrade(String value) {
        value = clean(value);

        if (value.matches("\\d")) {
            return value + "학년";
        }

        return value;
    }

    private String normalizeTerm(String value) {
        value = clean(value);

        if (value.matches("\\d")) {
            return value + "학기";
        }

        GradeTerm gradeTerm = parseGradeTerm(value);
        if (!gradeTerm.term().isBlank()) {
            return gradeTerm.term();
        }

        return value;
    }

    private String normalizeDivision(String value) {
        value = clean(value);

        int parenthesisIndex = value.indexOf("(");
        if (parenthesisIndex > 0) {
            value = value.substring(0, parenthesisIndex).trim();
        }

        return value;
    }

    private Integer normalizeCredit(String value) {
        Matcher matcher = Pattern.compile("\\d+").matcher(clean(value));

        if (!matcher.find()) {
            return null;
        }

        return Integer.parseInt(matcher.group());
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\"", "")
                .replace("\u00a0", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String compact(String value) {
        return clean(value).replace(" ", "");
    }

    private int parseSpan(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void ensureSize(List<String> row, int index) {
        while (row.size() <= index) {
            row.add("");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isLikelyNonCourseRow(String title) {
        String compactTitle = compact(title);

        return compactTitle.equals("교과목명")
                || compactTitle.equals("과목명")
                || compactTitle.equals("교과목")
                || compactTitle.contains("강의설명")
                || compactTitle.contains("소계")
                || compactTitle.contains("합계");
    }

    private record ColumnIndexes(
            int gradeIndex,
            int termIndex,
            int divisionIndex,
            int titleIndex,
            int creditIndex,
            int firstDataRowIndex
    ) {
    }

    private record RowspanCell(
            int columnIndex,
            int remainingRows,
            String value
    ) {
    }

    private record GradeTerm(
            String grade,
            String term
    ) {
    }
}
