package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.notice.enums.NoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeContentRepository;
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
    private NoticeContentRepository noticeContentRepository;
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
        noticeContentRepository = mock(NoticeContentRepository.class);
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
                noticeContentRepository,
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

    @Test
    void testSyncNoticeContent_NoChange_TouchesFetchedAt() {
        // Given
        String targetUrl = "https://www.inu.ac.kr/bbs/inu/246/426845/artclView";
        Notice notice = Notice.builder()
                .category("학사")
                .subCategory("일반")
                .title("공지사항 제목")
                .writer("작성부서")
                .createDate("2026.07.08")
                .url(targetUrl)
                .description("요약...")
                .build();

        String mockHtml = "<!DOCTYPE html><html><body><div class=\"view-con\"><p>본문 내용입니다.</p></div></body></html>";
        Document mockDoc = Jsoup.parse(mockHtml, targetUrl);

        // First crawl
        noticeService.parseAndSaveNoticeContent(notice, mockDoc);
        java.time.LocalDateTime firstFetchedAt = notice.getContentFetchedAt();
        String originalHash = notice.getContentHash();
        assertNotNull(firstFetchedAt);

        // When (Crawl again with same content)
        noticeService.parseAndSaveNoticeContent(notice, mockDoc);

        // Then
        assertEquals(originalHash, notice.getContentHash());
        assertNotNull(notice.getContentFetchedAt());
    }

    @Test
    void testSyncNoticeContent_PendingWithExistingContent_NormalizesStatus() {
        // Given (Existing content + marked PENDING)
        String targetUrl = "https://www.inu.ac.kr/bbs/inu/246/426845/artclView";
        Notice notice = Notice.builder()
                .category("학사")
                .subCategory("일반")
                .title("공지사항 제목")
                .writer("작성부서")
                .createDate("2026.07.08")
                .url(targetUrl)
                .description("요약...")
                .build();

        String mockHtml = "<!DOCTYPE html><html><body><div class=\"view-con\"><p>본문 내용입니다.</p></div></body></html>";
        Document mockDoc = Jsoup.parse(mockHtml, targetUrl);

        noticeService.parseAndSaveNoticeContent(notice, mockDoc);
        assertEquals(NoticeContentStatus.SUCCESS, notice.getContentStatus());

        // Fast Track marks PENDING
        notice.markContentPending();
        assertEquals(NoticeContentStatus.PENDING, notice.getContentStatus());

        // When
        noticeService.parseAndSaveNoticeContent(notice, mockDoc);

        // Then
        assertEquals(NoticeContentStatus.SUCCESS, notice.getContentStatus());
    }

    @Test
    void testSyncNoticeContent_ContentChanged_UpdatesContent() {
        // Given
        String targetUrl = "https://www.inu.ac.kr/bbs/inu/246/426845/artclView";
        Notice notice = Notice.builder()
                .category("학사")
                .subCategory("일반")
                .title("공지사항 제목")
                .writer("작성부서")
                .createDate("2026.07.08")
                .url(targetUrl)
                .description("요약...")
                .build();

        String initialHtml = "<!DOCTYPE html><html><body><div class=\"view-con\"><p>초기 본문</p></div></body></html>";
        noticeService.parseAndSaveNoticeContent(notice, Jsoup.parse(initialHtml, targetUrl));
        String initialHash = notice.getContentHash();
        assertEquals("초기 본문", notice.getContentText());

        // When (Content changed on server)
        String updatedHtml = "<!DOCTYPE html><html><body><div class=\"view-con\"><p>수정된 본문 내용</p></div></body></html>";
        noticeService.parseAndSaveNoticeContent(notice, Jsoup.parse(updatedHtml, targetUrl));

        // Then
        assertNotEquals(initialHash, notice.getContentHash());
        assertEquals("수정된 본문 내용", notice.getContentText());
    }

    @Test
    void testGetDepartmentNoticeDetail_Success() {
        // Given
        Long noticeId = 100L;
        kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice departmentNotice =
                kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice.create(
                        kr.inuappcenterportal.inuportal.domain.notice.enums.Department.COMPUTER_ENGINEERING,
                        "컴퓨터공학부 공지사항",
                        java.time.LocalDate.of(2026, 8, 6),
                        150L,
                        "https://cse.inu.ac.kr/notice/100"
                );
        departmentNotice.updateContent(
                "<p>컴퓨터공학부 상세 본문입니다.</p>",
                "컴퓨터공학부 상세 본문입니다.",
                "hash123",
                java.time.LocalDateTime.now(),
                "[]",
                "[{\"name\":\"file1.pdf\",\"url\":\"https://cse.inu.ac.kr/download/1\",\"fileType\":\"pdf\"}]"
        );

        when(departmentNoticeRepository.findById(noticeId)).thenReturn(Optional.of(departmentNotice));
        when(scheduleRepository.existsBySourceNoticeIdAndAiGeneratedTrue(noticeId)).thenReturn(true);

        // When
        kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeDetailResponseDto response =
                noticeService.getDepartmentNoticeDetail(noticeId);

        // Then
        assertNotNull(response);
        assertEquals(departmentNotice.getTitle(), response.getTitle());
        assertEquals(kr.inuappcenterportal.inuportal.domain.notice.enums.Department.COMPUTER_ENGINEERING, response.getDepartment());
        assertEquals("2026.08.06", response.getCreateDate());
        assertEquals(150L, response.getView());
        assertEquals("<p>컴퓨터공학부 상세 본문입니다.</p>", response.getContentHtml());
        assertEquals("컴퓨터공학부 상세 본문입니다.", response.getContentText());
        assertTrue(response.isHasSchedules());
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());
        assertEquals("file1.pdf", response.getAttachments().get(0).name());
    }
}
