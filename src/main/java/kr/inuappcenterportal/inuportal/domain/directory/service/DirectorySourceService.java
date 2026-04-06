package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryCategoryCountResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySourceResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySourceSyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectorySourceService {

    private static final String STAFF_SEARCH_URL = "https://inu.ac.kr/staffSearch/inu/srchView.do";
    private static final String UNKNOWN_LAYOUT = "unknown";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int REQUEST_TIMEOUT_MILLIS = 20000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final DirectorySourceRepository directorySourceRepository;
    private final DirectoryPersistenceService directoryPersistenceService;

    @Scheduled(cron = "0 15 4 * * SAT")
    public void scheduledSync() {
        try {
            DirectorySourceSyncResponse result = syncInventoryCategories();
            log.info("Directory source sync completed. totalSources={}", result.getTotalCount());
        } catch (Exception e) {
            log.error("Directory source sync failed.", e);
        }
    }

    public ListResponseDto<DirectorySourceResponse> getSources(DirectoryCategory category, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, DEFAULT_PAGE_SIZE, getSort(category));
        Page<DirectorySource> sources = category == null
                ? directorySourceRepository.findAll(pageable)
                : directorySourceRepository.findAllByCategory(category, pageable);

        return ListResponseDto.of(
                sources.getTotalPages(),
                sources.getTotalElements(),
                sources.getContent().stream()
                        .map(DirectorySourceResponse::of)
                        .toList()
        );
    }

    public DirectorySourceSyncResponse syncInventoryCategories() throws IOException {
        LocalDateTime syncedAt = LocalDateTime.now();
        List<DirectoryCategoryCountResponse> categoryCounts = refreshInventoryCategories(syncedAt);

        long totalCount = categoryCounts.stream()
                .mapToLong(DirectoryCategoryCountResponse::getCount)
                .sum();

        return DirectorySourceSyncResponse.of(syncedAt, totalCount, categoryCounts);
    }

    List<DirectoryCategoryCountResponse> refreshInventoryCategories(LocalDateTime syncedAt) throws IOException {
        List<DirectoryCategoryCountResponse> categoryCounts = new ArrayList<>();

        for (DirectoryCategory category : DirectoryCategory.inventoryCategories()) {
            List<DirectorySource> sources = crawlInventoryCategory(category, syncedAt);
            directoryPersistenceService.replaceSources(category, sources);
            categoryCounts.add(DirectoryCategoryCountResponse.of(category, sources.size()));
            log.info("Stored directory sources. category={}, count={}", category.name(), sources.size());
        }
        return categoryCounts;
    }

    private List<DirectorySource> crawlInventoryCategory(DirectoryCategory category, LocalDateTime syncedAt) throws IOException {
        Document document = fetchInventoryPage(category);
        validateInventoryPage(category, document);
        return DirectorySourceParser.parseSources(document, category, syncedAt);
    }

    private Document fetchInventoryPage(DirectoryCategory category) throws IOException {
        return Jsoup.connect(STAFF_SEARCH_URL)
                .method(Connection.Method.POST)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .maxBodySize(0)
                .data("layout", UNKNOWN_LAYOUT)
                .data("page", "1")
                .data("srchDeptType", String.valueOf(category.getDeptType()))
                .post();
    }

    private void validateInventoryPage(DirectoryCategory category, Document document) {
        boolean isValid = switch (category) {
            case UNIVERSITY -> !document.select(".func-list .univ-item").isEmpty();
            case GRADUATE_SCHOOL -> !document.select(".gradschool-list .gradschool-item").isEmpty();
            default -> false;
        };

        if (!isValid) {
            throw new IllegalStateException("Directory source inventory block not found for category: " + category.name());
        }
    }

    private Sort getSort(DirectoryCategory category) {
        if (category == null) {
            return Sort.by(Sort.Direction.ASC, "category", "displayOrder", "id");
        }
        return Sort.by(Sort.Direction.ASC, "displayOrder", "id");
    }
}
