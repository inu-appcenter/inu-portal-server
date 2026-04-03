package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CollegeOfficeContactParser {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(.*?)(?:\\s*\\((.*?)\\))?$");

    private CollegeOfficeContactParser() {
    }

    static List<CollegeOfficeContact> parse(Document document, String sourceUrl, LocalDateTime syncedAt) {
        List<CollegeOfficeContact> contacts = new ArrayList<>();
        int displayOrder = 0;

        for (Element heading : document.select("div._objHeading h2.objHeading_h2")) {
            HeadingInfo headingInfo = parseHeadingInfo(heading.text());
            Element table = findNextTable(heading.parent());
            if (table == null) {
                continue;
            }

            Elements rows = table.select("tbody tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 4) {
                    continue;
                }

                String departmentName = normalizeText(cells.get(0).text());
                String officePhoneNumber = normalizeText(cells.get(1).text());
                String homepageUrl = extractHomepageUrl(cells.get(2));
                String officeLocation = normalizeText(cells.get(3).text());

                if (!StringUtils.hasText(departmentName)) {
                    continue;
                }

                contacts.add(CollegeOfficeContact.create(
                        headingInfo.collegeName(),
                        headingInfo.collegeLocationSummary(),
                        departmentName,
                        officePhoneNumber,
                        DirectoryParser.normalizePhoneNumber(officePhoneNumber),
                        homepageUrl,
                        officeLocation,
                        sourceUrl,
                        ++displayOrder,
                        syncedAt
                ));
            }
        }

        return contacts;
    }

    private static Element findNextTable(Element headingWrapper) {
        Element sibling = headingWrapper == null ? null : headingWrapper.nextElementSibling();
        while (sibling != null) {
            Element table = sibling.selectFirst("table");
            if (table != null) {
                return table;
            }
            if (sibling.hasClass("_objHeading")) {
                return null;
            }
            sibling = sibling.nextElementSibling();
        }
        return null;
    }

    private static HeadingInfo parseHeadingInfo(String headingText) {
        String normalized = normalizeText(headingText);
        Matcher matcher = HEADING_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return new HeadingInfo(normalized, "");
        }
        return new HeadingInfo(
                normalizeText(matcher.group(1)),
                normalizeText(matcher.group(2))
        );
    }

    private static String extractHomepageUrl(Element cell) {
        if (cell == null) {
            return "";
        }
        Element anchor = cell.selectFirst("a[href]");
        if (anchor == null) {
            return normalizeText(cell.text());
        }
        String href = anchor.absUrl("href");
        if (!StringUtils.hasText(href)) {
            href = anchor.attr("href");
        }
        return normalizeText(href);
    }

    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record HeadingInfo(String collegeName, String collegeLocationSummary) {
    }
}
