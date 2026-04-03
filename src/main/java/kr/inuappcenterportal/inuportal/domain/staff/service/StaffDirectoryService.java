package kr.inuappcenterportal.inuportal.domain.staff.service;

import kr.inuappcenterportal.inuportal.domain.staff.dto.StaffDirectoryCategoryCountResponse;
import kr.inuappcenterportal.inuportal.domain.staff.dto.StaffDirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.staff.dto.StaffDirectorySyncResponse;
import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.staff.model.StaffDirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.staff.repository.StaffDirectoryEntryRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffDirectoryService {

    private static final String STAFF_SEARCH_URL = "https://inu.ac.kr/staffSearch/inu/srchView.do";
    private static final String UNKNOWN_LAYOUT = "unknown";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int REQUEST_TIMEOUT_MILLIS = 20000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final StaffDirectoryEntryRepository staffDirectoryEntryRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledSync() {
        try {
            StaffDirectorySyncResponse result = syncCrawlableCategories();
            log.info("Staff directory sync completed. totalEntries={}", result.getTotalCount());
        } catch (Exception e) {
            log.error("Staff directory sync failed.", e);
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto<StaffDirectoryEntryResponse> getEntries(StaffDirectoryCategory category, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, DEFAULT_PAGE_SIZE, getSort(category));
        Page<StaffDirectoryEntry> entries = category == null
                ? staffDirectoryEntryRepository.findAll(pageable)
                : staffDirectoryEntryRepository.findAllByCategory(category, pageable);

        return ListResponseDto.of(
                entries.getTotalPages(),
                entries.getTotalElements(),
                entries.getContent().stream()
                        .map(StaffDirectoryEntryResponse::of)
                        .collect(Collectors.toList())
        );
    }

    public StaffDirectorySyncResponse syncCrawlableCategories() throws IOException {
        LocalDateTime syncedAt = LocalDateTime.now();
        List<StaffDirectoryCategoryCountResponse> categoryCounts = new ArrayList<>();

        for (StaffDirectoryCategory category : StaffDirectoryCategory.crawlableCategories()) {
            List<StaffDirectoryEntry> entries = crawlCategory(category, syncedAt);
            replaceEntries(category, entries);
            categoryCounts.add(StaffDirectoryCategoryCountResponse.of(category, entries.size()));
            log.info("Stored staff directory entries. category={}, count={}", category.name(), entries.size());
        }

        long totalCount = categoryCounts.stream()
                .mapToLong(StaffDirectoryCategoryCountResponse::getCount)
                .sum();

        return StaffDirectorySyncResponse.of(syncedAt, totalCount, categoryCounts);
    }

    private List<StaffDirectoryEntry> crawlCategory(StaffDirectoryCategory category, LocalDateTime syncedAt) throws IOException {
        Document firstPage = fetchPage(category, 1);
        validateTablePage(category, firstPage);

        int totalPages = StaffDirectoryParser.extractTotalPages(firstPage);
        List<StaffDirectoryEntry> entries = new ArrayList<>(
                StaffDirectoryParser.parseEntries(firstPage, category, 0, syncedAt)
        );

        for (int page = 2; page <= totalPages; page++) {
            Document pageDocument = fetchPage(category, page);
            entries.addAll(StaffDirectoryParser.parseEntries(pageDocument, category, entries.size(), syncedAt));
        }

        return entries;
    }

    private Document fetchPage(StaffDirectoryCategory category, int page) throws IOException {
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

    private void validateTablePage(StaffDirectoryCategory category, Document document) {
        if (document.select(".func-table").isEmpty()) {
            throw new IllegalStateException("Staff directory table not found for category: " + category.name());
        }
    }

    @Transactional
    protected void replaceEntries(StaffDirectoryCategory category, List<StaffDirectoryEntry> entries) {
        staffDirectoryEntryRepository.deleteByCategory(category);
        staffDirectoryEntryRepository.saveAll(entries);
    }

    private Sort getSort(StaffDirectoryCategory category) {
        if (category == null) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        return Sort.by(Sort.Direction.ASC, "displayOrder", "id");
    }
}
