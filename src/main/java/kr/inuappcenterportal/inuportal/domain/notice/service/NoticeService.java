package kr.inuappcenterportal.inuportal.domain.notice.service;

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
import kr.inuappcenterportal.inuportal.global.config.DepartmentCrawlConfig;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeService {

    private static final String DEPT_INDEX_KEY = "departmentIndex";
    private static final String DEPT_CONTENT_INDEX_KEY = "departmentContentIndex";
    private static final int DEPT_SIZE = 4;
    private static final int DEPT_CONTENT_SIZE = 4;
    private static final int DEPT_NOTICE_LIMIT_PER_RUN = 4;
    private static final int DEPT_CONTENT_LIMIT_PER_DEPARTMENT = 4;
    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String CRAWLER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";
    private static final String ACCESS_DENIED_MESSAGE = "\uC811\uADFC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String GENERAL_NOTICE_LABEL = "\uC77C\uBC18\uACF5\uC9C0";
    private static final String ALL_BOARD_NOTICE_LABEL = "\uC804\uCCB4\uAC8C\uC2DC\uD310\uACF5\uC9C0";
    private static final String ALL_POSTER_NOTICE_LABEL = "\uC804\uCCB4\uAC8C\uC2DC\uC790\uACF5\uC9C0";
    private static final String SCHOOL_NOTICE_ACADEMIC = "\uD559\uC0AC";
    private static final String SCHOOL_NOTICE_CREDIT_EXCHANGE = "\uD559\uC810\uAD50\uB958";
    private static final String SCHOOL_NOTICE_GENERAL_EVENT_RECRUITING = "\uC77C\uBC18/\uD589\uC0AC/\uBAA8\uC9D1";
    private static final String SCHOOL_NOTICE_SCHOLARSHIP = "\uC7A5\uD559\uAE08";
    private static final String SCHOOL_NOTICE_TUITION = "\uB4F1\uB85D\uAE08\uB0A9\uBD80";
    private static final String SCHOOL_NOTICE_EDUCATION_TEST = "\uAD50\uC721\uC2DC\uD5D8";
    private static final String NO_LABEL = "NO";

    private final NoticeRepository noticeRepository;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final DepartmentCrawlerStateRepository departmentCrawlerStateRepository;
    private final CacheManager cacheManager;
    private final KeywordService keywordService;

    @Qualifier("localCacheManager")
    private final CacheManager localCacheManager;

    private static long id = 0;

    public NoticeService(
            @Qualifier("cacheManager") CacheManager cacheManager,
            @Qualifier("localCacheManager") CacheManager localCacheManager,
            NoticeRepository noticeRepository,
            DepartmentNoticeRepository departmentNoticeRepository,
            DepartmentCrawlerStateRepository departmentCrawlerStateRepository,
            KeywordService keywordService
    ) {
        this.noticeRepository = noticeRepository;
        this.departmentNoticeRepository = departmentNoticeRepository;
        this.cacheManager = cacheManager;
        this.localCacheManager = localCacheManager;
        this.departmentCrawlerStateRepository = departmentCrawlerStateRepository;
        this.keywordService = keywordService;
    }

    @Scheduled(cron = "0 0/30 * * * *")
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

    @Transactional
    public void crawlingNotices() {
        id = 0;
        noticeRepository.deleteAllInBatch();

        getNoticeByCategory(1516, 46, SCHOOL_NOTICE_ACADEMIC);
        getNoticeByCategory(1517, 47, SCHOOL_NOTICE_CREDIT_EXCHANGE);
        getNoticeByCategory(1518, 611, SCHOOL_NOTICE_GENERAL_EVENT_RECRUITING);
        getNoticeByCategory(1519, 49, SCHOOL_NOTICE_SCHOLARSHIP);
        getNoticeByCategory(1520, 50, SCHOOL_NOTICE_TUITION);
        getNoticeByCategory(1530, 52, SCHOOL_NOTICE_EDUCATION_TEST);
    }

    private void getNoticeByCategory(int category, int categoryNum, String categoryName) {
        try {
            String url = "https://www.inu.ac.kr/inu/" + category + "/subview.do?enc=";
            int index = 1;
            boolean outLoop = false;

            while (!outLoop) {
                String postUrl = "fnct1|@@|%2Fbbs%2Finu%2F2" + categoryNum + "%2FartclList.do%3Fpage%3D" + index
                        + "%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%267";
                String encodedUrl = url + encoding(postUrl);
                Document document = connect(encodedUrl).get();
                Elements notice = document.select("tr");

                for (Element ele : notice) {
                    if (NO_LABEL.equals(ele.select("th.th-num").text())) {
                        continue;
                    }
                    if (isAMonthAgo(ele.select("td.td-date").text())) {
                        outLoop = true;
                        break;
                    }

                    String href = "www.inu.ac.kr" + ele.select("td.td-subject").select("a").attr("href");
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(href);
                    matcher.find();
                    matcher.find();
                    String number = matcher.group();
                    String baseUrl = "fnct1|@@|%2Fbbs%2Finu%2F2006%2F" + number
                            + "%2FartclView.do%3Fpage%3D3%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%26password%3D%267";

                    noticeRepository.save(Notice.builder()
                            .category(categoryName)
                            .title(Objects.requireNonNull(Objects.requireNonNull(ele.select("td.td-subject").first()).selectFirst("strong").text()))
                            .url("www.inu.ac.kr/inu/" + category + "/subview.do?enc=" + encoding(baseUrl))
                            .writer(ele.select("td.td-write").text())
                            .createDate(ele.select("td.td-date").text())
                            .view(Long.parseLong(ele.select("td.td-access").text()))
                            .id(++id)
                            .build());
                }
                index++;
            }
        } catch (Exception e) {
            log.warn("학교 공지 크롤링에 실패했습니다. category={}, reason={}", categoryName, e.getMessage());
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
                    List.of("#bo_v_con", ".bo_v_con", ".view_content", ".board_view")
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
                    List.of(DepartmentNoticeContentStatus.PENDING, DepartmentNoticeContentStatus.FAILED),
                    PageRequest.of(0, DEPT_CONTENT_LIMIT_PER_DEPARTMENT)
            );

            for (DepartmentNotice notice : notices) {
                syncDepartmentNoticeContent(notice, config);
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
                        keywordService.departmentNotifyMatchedUsersAndKeyword(departmentNotice, department);
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
        if (departmentNotice.hasContent() || departmentNotice.isContentCrawlBlocked()) {
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
            if (contentRoot == null) {
                departmentNotice.markContentFailed();
                log.warn("학과 공지 본문 selector를 찾지 못했습니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }

            Element sanitizedContent = contentRoot.clone();
            sanitizedContent.select("script, style, noscript, iframe").remove();

            String contentHtml = sanitizedContent.html().trim();
            String contentText = sanitizedContent.text().trim();
            if (contentText.isBlank()) {
                departmentNotice.markContentFailed();
                log.warn("학과 공지 본문 추출 결과가 비어 있습니다. department={}, url={}",
                        departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return;
            }

            departmentNotice.updateContent(
                    contentHtml,
                    contentText,
                    sha256(contentText),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            departmentNotice.markContentFailed();
            log.warn("학과 공지 본문 크롤링에 실패했습니다. department={}, url={}, reason={}",
                    departmentNotice.getDepartment().name(), departmentNotice.getUrl(), e.getMessage());
        }
    }

    private Element findDepartmentNoticeContentRoot(Document document, DepartmentCrawlConfig config) {
        for (String selector : config.getContentSelectors()) {
            Element selected = document.selectFirst(selector);
            if (selected != null && !selected.text().isBlank()) {
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

    @Transactional(readOnly = true)
    public ListResponseDto<DepartmentNoticeListResponse> getDepartmentNotices(Department department, String sort, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 8, sort(sort));
        Page<DepartmentNotice> departmentNotices = departmentNoticeRepository.findAllByDepartment(department, pageable);

        return ListResponseDto.of(
                departmentNotices.getTotalPages(),
                departmentNotices.getTotalElements(),
                departmentNotices.getContent().stream().map(DepartmentNoticeListResponse::of).collect(Collectors.toList())
        );
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

    private String encoding(String baseUrl) {
        return Base64.getEncoder().encodeToString(baseUrl.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAMonthAgo(String date) {
        LocalDate currentDate = LocalDate.now();
        LocalDate formedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        LocalDate oneMonthAgo = currentDate.minusMonths(1);
        return formedDate.isBefore(oneMonthAgo);
    }

    private Sort sort(String sort) {
        if (sort.equals("date")) {
            return Sort.by(Sort.Direction.DESC, "createDate", "id");
        } else if (sort.equals("view")) {
            return Sort.by(Sort.Direction.DESC, "view", "id");
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

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate content hash", e);
        }
    }
}
