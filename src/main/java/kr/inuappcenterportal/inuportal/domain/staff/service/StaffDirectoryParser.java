package kr.inuappcenterportal.inuportal.domain.staff.service;

import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.staff.model.StaffDirectoryEntry;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class StaffDirectoryParser {

    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

    private StaffDirectoryParser() {
    }

    static int extractTotalPages(Document document) {
        Element totalPageElement = document.selectFirst("._paging ._totPage");
        if (totalPageElement != null && StringUtils.hasText(totalPageElement.text())) {
            return Integer.parseInt(totalPageElement.text().trim());
        }
        return document.select(".func-table tbody tr").isEmpty() ? 0 : 1;
    }

    static List<StaffDirectoryEntry> parseEntries(Document document, StaffDirectoryCategory category,
                                                  int startingDisplayOrder, LocalDateTime syncedAt) {
        Elements rows = document.select(".func-table tbody tr");
        List<StaffDirectoryEntry> entries = new ArrayList<>();
        int displayOrder = startingDisplayOrder;

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 5) {
                continue;
            }

            String affiliation = normalizeSingleLine(cells.get(0).text());
            String detailAffiliation = normalizeSingleLine(cells.get(1).text());
            String position = normalizeSingleLine(cells.get(2).text());
            String duties = extractDuties(cells.get(3));
            String phoneNumber = normalizeSingleLine(cells.get(4).text());

            if (!StringUtils.hasText(affiliation) && !StringUtils.hasText(detailAffiliation)
                    && !StringUtils.hasText(position) && !StringUtils.hasText(phoneNumber)) {
                continue;
            }

            entries.add(StaffDirectoryEntry.create(
                    category,
                    affiliation,
                    detailAffiliation,
                    position,
                    duties,
                    phoneNumber,
                    normalizePhoneNumber(phoneNumber),
                    ++displayOrder,
                    syncedAt
            ));
        }

        return entries;
    }

    static String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private static String extractDuties(Element dutiesCell) {
        Element pre = dutiesCell.selectFirst("pre");
        if (pre == null) {
            return normalizeSingleLine(dutiesCell.text());
        }

        String normalized = normalizeMultiline(pre.wholeText());
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return normalizeSingleLine(pre.text());
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
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }
}
