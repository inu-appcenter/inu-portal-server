package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
class K2WebDirectorySourceEntryAdapter extends AbstractDirectorySourceEntryAdapter {

    private static final int KEYWORD_CANDIDATE_LIMIT = 8;
    private static final int FALLBACK_CANDIDATE_LIMIT = 12;

    @Override
    public boolean supports(DirectorySource source) {
        return source.getTemplateType() == DirectorySourceTemplateType.SUBVIEW_DO
                || source.getTemplateType() == DirectorySourceTemplateType.INDEX_DO;
    }

    @Override
    public List<DirectoryEntry> crawl(DirectorySource source, LocalDateTime syncedAt) throws IOException {
        Document sourceDocument = fetchDocument(source.getSourceUrl());
        Map<String, DirectoryEntry> entriesByKey = new LinkedHashMap<>();

        collectEntries(entriesByKey, DirectorySourceEntryParser.parseDocument(
                sourceDocument,
                source,
                syncedAt,
                source.getSourceUrl()
        ));

        for (CandidatePage candidatePage : discoverCandidatePages(source, sourceDocument)) {
            try {
                Document candidateDocument = fetchDocument(candidatePage.url());
                collectEntries(entriesByKey, DirectorySourceEntryParser.parseDocument(
                        candidateDocument,
                        source,
                        syncedAt,
                        candidatePage.url()
                ));
            } catch (Exception e) {
                log.warn("Failed to crawl directory candidate page. sourceUrl={}, candidateUrl={}",
                        source.getSourceUrl(), candidatePage.url(), e);
            }
        }

        return new ArrayList<>(entriesByKey.values());
    }

    private List<CandidatePage> discoverCandidatePages(DirectorySource source, Document document) {
        Map<String, CandidatePage> keywordCandidates = new LinkedHashMap<>();
        Map<String, CandidatePage> fallbackCandidates = new LinkedHashMap<>();

        int order = 0;
        for (Element anchor : document.select("a[href]")) {
            String candidateUrl = normalizeUrl(anchor.absUrl("href"));
            if (!StringUtils.hasText(candidateUrl)) {
                candidateUrl = normalizeUrl(anchor.attr("href"));
            }

            if (!isCrawlableCandidate(source.getSourceUrl(), candidateUrl)) {
                continue;
            }

            int score = scoreCandidate(anchor, candidateUrl);
            CandidatePage candidatePage = new CandidatePage(candidateUrl, score, ++order);

            if (score > 0) {
                keywordCandidates.compute(candidateUrl, (key, existing) -> existing == null || existing.score() < score
                        ? candidatePage
                        : existing);
                continue;
            }

            if (source.getTemplateType() == DirectorySourceTemplateType.INDEX_DO && isFallbackCandidate(candidateUrl)) {
                fallbackCandidates.putIfAbsent(candidateUrl, candidatePage);
            }
        }

        if (!keywordCandidates.isEmpty()) {
            return keywordCandidates.values().stream()
                    .sorted((left, right) -> {
                        if (left.score() != right.score()) {
                            return Integer.compare(right.score(), left.score());
                        }
                        return Integer.compare(left.order(), right.order());
                    })
                    .limit(KEYWORD_CANDIDATE_LIMIT)
                    .toList();
        }

        return fallbackCandidates.values().stream()
                .limit(FALLBACK_CANDIDATE_LIMIT)
                .toList();
    }

    private boolean isCrawlableCandidate(String sourceUrl, String candidateUrl) {
        if (!StringUtils.hasText(candidateUrl) || normalizeUrl(sourceUrl).equals(candidateUrl)) {
            return false;
        }

        String lowerUrl = candidateUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.startsWith("javascript:")
                || lowerUrl.startsWith("mailto:")
                || lowerUrl.contains("/profl/")
                || lowerUrl.contains("empview.do")
                || lowerUrl.contains("wr_id=")) {
            return false;
        }

        return sameHost(sourceUrl, candidateUrl);
    }

    private boolean isFallbackCandidate(String candidateUrl) {
        String lowerUrl = candidateUrl.toLowerCase(Locale.ROOT);
        return lowerUrl.contains("subview.do")
                || lowerUrl.contains("board.php")
                || lowerUrl.contains("index.do");
    }

    private int scoreCandidate(Element anchor, String candidateUrl) {
        String target = (anchor.text() + " " + anchor.attr("title") + " " + anchor.attr("aria-label") + " " + candidateUrl)
                .toLowerCase(Locale.ROOT);

        int score = 0;
        score += keywordScore(target, "교수소개", 120);
        score += keywordScore(target, "전임교수", 115);
        score += keywordScore(target, "교수진", 110);
        score += keywordScore(target, "faculty", 100);
        score += keywordScore(target, "professor", 95);
        score += keywordScore(target, "교원", 90);
        score += keywordScore(target, "교수", 80);
        score += keywordScore(target, "교직원", 75);
        score += keywordScore(target, "행정실", 70);
        score += keywordScore(target, "직원", 65);
        score += keywordScore(target, "staff", 65);
        score += keywordScore(target, "구성원", 55);
        score += keywordScore(target, "member", 50);
        score += keywordScore(target, "people", 45);

        if (candidateUrl.contains("board.php")) {
            score += 20;
        }
        if (candidateUrl.contains("subview.do")) {
            score += 10;
        }
        if (candidateUrl.toLowerCase(Locale.ROOT).contains("faculty")
                || candidateUrl.toLowerCase(Locale.ROOT).contains("professor")
                || candidateUrl.toLowerCase(Locale.ROOT).contains("staff")) {
            score += 30;
        }

        return score;
    }

    private int keywordScore(String target, String keyword, int weight) {
        return target.contains(keyword.toLowerCase(Locale.ROOT)) ? weight : 0;
    }

    private boolean sameHost(String sourceUrl, String candidateUrl) {
        try {
            URI sourceUri = URI.create(sourceUrl);
            URI candidateUri = URI.create(candidateUrl);
            return StringUtils.hasText(sourceUri.getHost())
                    && sourceUri.getHost().equalsIgnoreCase(candidateUri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        return normalized;
    }

    private void collectEntries(Map<String, DirectoryEntry> entriesByKey, List<DirectoryEntry> entries) {
        for (DirectoryEntry entry : entries) {
            entriesByKey.putIfAbsent(buildKey(entry), entry);
        }
    }

    private String buildKey(DirectoryEntry entry) {
        return String.join("|",
                entry.getCategory().name(),
                normalizeText(entry.getAffiliation()),
                normalizeText(entry.getDetailAffiliation()),
                normalizeText(entry.getName()),
                normalizeText(entry.getPosition()),
                normalizeText(entry.getPhoneNumberNormalized()),
                normalizeText(entry.getEmail()));
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private record CandidatePage(String url, int score, int order) {
    }
}
