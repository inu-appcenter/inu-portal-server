package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryCategoryCountResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import kr.inuappcenterportal.inuportal.domain.directory.repository.DirectoryEntryRepository;
import kr.inuappcenterportal.inuportal.domain.directory.repository.DirectorySourceRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectoryService {

    private static final String STAFF_SEARCH_URL = "https://inu.ac.kr/staffSearch/inu/srchView.do";
    private static final String UNKNOWN_LAYOUT = "unknown";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int REQUEST_TIMEOUT_MILLIS = 20000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final DirectoryEntryRepository directoryEntryRepository;
    private final DirectorySourceRepository directorySourceRepository;
    private final DirectorySourceService directorySourceService;
    private final DirectoryPersistenceService directoryPersistenceService;
    private final List<DirectorySourceEntryAdapter> sourceEntryAdapters;

    @Scheduled(cron = "0 0 4 * * SAT")
    public void scheduledSync() {
        try {
            DirectorySyncResponse result = syncAllCategories();
            log.info("[DirectoryService] 전화번호부 크롤링 성공. totalEntries={}", result.getTotalCount());
        } catch (Exception e) {
            log.error("[DirectoryService] 전화번호부 크롤링 실패..", e);
        }
    }

    public ListResponseDto<DirectoryEntryResponse> getEntries(DirectoryCategory category, String query, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, DEFAULT_PAGE_SIZE, getSort(category));
        String normalizedQuery = normalizeQuery(query);
        String normalizedPhoneQuery = normalizePhoneQuery(normalizedQuery);

        Page<DirectoryEntry> entries;
        if (normalizedQuery == null) {
            entries = category == null
                    ? directoryEntryRepository.findAll(pageable)
                    : directoryEntryRepository.findAllByCategory(category, pageable);
        } else {
            entries = category == null
                    ? directoryEntryRepository.searchAll(normalizedQuery, normalizedPhoneQuery, pageable)
                    : directoryEntryRepository.searchAllByCategory(category, normalizedQuery, normalizedPhoneQuery, pageable);
        }

        return ListResponseDto.of(
                entries.getTotalPages(),
                entries.getTotalElements(),
                entries.getContent().stream()
                        .map(DirectoryEntryResponse::of)
                        .collect(Collectors.toList())
        );
    }

    public DirectorySyncResponse syncAllCategories() throws IOException {
        LocalDateTime syncedAt = LocalDateTime.now();
        List<DirectoryCategoryCountResponse> categoryCounts = new ArrayList<>();

        for (DirectoryCategory category : DirectoryCategory.crawlableCategories()) {
            List<DirectoryEntry> entries = crawlCategory(category, syncedAt);
            directoryPersistenceService.replaceEntries(category, entries);
            categoryCounts.add(DirectoryCategoryCountResponse.of(category, entries.size()));
            log.info("Stored directory entries. category={}, count={}", category.name(), entries.size());
        }

        directorySourceService.refreshInventoryCategories(syncedAt);

        for (DirectoryCategory category : DirectoryCategory.inventoryCategories()) {
            List<DirectoryEntry> entries = crawlSourceCategory(category, syncedAt);
            directoryPersistenceService.replaceEntries(category, entries);
            categoryCounts.add(DirectoryCategoryCountResponse.of(category, entries.size()));
            log.info("Stored source-based directory entries. category={}, count={}", category.name(), entries.size());
        }

        long totalCount = categoryCounts.stream()
                .mapToLong(DirectoryCategoryCountResponse::getCount)
                .sum();

        return DirectorySyncResponse.of(syncedAt, totalCount, categoryCounts);
    }

    private List<DirectoryEntry> crawlCategory(DirectoryCategory category, LocalDateTime syncedAt) throws IOException {
        Document firstPage = fetchPage(category, 1);
        validateTablePage(category, firstPage);

        int totalPages = DirectoryParser.extractTotalPages(firstPage);
        List<DirectoryEntry> entries = new ArrayList<>(
                DirectoryParser.parseEntries(firstPage, category, 0, syncedAt)
        );

        for (int page = 2; page <= totalPages; page++) {
            Document pageDocument = fetchPage(category, page);
            entries.addAll(DirectoryParser.parseEntries(pageDocument, category, entries.size(), syncedAt));
        }

        return entries;
    }

    private List<DirectoryEntry> crawlSourceCategory(DirectoryCategory category, LocalDateTime syncedAt) {
        List<DirectorySource> sources = directorySourceRepository.findAllByCategoryOrderByDisplayOrderAscIdAsc(category);
        if (sources.isEmpty()) {
            log.warn("No directory sources found for category={}", category.name());
            return List.of();
        }

        List<DirectoryEntry> rawEntries = new ArrayList<>();
        for (DirectorySource source : sources) {
            DirectorySourceEntryAdapter adapter = findAdapter(source);
            if (adapter == null) {
                log.debug("Skipping unsupported directory source. category={}, sourceUrl={}, templateType={}",
                        category.name(), source.getSourceUrl(), source.getTemplateType());
                continue;
            }

            try {
                List<DirectoryEntry> entries = adapter.crawl(source, syncedAt);
                rawEntries.addAll(entries);
                log.info("Crawled directory source. category={}, sourceName={}, sourceUrl={}, count={}",
                        category.name(), source.getSourceName(), source.getSourceUrl(), entries.size());
            } catch (Exception e) {
                log.warn("Failed to crawl directory source. category={}, sourceName={}, sourceUrl={}",
                        category.name(), source.getSourceName(), source.getSourceUrl(), e);
            }
        }

        return assignDisplayOrders(deduplicateEntries(rawEntries));
    }

    private Document fetchPage(DirectoryCategory category, int page) throws IOException {
        return Jsoup.connect(STAFF_SEARCH_URL)
                .method(Connection.Method.POST)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .maxBodySize(0)
                .data("layout", UNKNOWN_LAYOUT)
                .data("page", String.valueOf(page))
                .data("srchDeptType", String.valueOf(category.getDeptType()))
                .data("srchDptCd", "")
                .data("levelCd", "")
                .post();
    }

    private void validateTablePage(DirectoryCategory category, Document document) {
        if (document.select(".func-table").isEmpty()) {
            throw new IllegalStateException("Directory table not found for category: " + category.name());
        }
    }

    private DirectorySourceEntryAdapter findAdapter(DirectorySource source) {
        return sourceEntryAdapters.stream()
                .filter(adapter -> adapter.supports(source))
                .findFirst()
                .orElse(null);
    }

    private List<DirectoryEntry> deduplicateEntries(List<DirectoryEntry> entries) {
        Map<String, DirectoryEntry> entriesByKey = new LinkedHashMap<>();
        for (DirectoryEntry entry : entries) {
            entriesByKey.putIfAbsent(buildDeduplicationKey(entry), entry);
        }
        return new ArrayList<>(entriesByKey.values());
    }

    private List<DirectoryEntry> assignDisplayOrders(List<DirectoryEntry> entries) {
        List<DirectoryEntry> reorderedEntries = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            reorderedEntries.add(entries.get(index).withDisplayOrder(index + 1));
        }
        return reorderedEntries;
    }

    private String buildDeduplicationKey(DirectoryEntry entry) {
        return String.join("|",
                entry.getCategory().name(),
                normalizeKeyPart(entry.getAffiliation()),
                normalizeKeyPart(entry.getDetailAffiliation()),
                normalizeKeyPart(entry.getName()),
                normalizeKeyPart(entry.getPosition()),
                normalizeKeyPart(entry.getPhoneNumberNormalized()),
                normalizeKeyPart(entry.getEmail()));
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }

        String trimmed = query.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizePhoneQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.replaceAll("\\D", "");
    }

    private Sort getSort(DirectoryCategory category) {
        if (category == null) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        return Sort.by(Sort.Direction.ASC, "displayOrder", "id");
    }
}
