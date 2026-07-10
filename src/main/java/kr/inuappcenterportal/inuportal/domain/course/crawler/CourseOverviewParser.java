package kr.inuappcenterportal.inuportal.domain.course.crawler;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CourseOverviewParser {
    public List<CourseOverviewItemDto> parse(Document document) {
        List<CourseOverviewItemDto> result = new ArrayList<>();

        result.addAll(parseHBoxOverview(document));
        result.addAll(parseListOverview(document));
        result.addAll(parseCurTableOverview(document));

        return result;
    }

    /**
     * hBox6 파싱 로직
     * (대부분의 학과가 여기에 포함)
     */
    private List<CourseOverviewItemDto> parseHBoxOverview(Document document) {
        Elements items = document.select("ul.hBox6 > li");

        List<CourseOverviewItemDto> result = new ArrayList<>();

        for (Element item : items) {
            String title = item.select(".tit1").text();
            Element titleElement = item.selectFirst(".tit");

            if (title.isBlank() && titleElement != null) {
                title = titleElement.ownText();
            }

            if (title.isBlank() && titleElement != null) {
                title = titleElement.text();
            }

            String content = item.select(".cont").text();

            if (title.isBlank() || content.isBlank()) {
                continue;
            }

            result.add(new CourseOverviewItemDto(title, content));
        }

        return result;
    }

    /**
     * list 파싱 로직
     * (바이오-로봇 시스템공학과 이 로직에 해당)
     */
    private List<CourseOverviewItemDto> parseListOverview(Document document) {
        Elements items = document.select("ul.list_1 > li");

        List<CourseOverviewItemDto> result = new ArrayList<>();

        for (Element item : items) {
            Element titleElement = item.clone();
            titleElement.select("ul.list_3").remove();

            String title = clean(titleElement.text());
            String content = clean(item.select("ul.list_3 > li").text());

            if (title.isBlank() || content.isBlank()) {
                continue;
            }

            result.add(new CourseOverviewItemDto(title, content));
        }

        return result;
    }

    private List<CourseOverviewItemDto> parseCurTableOverview(Document document) {
        List<CourseOverviewItemDto> result = new ArrayList<>();

        for (Element table : document.select("table.curTable")) {
            int titleIndex = findTitleIndexFromHeader(table);

            if (titleIndex < 0) {
                continue;
            }

            Elements rows = table.select("tbody > tr");

            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);

                if (isDescriptionRow(row)) {
                    continue;
                }

                String title = getCellText(row, titleIndex);
                Element descriptionRow = findNextDescriptionRow(rows, i);
                String content = extractDescriptionContent(descriptionRow);

                if (title.isBlank() || content.isBlank()) {
                    continue;
                }

                result.add(new CourseOverviewItemDto(title, content));
            }
        }

        return result;
    }

    private int findTitleIndexFromHeader(Element table) {
        for (Element row : table.select("thead tr")) {
            List<String> headers = expandRowCells(row);

            for (int i = 0; i < headers.size(); i++) {
                String header = compact(headers.get(i));

                if (isTitleHeader(header)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean isTitleHeader(String header) {
        if (header.contains("코드")) {
            return false;
        }

        return header.contains("교과목명")
                || header.contains("과목명")
                || header.equals("교과목");
    }

    private List<String> expandRowCells(Element row) {
        List<String> values = new ArrayList<>();

        for (Element cell : directCells(row)) {
            String text = clean(cell.text());
            int colspan = parseSpan(cell.attr("colspan"));

            for (int i = 0; i < colspan; i++) {
                values.add(text);
            }
        }

        return values;
    }

    private String getCellText(Element row, int index) {
        Elements cells = directCells(row);

        if (index < 0 || index >= cells.size()) {
            return "";
        }

        return clean(cells.get(index).text());
    }

    private Element findNextDescriptionRow(Elements rows, int currentIndex) {
        if (currentIndex + 1 >= rows.size()) {
            return null;
        }

        Element nextRow = rows.get(currentIndex + 1);

        return isDescriptionRow(nextRow) ? nextRow : null;
    }

    private boolean isDescriptionRow(Element row) {
        return row != null
                && row.text().contains("강의설명")
                && row.selectFirst("p") != null;
    }

    private String extractDescriptionContent(Element row) {
        if (row == null) {
            return "";
        }

        String content = row.select("p").text();

        if (content.isBlank()) {
            content = row.text().replace("강의설명", "");
        }

        return clean(content);
    }

    private Elements directCells(Element row) {
        Elements cells = new Elements();

        for (Element child : row.children()) {
            if (child.normalName().equals("th") || child.normalName().equals("td")) {
                cells.add(child);
            }
        }

        return cells;
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

}
