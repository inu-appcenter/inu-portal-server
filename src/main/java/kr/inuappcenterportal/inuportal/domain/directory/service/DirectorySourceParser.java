package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DirectorySourceParser {

    private DirectorySourceParser() {
    }

    public static List<DirectorySource> parseSources(Document document, DirectoryCategory category,
                                                     LocalDateTime syncedAt) {
        return switch (category) {
            case UNIVERSITY -> parseUniversitySources(document, syncedAt);
            case GRADUATE_SCHOOL -> parseGraduateSchoolSources(document, syncedAt);
            default -> throw new IllegalArgumentException("Source inventory is not supported for category: " + category.name());
        };
    }

    private static List<DirectorySource> parseUniversitySources(Document document, LocalDateTime syncedAt) {
        List<DirectorySource> sources = new ArrayList<>();
        int displayOrder = 1;

        for (Element universityItem : document.select(".func-list .univ-item")) {
            Element universityNameElement = universityItem.selectFirst(".univ-name");
            String parentName = normalizeText(universityNameElement == null ? null : universityNameElement.text());
            String parentUrl = extractAbsoluteUrl(universityNameElement == null ? null : universityNameElement.selectFirst("a[href]"));

            Elements departmentAnchors = universityItem.select(".dept-list a[href]");
            for (Element departmentAnchor : departmentAnchors) {
                String sourceName = normalizeText(departmentAnchor.text());
                String sourceUrl = extractAbsoluteUrl(departmentAnchor);
                if (parentName.isBlank() || sourceName.isBlank() || sourceUrl == null) {
                    continue;
                }

                sources.add(DirectorySource.create(
                        DirectoryCategory.UNIVERSITY,
                        parentName,
                        null,
                        sourceName,
                        parentUrl,
                        sourceUrl,
                        DirectorySourceTemplateType.fromUrl(sourceUrl),
                        displayOrder++,
                        syncedAt
                ));
            }
        }

        return sources;
    }

    private static List<DirectorySource> parseGraduateSchoolSources(Document document, LocalDateTime syncedAt) {
        List<DirectorySource> sources = new ArrayList<>();
        int displayOrder = 1;

        for (Element graduateSchoolItem : document.select(".gradschool-list .gradschool-item")) {
            Element graduateSchoolNameElement = graduateSchoolItem.selectFirst(".gradschool-name");
            String parentName = normalizeText(graduateSchoolNameElement == null ? null : graduateSchoolNameElement.text());
            String parentUrl = extractAbsoluteUrl(graduateSchoolNameElement == null ? null : graduateSchoolNameElement.selectFirst("a[href]"));

            Elements trackListElements = graduateSchoolItem.select(".track-list");
            if (trackListElements.isEmpty()) {
                displayOrder = appendGraduateSources(
                        sources,
                        graduateSchoolItem.select(".gradschool-content a[href]"),
                        parentName,
                        null,
                        parentUrl,
                        displayOrder,
                        syncedAt
                );
                continue;
            }

            for (Element trackListElement : trackListElements) {
                String sectionName = normalizeText(
                        trackListElement.selectFirst(".track-name") == null
                                ? null
                                : trackListElement.selectFirst(".track-name").text()
                );
                displayOrder = appendGraduateSources(
                        sources,
                        trackListElement.select(".major-list a[href]"),
                        parentName,
                        sectionName,
                        parentUrl,
                        displayOrder,
                        syncedAt
                );
            }
        }

        return sources;
    }

    private static int appendGraduateSources(List<DirectorySource> sources, Elements anchors, String parentName,
                                             String sectionName, String parentUrl, int displayOrder,
                                             LocalDateTime syncedAt) {
        for (Element anchor : anchors) {
            String sourceName = normalizeText(anchor.text());
            String sourceUrl = extractAbsoluteUrl(anchor);
            if (parentName.isBlank() || sourceName.isBlank() || sourceUrl == null) {
                continue;
            }

            sources.add(DirectorySource.create(
                    DirectoryCategory.GRADUATE_SCHOOL,
                    parentName,
                    sectionName == null || sectionName.isBlank() ? null : sectionName,
                    sourceName,
                    parentUrl,
                    sourceUrl,
                    DirectorySourceTemplateType.fromUrl(sourceUrl),
                    displayOrder++,
                    syncedAt
            ));
        }

        return displayOrder;
    }

    private static String extractAbsoluteUrl(Element anchor) {
        if (anchor == null) {
            return null;
        }

        String href = anchor.absUrl("href");
        if (href == null || href.isBlank()) {
            href = anchor.attr("href");
        }

        if (href == null || href.isBlank()) {
            return null;
        }

        String normalizedUrl = href.trim();
        int fragmentIndex = normalizedUrl.indexOf('#');
        if (fragmentIndex >= 0) {
            normalizedUrl = normalizedUrl.substring(0, fragmentIndex);
        }
        return normalizedUrl.isBlank() ? null : normalizedUrl;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
