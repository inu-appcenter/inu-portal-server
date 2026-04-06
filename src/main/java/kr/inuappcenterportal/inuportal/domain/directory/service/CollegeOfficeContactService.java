package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactSyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import kr.inuappcenterportal.inuportal.domain.directory.repository.CollegeOfficeContactRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CollegeOfficeContactService {

    private static final String COLLEGE_CONTACTS_URL = "https://www.inu.ac.kr/isc/6071/subview.do";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int REQUEST_TIMEOUT_MILLIS = 20000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final CollegeOfficeContactRepository collegeOfficeContactRepository;
    private final DirectoryPersistenceService directoryPersistenceService;

    @Scheduled(cron = "0 20 4 * * SAT")
    public void scheduledSync() {
        try {
            CollegeOfficeContactSyncResponse result = sync();
            log.info("College office contact sync completed. totalContacts={}", result.getTotalCount());
        } catch (Exception e) {
            log.error("College office contact sync failed.", e);
        }
    }

    public ListResponseDto<CollegeOfficeContactResponse> getContacts(String collegeName, String query, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "displayOrder", "id"));

        String normalizedCollegeName = normalizeQuery(collegeName);
        String normalizedQuery = normalizeQuery(query);
        String normalizedPhoneQuery = normalizePhoneQuery(normalizedQuery);

        Page<CollegeOfficeContact> contacts;
        if (normalizedCollegeName == null) {
            contacts = normalizedQuery == null
                    ? collegeOfficeContactRepository.findAll(pageable)
                    : collegeOfficeContactRepository.searchAll(normalizedQuery, normalizedPhoneQuery, pageable);
        } else {
            contacts = normalizedQuery == null
                    ? collegeOfficeContactRepository.findAllByCollegeName(normalizedCollegeName, pageable)
                    : collegeOfficeContactRepository.searchAllByCollegeName(
                            normalizedCollegeName,
                            normalizedQuery,
                            normalizedPhoneQuery,
                            pageable
                    );
        }

        return ListResponseDto.of(
                contacts.getTotalPages(),
                contacts.getTotalElements(),
                contacts.getContent().stream()
                        .map(CollegeOfficeContactResponse::of)
                        .toList()
        );
    }

    public CollegeOfficeContactSyncResponse sync() throws IOException {
        LocalDateTime syncedAt = LocalDateTime.now();
        Document document = fetchDocument();
        var contacts = CollegeOfficeContactParser.parse(document, COLLEGE_CONTACTS_URL, syncedAt);
        directoryPersistenceService.replaceCollegeOfficeContacts(contacts);
        log.info("Stored college office contacts. count={}", contacts.size());
        return CollegeOfficeContactSyncResponse.of(syncedAt, contacts.size());
    }

    private Document fetchDocument() throws IOException {
        return Jsoup.connect(COLLEGE_CONTACTS_URL)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .maxBodySize(0)
                .get();
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

}
