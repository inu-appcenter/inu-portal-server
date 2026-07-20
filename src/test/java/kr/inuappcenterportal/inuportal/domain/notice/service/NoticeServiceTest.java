package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.notice.enums.NoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentCrawlerStateRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticeServiceTest {

    private NoticeService noticeService;
    private NoticeRepository noticeRepository;
    private DepartmentNoticeRepository departmentNoticeRepository;
    private DepartmentCrawlerStateRepository departmentCrawlerStateRepository;
    private CacheManager cacheManager;
    private CacheManager localCacheManager;
    private KeywordService keywordService;
    private ObjectMapper objectMapper;
    private ScheduleRepository scheduleRepository;
    private DepartmentNoticeScheduleExtractService scheduleExtractService;
    private NoticeCrawlHelper noticeCrawlHelper;

    @BeforeEach
    void setUp() {
        noticeRepository = mock(NoticeRepository.class);
        departmentNoticeRepository = mock(DepartmentNoticeRepository.class);
        departmentCrawlerStateRepository = mock(DepartmentCrawlerStateRepository.class);
        cacheManager = mock(CacheManager.class);
        localCacheManager = mock(CacheManager.class);
        keywordService = mock(KeywordService.class);
        objectMapper = new ObjectMapper();
        scheduleRepository = mock(ScheduleRepository.class);
        scheduleExtractService = mock(DepartmentNoticeScheduleExtractService.class);
        noticeCrawlHelper = mock(NoticeCrawlHelper.class);

        noticeService = new NoticeService(
                cacheManager,
                localCacheManager,
                noticeRepository,
                departmentNoticeRepository,
                departmentCrawlerStateRepository,
                keywordService,
                objectMapper,
                scheduleRepository,
                scheduleExtractService,
                noticeCrawlHelper
        );
    }

    @Test
    void testSyncNoticeContent_Success() {
        // Given
        String targetUrl = "https://www.inu.ac.kr/bbs/inu/246/426845/artclView";
        Notice notice = Notice.builder()
                .category("학사")
                .subCategory("일반")
                .title("외국인 유학생 학점인정을 위한 국어국문학과 한국어 교과목 강의계획서 및 교과목 설명서 관련 안내")
                .writer("국어국문학과")
                .createDate("2026.07.08")
                .url(targetUrl)
                .description("외국인 복수, 교환학생 학점인정...")
                .build();

        String mockHtml = "<!DOCTYPE html>"
                + "<html>"
                + "<body>"
                + "  <div class=\"view-con\">"
                + "    <p>안녕하세요. 국어국문학과입니다.</p>"
                + "  </div>"
                + "  <div class=\"view-file\">"
                + "    <ul>"
                + "      <li>"
                + "        <a href=\"/bbs/inu/246/388671/download.do\">국어국문학과 교과설명서.pdf</a>"
                + "      </li>"
                + "    </ul>"
                + "  </div>"
                + "</body>"
                + "</html>";

        Document mockDoc = Jsoup.parse(mockHtml, targetUrl);

        // When
        noticeService.parseAndSaveNoticeContent(notice, mockDoc);

        // Then
        assertNotNull(notice.getContentStatus());
        assertNotEquals(NoticeContentStatus.PENDING, notice.getContentStatus());
        assertNotEquals(NoticeContentStatus.FAILED, notice.getContentStatus());
        
        // Check if content HTML and text were extracted
        assertNotNull(notice.getContentHtml());
        assertTrue(notice.getContentHtml().contains("안녕하세요. 국어국문학과입니다."));
        
        assertNotNull(notice.getContentText());
        assertEquals("안녕하세요. 국어국문학과입니다.", notice.getContentText());

        // Check if attachments are detected and saved in JSON format
        assertNotNull(notice.getAttachmentMetaJson());
        assertTrue(notice.getAttachmentMetaJson().contains(".pdf"));
    }
}
