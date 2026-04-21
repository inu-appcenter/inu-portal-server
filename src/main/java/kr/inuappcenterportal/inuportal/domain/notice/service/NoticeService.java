package kr.inuappcenterportal.inuportal.domain.notice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeListResponse;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentCrawlerState;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentCrawlerStateRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.global.config.DepartmentCrawlConfig;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeService {

    private static final String DEPT_INDEX_KEY = "departmentIndex";
    private static final String DEPT_CONTENT_INDEX_KEY = "departmentContentIndex";
    private static final String DEPT_ENRICH_INDEX_KEY = "departmentEnrichIndex";
    private static final int DEPT_SIZE = 4;
    private static final int DEPT_CONTENT_SIZE = 4;
    private static final int DEPT_ENRICH_SIZE = 4;
    private static final int DEPT_NOTICE_LIMIT_PER_RUN = 4;
    private static final int DEPT_CONTENT_LIMIT_PER_DEPARTMENT = 4;
    private static final int DEPT_ENRICH_LIMIT_PER_DEPARTMENT = 4;
    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String CRAWLER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";
    private static final String ACCESS_DENIED_MESSAGE = "접근 권한이 없습니다.";
    private static final String GENERAL_NOTICE_LABEL = "일반공지";
    private static final String ALL_BOARD_NOTICE_LABEL = "전체게시판공지";
    private static final String ALL_POSTER_NOTICE_LABEL = "전체게시자공지";
    private static final String SCHOOL_NOTICE_ACADEMIC = "학사";
    private static final String SCHOOL_NOTICE_CREDIT_EXCHANGE = "학점교류";
    private static final String SCHOOL_NOTICE_GENERAL_EVENT_RECRUITING = "일반/행사/모집";
    private static final String SCHOOL_NOTICE_SCHOLARSHIP = "장학금";
    private static final String SCHOOL_NOTICE_TUITION = "등록금 납부";
    private static final String SCHOOL_NOTICE_EDUCATION_TEST = "교육시험";
    private static final String SCHOOL_NOTICE_VOLUNTEER = "봉사";
    private static final String NO_LABEL = "NO";
    private static final int ERROR_MESSAGE_LIMIT = 500;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<AttachmentMeta>> ATTACHMENT_META_LIST_TYPE = new TypeReference<>() {};

    private final NoticeRepository noticeRepository;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final DepartmentCrawlerStateRepository departmentCrawlerStateRepository;
    private final CacheManager cacheManager;
    private final KeywordService keywordService;
    private final ObjectMapper objectMapper;
    private final ScheduleRepository scheduleRepository;

    @Qualifier("localCacheManager")
    private final CacheManager localCacheManager;


    public NoticeService(
            @Qualifier("cacheManager") CacheManager cacheManager,
            @Qualifier("localCacheManager") CacheManager localCacheManager,
            NoticeRepository noticeRepository,
            DepartmentNoticeRepository departmentNoticeRepository,
            DepartmentCrawlerStateRepository departmentCrawlerStateRepository,
            KeywordService keywordService,
            ObjectMapper objectMapper,
            ScheduleRepository scheduleRepository
    ) {
        this.noticeRepository = noticeRepository;
        this.departmentNoticeRepository = departmentNoticeRepository;
        this.cacheManager = cacheManager;
        this.localCacheManager = localCacheManager;
        this.departmentCrawlerStateRepository = departmentCrawlerStateRepository;
        this.keywordService = keywordService;
        this.objectMapper = objectMapper;
        this.scheduleRepository = scheduleRepository;
    }

    @Scheduled(cron = "0 0/15 * * * *")
    @CacheEvict(value = "noticeCache", cacheManager = "cacheManager")
    @Transactional
    public void getNewNotice() {
        crawlingNotices();
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @CacheEvict(value = "noticeCache", cacheManager = "cacheManager")
    @Transactional
    public void getNewDepartmentNotice() {
        Department[] departments = Department.values();
        int start = getCrawlerIndex(DEPT_INDEX_KEY);
        int end = Math.min(start + DEPT_SIZE, departments.length);

        crawlingDepartmentNotices(departments, start, end);
        setCrawlerIndex(DEPT_INDEX_KEY, end >= departments.length ? 0 : end);

        log.info("학과 공지 크롤링을 완료했습니다. startIndex={}, endIndex={}", start, end);
    }

    @Scheduled(cron = "0 5/10 * * * *")
    @Transactional
    public void backfillDepartmentNoticeContents() {
        Department[] departments = Department.values();
        int start = getCrawlerIndex(DEPT_CONTENT_INDEX_KEY);
        int end = Math.min(start + DEPT_CONTENT_SIZE, departments.length);
        int count = backfillDepartmentNoticeContents(departments, start, end);

        setCrawlerIndex(DEPT_CONTENT_INDEX_KEY, end >= departments.length ? 0 : end);

        log.info("학과 공지 본문 백필을 완료했습니다. startIndex={}, endIndex={}, count={}", start, end, count);
    }

    @Scheduled(cron = "0 7/10 * * * *")
    @Transactional
    public void enrichDepartmentNoticeContents() {
        Department[] departments = Department.values();
        int start = getCrawlerIndex(DEPT_ENRICH_INDEX_KEY);
        int end = Math.min(start + DEPT_ENRICH_SIZE, departments.length);
        int count = enrichDepartmentNoticeContents(departments, start, end);

        setCrawlerIndex(DEPT_ENRICH_INDEX_KEY, end >= departments.length ? 0 : end);

        log.info("학과 공지 본문 보강을 완료했습니다. startIndex={}, endIndex={}, count={}", start, end, count);
    }

    @Transactional
    public void crawlingNotices() {
        syncNoticesByCategory(246, SCHOOL_NOTICE_ACADEMIC, 10, true);
        syncNoticesByCategory(247, SCHOOL_NOTICE_CREDIT_EXCHANGE, 10, true);
        syncNoticesByCategory(2611, SCHOOL_NOTICE_GENERAL_EVENT_RECRUITING, 10, true);
        syncNoticesByCategory(249, SCHOOL_NOTICE_SCHOLARSHIP, 10, true);
        syncNoticesByCategory(250, SCHOOL_NOTICE_TUITION, 10, true);
        syncNoticesByCategory(252, SCHOOL_NOTICE_EDUCATION_TEST, 10, true);
        syncNoticesByCategory(253, SCHOOL_NOTICE_VOLUNTEER, 10, true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        int[] categories = {246, 247, 2611, 249, 250, 252, 253};
        String[] categoryNames = {
                SCHOOL_NOTICE_ACADEMIC, SCHOOL_NOTICE_CREDIT_EXCHANGE, SCHOOL_NOTICE_GENERAL_EVENT_RECRUITING,
                SCHOOL_NOTICE_SCHOLARSHIP, SCHOOL_NOTICE_TUITION, SCHOOL_NOTICE_EDUCATION_TEST, SCHOOL_NOTICE_VOLUNTEER
        };

        for (int i = 0; i < categories.length; i++) {
            try {
                syncNoticesByCategory(categories[i], categoryNames[i], 100, false);
            } catch (Exception e) {
                log.error("초기 공지 동기화 중 오류 발생: category={}, reason={}", categoryNames[i], e.getMessage());
            }
        }
        log.info("학교 공지 초기 크롤링 프로세스를 완료했습니다.");
    }

    private void syncNoticesByCategory(int categoryId, String categoryName, int rowSize, boolean shouldNotify) {
        try {
            String rssUrl = "https://www.inu.ac.kr/bbs/inu/" + categoryId + "/rssList.do?row=" + rowSize;
            Document document = Jsoup.connect(rssUrl)
                    .userAgent(CRAWLER_USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .parser(org.jsoup.parser.Parser.xmlParser())
                    .get();

            Elements items = document.select("item");
            Set<String> activeUrls = new LinkedHashSet<>();
            String oldestDate = null;

            for (Element item : items) {
                String title = item.select("title").text();
                String link = item.select("link").text();
                if (!link.startsWith("http")) {
                    link = "https://www.inu.ac.kr" + link;
                }
                activeUrls.add(link);

                String pubDateStr = item.select("pubDate").text();
                String createDate = parseRssDate(pubDateStr);
                
                if (oldestDate == null || createDate.compareTo(oldestDate) < 0) {
                    oldestDate = createDate;
                }

                String writer = item.select("departmentName").text();
                String subCategory = item.select("category").text();
                String description = item.select("description").text();

                Optional<Notice> existingNotice = noticeRepository.findByUrl(link);
                if (existingNotice.isPresent()) {
                    Notice notice = existingNotice.get();
                    notice.update(subCategory, title, writer, description);
                } else {
                    Notice notice = noticeRepository.save(Notice.builder()
                            .category(categoryName)
                            .subCategory(subCategory)
                            .title(title)
                            .writer(writer)
                            .createDate(createDate)
                            .url(link)
                            .description(description)
                            .build());

                    if (shouldNotify) {
                        keywordService.noticeNotifyMatchedUsers(notice);
                    }
                }
            }

            if (oldestDate != null) {
                cleanupDeletedNotices(categoryName, oldestDate, activeUrls);
            }

        } catch (Exception e) {
            log.warn("학교 공지 RSS 크롤링에 실패했습니다. category={}, reason={}", categoryName, e.getMessage());
        }
    }

    private void cleanupDeletedNotices(String categoryName, String oldestDate, Set<String> activeUrls) {
        List<Notice> dbNotices = noticeRepository.findAllByCategoryAndCreateDateGreaterThanEqual(categoryName, oldestDate);
        List<Notice> toDelete = dbNotices.stream()
                .filter(notice -> !activeUrls.contains(notice.getUrl()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            noticeRepository.deleteAllInBatch(toDelete);
            log.info("학교 공지 삭제 처리 완료: category={}, count={}", categoryName, toDelete.size());
        }
    }

    private String parseRssDate(String pubDateStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(pubDateStr, inputFormatter);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } catch (Exception e) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto<NoticeListResponseDto> getNoticeList(String category, String sort, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 8, sort(sort));
        Page<Notice> notices;
        if (category == null) {
            notices = noticeRepository.findAllBy(pageable);
        } else {
            notices = noticeRepository.findAllByCategory(category, pageable);
        }
        return ListResponseDto.of(
                notices.getTotalPages(),
                notices.getTotalElements(),
                notices.getContent().stream().map(NoticeListResponseDto::of).collect(Collectors.toList())
        );
    }

    @Transactional(readOnly = true)
    public List<NoticeListResponseDto> getTop() {
        try {
            Cache cache = cacheManager.getCache("noticeCache");
            List<NoticeListResponseDto> list = cache.get("noticeTop", List.class);
            if (list != null) {
                return list;
            }
        } catch (Exception e) {
            log.warn("메인 캐시 매니저에서 공지 캐시를 읽지 못했습니다.");
        }

        try {
            Cache cache = localCacheManager.getCache("noticeCache");
            List<NoticeListResponseDto> list = cache.get("noticeTop", List.class);
            if (list != null) {
                return list;
            }
        } catch (Exception e) {
            log.warn("로컬 캐시 매니저에서 공지 캐시를 읽지 못했습니다.");
        }

        List<NoticeListResponseDto> notices = noticeRepository.findTop12().stream()
                .map(NoticeListResponseDto::of)
                .collect(Collectors.toList());

        try {
            cacheManager.getCache("noticeCache").put("noticeTop", notices);
            return notices;
        } catch (Exception e) {
            log.warn("메인 캐시 매니저에 공지 캐시를 저장하지 못했습니다.");
        }

        try {
            localCacheManager.getCache("noticeCache").put("noticeTop", notices);
        } catch (Exception e) {
            log.warn("로컬 캐시 매니저에 공지 캐시를 저장하지 못했습니다.");
        }
        return notices;
    }

    @Transactional(readOnly = true)
    public ListResponseDto<NoticeListResponseDto> searchNotice(String query, String category, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 8, Sort.by(Sort.Direction.DESC, "createDate", "id"));
        Page<Notice> notices = noticeRepository.searchNotices(query, category, pageable);

        return ListResponseDto.of(
                notices.getTotalPages(),
                notices.getTotalElements(),
                notices.getContent().stream().map(NoticeListResponseDto::of).collect(Collectors.toList())
        );
    }

    @Transactional
    public void crawlingDepartmentNotices(Department[] departments, int start, int end) {
        for (int i = start; i < end; i++) {
            Department department = departments[i];
            getNoticeByDepartment(department, getDepartmentCrawlConfig(department));
        }
    }

    private DepartmentCrawlConfig getDepartmentCrawlConfig(Department department) {
        if (department == Department.SPORTS_SCIENCE) {
            return new DepartmentCrawlConfig(
                    "td.td_subject a",
                    "td.td_datetime",
                    "td.td_subject a",
                    "td.td_num.td_hit2",
                    ele -> !ele.select("strong.notice_icon").isEmpty(),
                    false,
                    List.of("#bo_v_con", ".bo_v_con", ".view_content", ".board_view"),
                    List.of("#bo_v_file a[href]", ".view_file_download")
            );
        }

        return new DepartmentCrawlConfig(
                "td.td-subject a strong",
                "td.td-date",
                "td.td-subject a",
                "td.td-access",
                ele -> {
                    String num = ele.select("td.td-num").text();
                    return GENERAL_NOTICE_LABEL.equals(num)
                            || NO_LABEL.equals(num)
                            || ALL_BOARD_NOTICE_LABEL.equals(num)
                            || ALL_POSTER_NOTICE_LABEL.equals(num);
                },
                true,
                List.of(
                        ".view-con",
                        ".board-view .view-con",
                        ".board-view",
                        ".artclView .view_contents",
                        ".view_contents",
                        ".artclContents",
                        ".fr-view",
                        "#jwxe_main_content"
                ),
                List.of(
                        ".view-file a[href]",
                        ".view-file li a[href]",
                        ".artclFile a[href]",
                        ".file a[href]"
                )
        );
    }

    private int backfillDepartmentNoticeContents(Department[] departments, int start, int end) {
        int processedCount = 0;

        for (int i = start; i < end; i++) {
            Department department = departments[i];
            DepartmentCrawlConfig config = getDepartmentCrawlConfig(department);
            List<DepartmentNotice> notices = departmentNoticeRepository.findBackfillTargetsByDepartment(
                    department,
                    List.of(
                            DepartmentNoticeContentStatus.PENDING,
                            DepartmentNoticeContentStatus.FAILED,
                            DepartmentNoticeContentStatus.SUCCESS,
                            DepartmentNoticeContentStatus.ENRICH_PENDING,
                            DepartmentNoticeContentStatus.NO_TEXT_CONTENT,
                            DepartmentNoticeContentStatus.OCR_PENDING
                    ),
                    PageRequest.of(0, DEPT_CONTENT_LIMIT_PER_DEPARTMENT)
            );

            for (DepartmentNotice notice : notices) {
                syncDepartmentNoticeContent(notice, config);
                processedCount++;
            }
        }

        return processedCount;
    }

    private int enrichDepartmentNoticeContents(Department[] departments, int start, int end) {
        int processedCount = 0;

        for (int i = start; i < end; i++) {
            Department department = departments[i];
            List<DepartmentNotice> notices = departmentNoticeRepository.findByDepartmentAndContentStatusInOrderByIdDesc(
                    department,
                    List.of(DepartmentNoticeContentStatus.ENRICH_PENDING, DepartmentNoticeContentStatus.FAILED),
                    PageRequest.of(0, DEPT_ENRICH_LIMIT_PER_DEPARTMENT)
            );

            for (DepartmentNotice notice : notices) {
                enrichDepartmentNoticeContent(notice);
                processedCount++;
            }
        }

        return processedCount;
    }

    private void getNoticeByDepartment(Department department, DepartmentCrawlConfig config) {
        try {
            String url = department.getUrls();
            int index = 1;
            int count = 0;
            boolean outLoop = false;

            while (!outLoop) {
                String pageUrl = url + (url.contains("?") ? "&" : "?") + "page=" + index;
                Document document = connect(pageUrl).get();

                if (containsAccessDenied(document)) {
                    log.warn("접근 권한 제한으로 학과 공지 크롤링을 중단합니다. department={}", department.name());
                    break;
                }

                Elements notices = document.select("tbody tr");
                if (notices.isEmpty()) {
                    break;
                }

                for (Element ele : notices) {
                    if (config.getSkipCondition().test(ele)) {
                        continue;
                    }
                    if (!ele.select("td.no-data").isEmpty()) {
                        outLoop = true;
                        break;
                    }

                    String title = ele.select(config.getTitleSelector()).text();
                    String date = ele.select(config.getDateSelector()).text();
                    String href = resolveDepartmentNoticeUrl(ele.selectFirst(config.getLinkSelector()), url, config.isUseAbsoluteHref());
                    long views = parseLongValue(ele.select(config.getViewsSelector()).text());

                    Optional<DepartmentNotice> existingDepartmentNotice = departmentNoticeRepository.findFirstByDepartmentAndUrl(department, href);
                    if (existingDepartmentNotice.isEmpty()) {
                        existingDepartmentNotice = departmentNoticeRepository.findFirstByDepartmentAndTitleAndCreateDate(department, title, date);
                    }

                    DepartmentNotice departmentNotice;
                    boolean isNewNotice = false;

                    if (existingDepartmentNotice.isPresent()) {
                        departmentNotice = existingDepartmentNotice.get();
                        departmentNotice.updateListing(title, date, views, href);
                    } else {
                        departmentNotice = departmentNoticeRepository.save(
                                DepartmentNotice.create(department, title, date, views, href)
                        );
                        isNewNotice = true;
                    }

                    syncDepartmentNoticeContent(departmentNotice, config);

                    if (isNewNotice) {
                        keywordService.departmentNotifyMatchedUsers(departmentNotice, department);
                    }

                    count++;
                    if (count >= DEPT_NOTICE_LIMIT_PER_RUN) {
                        outLoop = true;
                        break;
                    }
                }
                index++;
            }

            log.info("학과 공지 크롤링을 완료했습니다. department={}", department.name());
        } catch (Exception e) {
            log.warn("학과 공지 크롤링에 실패했습니다. department={}, reason={}", department.name(), e.getMessage());
        }
    }

    private void syncDepartmentNoticeContent(DepartmentNotice departmentNotice, DepartmentCrawlConfig config) {
        if (departmentNotice.isContentCrawlBlocked() && departmentNotice.hasContentCrawlMetadata()) {
            return;
        }

        try {
            Document detailDocument = connect(departmentNotice.getUrl()).get();
            if (containsAccessDenied(detailDocument)) {
                departmentNotice.markContentAccessDenied();
                log.info("접근 권한 제한으로 학과 공지 본문 크롤링을 건너뜁니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }

            Element contentRoot = findDepartmentNoticeContentRoot(detailDocument, config);
            if (contentRoot == null && false) {
                departmentNotice.markContentFailed(limitMessage("학과 공지 본문 selector를 찾지 못했습니다."));
                log.warn("학과 공지 본문 selector를 찾지 못했습니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }

            Element sanitizedContent = contentRoot == null ? null : contentRoot.clone();
            if (sanitizedContent != null) {
                sanitizedContent.select("script, style, noscript, iframe").remove();
            }

            String contentHtml = sanitizedContent == null ? "" : sanitizedContent.html().trim();
            String contentText = sanitizedContent == null ? "" : sanitizedContent.text().trim();
            List<String> inlineImageUrls = collectInlineImageUrls(sanitizedContent, departmentNotice.getUrl());
            List<AttachmentMeta> attachmentMetas = collectAttachmentMetas(detailDocument, config, departmentNotice.getUrl());
            if (contentRoot == null && inlineImageUrls.isEmpty() && attachmentMetas.isEmpty()) {
                departmentNotice.markContentFailed(limitMessage("학과 공지 본문 selector를 찾지 못했습니다."));
                log.warn("학과 공지 본문 selector를 찾지 못했습니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }
            if (contentText.isBlank() && false) {
                departmentNotice.markContentFailed(limitMessage("학과 공지 본문 추출 결과가 비어 있습니다."));
                log.warn("학과 공지 본문 추출 결과가 비어 있습니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }

            departmentNotice.updateContent(
                    contentHtml,
                    contentText,
                    sha256(contentText),
                    LocalDateTime.now(),
                    writeJson(inlineImageUrls),
                    writeJson(attachmentMetas)
            );
            updateContentStatusAfterCrawl(departmentNotice, contentText, inlineImageUrls, attachmentMetas);
        } catch (Exception e) {
            departmentNotice.markContentFailed(limitMessage(e.getMessage()));
            log.warn("학과 공지 본문 크롤링에 실패했습니다. department={}, url={}, reason={}",
                    departmentNotice.getDepartment().name(), departmentNotice.getUrl(), e.getMessage());
        }
    }

    private void updateContentStatusAfterCrawl(
            DepartmentNotice departmentNotice,
            String contentText,
            List<String> inlineImageUrls,
            List<AttachmentMeta> attachmentMetas
    ) {
        String mergedText = mergeTexts(contentText, departmentNotice.getAttachmentText(), departmentNotice.getOcrText());
        departmentNotice.updateEnrichmentTexts(departmentNotice.getOcrText(), departmentNotice.getAttachmentText(), mergedText);

        boolean hasBaseText = !normalizeText(contentText).isBlank();
        boolean hasParsableAttachments = attachmentMetas.stream().anyMatch(this::isParsableAttachment);
        boolean hasImageAssets = !inlineImageUrls.isEmpty() || attachmentMetas.stream().anyMatch(this::isImageAttachment);

        if (hasParsableAttachments) {
            departmentNotice.markContentEnrichPending();
            return;
        }

        if (hasBaseText) {
            departmentNotice.markContentSuccess();
            return;
        }

        if (hasImageAssets) {
            departmentNotice.markContentOcrPending();
            return;
        }

        departmentNotice.markNoTextContent();
    }

    private void enrichDepartmentNoticeContent(DepartmentNotice departmentNotice) {
        try {
            if ((departmentNotice.getAttachmentMetaJson() == null || departmentNotice.getAttachmentMetaJson().isBlank())
                    && !departmentNotice.hasContent()) {
                return;
            }

            List<AttachmentMeta> attachmentMetas = readAttachmentMetas(departmentNotice.getAttachmentMetaJson());
            List<AttachmentMeta> parsableAttachments = attachmentMetas.stream()
                    .filter(this::isParsableAttachment)
                    .toList();

            boolean hasImageAssets = !readInlineImageUrls(departmentNotice.getInlineImageUrlsJson()).isEmpty()
                    || attachmentMetas.stream().anyMatch(this::isImageAttachment);

            if (parsableAttachments.isEmpty()) {
                finalizeNoticeWithoutAttachmentParse(departmentNotice, hasImageAssets);
                return;
            }

            List<String> extractedTexts = new ArrayList<>();
            List<String> failedAttachmentNames = new ArrayList<>();

            for (AttachmentMeta attachmentMeta : parsableAttachments) {
                try {
                    String extractedText = extractAttachmentText(attachmentMeta);
                    if (!normalizeText(extractedText).isBlank()) {
                        extractedTexts.add("[" + attachmentMeta.name() + "]\n" + extractedText.trim());
                    }
                } catch (Exception e) {
                    failedAttachmentNames.add(attachmentMeta.name());
                    log.warn("첨부파일 본문 추출에 실패했습니다. department={}, url={}, attachmentName={}, reason={}",
                            departmentNotice.getDepartment().name(), departmentNotice.getUrl(), attachmentMeta.name(), e.getMessage());
                }
            }

            if (!failedAttachmentNames.isEmpty()) {
                departmentNotice.markContentFailed(limitMessage("첨부파일 본문 추출 재시도 필요: " + String.join(", ", failedAttachmentNames)));
                return;
            }

            String attachmentText = mergeTexts(extractedTexts);
            String mergedText = mergeTexts(departmentNotice.getContentText(), attachmentText, departmentNotice.getOcrText());
            departmentNotice.updateEnrichmentTexts(departmentNotice.getOcrText(), attachmentText, mergedText);

            if (!normalizeText(mergedText).isBlank()) {
                departmentNotice.markContentSuccess();
                return;
            }

            if (hasImageAssets) {
                departmentNotice.markContentOcrPending();
                return;
            }

            departmentNotice.markNoTextContent();
        } catch (Exception e) {
            departmentNotice.markContentFailed(limitMessage(e.getMessage()));
            log.warn("학과 공지 본문 보강에 실패했습니다. department={}, url={}, reason={}",
                    departmentNotice.getDepartment().name(), departmentNotice.getUrl(), e.getMessage());
        }
    }

    private void finalizeNoticeWithoutAttachmentParse(DepartmentNotice departmentNotice, boolean hasImageAssets) {
        String mergedText = mergeTexts(departmentNotice.getContentText(), departmentNotice.getAttachmentText(), departmentNotice.getOcrText());
        departmentNotice.updateEnrichmentTexts(departmentNotice.getOcrText(), departmentNotice.getAttachmentText(), mergedText);

        if (!normalizeText(mergedText).isBlank()) {
            departmentNotice.markContentSuccess();
            return;
        }

        if (hasImageAssets) {
            departmentNotice.markContentOcrPending();
            return;
        }

        departmentNotice.markNoTextContent();
    }

    private Element findDepartmentNoticeContentRoot(Document document, DepartmentCrawlConfig config) {
        for (String selector : config.getContentSelectors()) {
            Element selected = document.selectFirst(selector);
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    private String resolveDepartmentNoticeUrl(Element linkElement, String listUrl, boolean useAbsoluteHref) {
        if (linkElement == null) {
            return listUrl;
        }

        if (useAbsoluteHref) {
            String absoluteHref = linkElement.attr("abs:href");
            if (!absoluteHref.isBlank()) {
                return absoluteHref;
            }
        }

        String rawHref = linkElement.attr("href");
        if (rawHref == null || rawHref.isBlank()) {
            return listUrl;
        }
        if (rawHref.startsWith("http://") || rawHref.startsWith("https://")) {
            return rawHref;
        }

        String absoluteHref = linkElement.attr("abs:href");
        if (!absoluteHref.isBlank()) {
            return absoluteHref;
        }

        return java.net.URI.create(listUrl).resolve(rawHref).toString();
    }

    private List<String> collectInlineImageUrls(Element contentRoot, String detailUrl) {
        if (contentRoot == null) {
            return List.of();
        }

        Set<String> imageUrls = new LinkedHashSet<>();
        for (Element image : contentRoot.select("img[src]")) {
            String resolvedUrl = resolveRelativeUrl(detailUrl, image.attr("abs:src"), image.attr("src"));
            if (!resolvedUrl.isBlank()) {
                imageUrls.add(resolvedUrl);
            }
        }
        return List.copyOf(imageUrls);
    }

    private List<AttachmentMeta> collectAttachmentMetas(Document document, DepartmentCrawlConfig config, String detailUrl) {
        Set<String> seenUrls = new LinkedHashSet<>();
        List<AttachmentMeta> attachmentMetas = new ArrayList<>();

        for (String selector : config.getAttachmentSelectors()) {
            for (Element link : document.select(selector)) {
                String resolvedUrl = resolveRelativeUrl(detailUrl, link.attr("abs:href"), link.attr("href"));
                if (resolvedUrl.isBlank() || !seenUrls.add(resolvedUrl)) {
                    continue;
                }

                String name = normalizeText(link.text());
                if (name.isBlank()) {
                    name = extractFileName(resolvedUrl);
                }

                attachmentMetas.add(new AttachmentMeta(
                        name,
                        resolvedUrl,
                        detectFileType(name, resolvedUrl)
                ));
            }
        }

        return attachmentMetas;
    }

    private String resolveRelativeUrl(String baseUrl, String absoluteCandidate, String rawCandidate) {
        if (absoluteCandidate != null && !absoluteCandidate.isBlank()) {
            return absoluteCandidate;
        }
        if (rawCandidate == null || rawCandidate.isBlank()) {
            return "";
        }
        if (rawCandidate.startsWith("http://") || rawCandidate.startsWith("https://")) {
            return rawCandidate;
        }
        return java.net.URI.create(baseUrl).resolve(rawCandidate).toString();
    }

    private boolean isParsableAttachment(AttachmentMeta attachmentMeta) {
        return switch (attachmentMeta.fileType()) {
            case "pdf", "txt", "csv", "md", "json", "xml", "html", "htm" -> true;
            default -> false;
        };
    }

    private boolean isImageAttachment(AttachmentMeta attachmentMeta) {
        return switch (attachmentMeta.fileType()) {
            case "png", "jpg", "jpeg", "gif", "bmp", "webp", "tif", "tiff" -> true;
            default -> false;
        };
    }

    private String detectFileType(String name, String url) {
        String candidate = name == null || name.isBlank() ? url : name;
        int dotIndex = candidate.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == candidate.length() - 1) {
            return "";
        }
        return candidate.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String extractAttachmentText(AttachmentMeta attachmentMeta) throws IOException {
        byte[] bytes = downloadBinary(attachmentMeta.url());

        return switch (attachmentMeta.fileType()) {
            case "pdf" -> extractPdfText(bytes);
            case "txt", "csv", "md", "json", "xml", "html", "htm" -> new String(bytes, StandardCharsets.UTF_8);
            default -> "";
        };
    }

    private byte[] downloadBinary(String url) throws IOException {
        Connection.Response response = connect(url)
                .ignoreContentType(true)
                .execute();
        return response.bodyAsBytes();
    }

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private List<AttachmentMeta> readAttachmentMetas(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ATTACHMENT_META_LIST_TYPE);
        } catch (Exception e) {
            log.warn("첨부 메타데이터를 읽지 못했습니다. reason={}", e.getMessage());
            return List.of();
        }
    }

    private List<String> readInlineImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("본문 이미지 메타데이터를 읽지 못했습니다. reason={}", e.getMessage());
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 직렬화에 실패했습니다.", e);
        }
    }

    private String extractFileName(String url) {
        String sanitizedUrl = url.replace("&amp;", "&");
        int queryIndex = sanitizedUrl.indexOf('?');
        String path = queryIndex >= 0 ? sanitizedUrl.substring(0, queryIndex) : sanitizedUrl;
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == path.length() - 1) {
            return "attachment";
        }
        return path.substring(slashIndex + 1);
    }

    private String mergeTexts(String... texts) {
        List<String> normalized = new ArrayList<>();
        for (String text : texts) {
            String value = normalizeText(text);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return String.join("\n\n", normalized);
    }

    private String mergeTexts(List<String> texts) {
        return mergeTexts(texts.toArray(String[]::new));
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u00A0", " ").trim();
    }

    @Transactional(readOnly = true)
    public ListResponseDto<DepartmentNoticeListResponse> getDepartmentNotices(Department department, String sort, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 8, sort(sort));
        Page<DepartmentNotice> departmentNotices = departmentNoticeRepository.findAllByDepartment(department, pageable);
        Map<Long, Boolean> hasSchedulesMap = loadHasSchedulesMap(departmentNotices.getContent());

        return ListResponseDto.of(
                departmentNotices.getTotalPages(),
                departmentNotices.getTotalElements(),
                departmentNotices.getContent().stream()
                        .map(notice -> DepartmentNoticeListResponse.of(
                                notice,
                                hasSchedulesMap.getOrDefault(notice.getId(), false)
                        ))
                        .collect(Collectors.toList())
        );
    }

    private Map<Long, Boolean> loadHasSchedulesMap(List<DepartmentNotice> notices) {
        List<Long> noticeIds = notices.stream()
                .map(DepartmentNotice::getId)
                .toList();

        if (noticeIds.isEmpty()) {
            return Map.of();
        }

        return scheduleRepository.findAllBySourceNoticeIdInAndAiGeneratedTrue(noticeIds).stream()
                .filter(schedule -> schedule.getSourceNoticeId() != null)
                .collect(Collectors.groupingBy(
                        Schedule::getSourceNoticeId,
                        Collectors.collectingAndThen(
                                Collectors.counting(),
                                count -> count > 0
                        )
                ));
    }

    private Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent(CRAWLER_USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MILLIS);
    }

    private boolean containsAccessDenied(Document document) {
        return containsAccessDenied(document.text());
    }

    private boolean containsAccessDenied(String value) {
        return value != null && value.contains(ACCESS_DENIED_MESSAGE);
    }

    private long parseLongValue(String value) {
        if (value == null) {
            return 0L;
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0L;
        }

        return Long.parseLong(digits);
    }


    private Sort sort(String sort) {
        if ("date".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "createDate", "id");
        } else {
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    private int getCrawlerIndex(String key) {
        return departmentCrawlerStateRepository.findByDeptKey(key)
                .map(DepartmentCrawlerState::getDeptIndex)
                .orElse(0);
    }

    private void setCrawlerIndex(String key, int deptIndex) {
        DepartmentCrawlerState state = departmentCrawlerStateRepository.findByDeptKey(key)
                .orElse(new DepartmentCrawlerState(key, 0));
        state.updateIndex(deptIndex);
        departmentCrawlerStateRepository.save(state);
    }

    private String limitMessage(String message) {
        String normalized = normalizeText(message);
        if (normalized.length() <= ERROR_MESSAGE_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, ERROR_MESSAGE_LIMIT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizeText(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate content hash", e);
        }
    }

    private record AttachmentMeta(String name, String url, String fileType) {
    }
}
