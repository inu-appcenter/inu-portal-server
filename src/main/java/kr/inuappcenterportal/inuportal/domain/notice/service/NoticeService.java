package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeListResponse;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final DepartmentCrawlerStateRepository departmentCrawlerStateRepository;
    private final CacheManager cacheManager;
    private final KeywordService keywordService;
    private static long id = 0;
    private static final String DEPT_INDEX_KEY = "departmentIndex";
    private static final int DEPT_SIZE = 4;

    @Qualifier("localCacheManager")
    private final CacheManager localCacheManager;

    public NoticeService(@Qualifier("cacheManager") CacheManager cacheManager,
                         @Qualifier("localCacheManager") CacheManager localCacheManager,
                         NoticeRepository noticeRepository,
                         DepartmentNoticeRepository departmentNoticeRepository,
                         DepartmentCrawlerStateRepository departmentCrawlerStateRepository,
                         KeywordService keywordService) {
        this.noticeRepository = noticeRepository;
        this.departmentNoticeRepository = departmentNoticeRepository;
        this.cacheManager = cacheManager;
        this.localCacheManager = localCacheManager;
        this.departmentCrawlerStateRepository = departmentCrawlerStateRepository;
        this.keywordService = keywordService;
    }
    /*@PostConstruct
    @Transactional
    public void getNotice() throws IOException {
        crawlingNotices();
    }*/

    @Scheduled(cron = "0 0/30 * * * *")
    @CacheEvict(value = "noticeCache",cacheManager = "cacheManager")
    @Transactional
    public void getNewNotice() throws IOException {
        crawlingNotices();
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @CacheEvict(value = "noticeCache",cacheManager = "cacheManager")
    @Transactional
    public void getNewDepartmentNotice() throws IOException {
        Department[] departments = Department.values();

        int start = getDepartmentIndex();
        int end = Math.min(start + DEPT_SIZE, departments.length);

        crawlingDepartmentNotices(departments, start, end);

        setDepartmentIndex((end >= departments.length) ? 0 : end);

        log.info("학과 공지 크롤링 완료, 시작 인덱스: {}, 끝 인덱스: {}", start, end);
    }

    @Transactional
    public void crawlingNotices()  {
        id = 0;
        noticeRepository.deleteAllInBatch();
        int bachelor = 1516;
        int bachelorNum = 46;
        getNoticeByCategory(bachelor, bachelorNum,"학사");
        log.info("학사공지 크롤링 완료");
        int credit = 1517;
        int creditNum = 47;
        getNoticeByCategory(credit, creditNum,"학점교류");
        log.info("학점교류 공지 크롤링 완료");
        int recruitment = 1518;
        int recruitmentNum = 611;
        getNoticeByCategory(recruitment, recruitmentNum,"일반/행사/모집");
        log.info("일반/행사/모집 공지 크롤링 완료");
        int scholarship = 1519;
        int scholarshipNum = 49;
        getNoticeByCategory(scholarship, scholarshipNum,"장학금");
        log.info("장학금 크롤링 완료");
        int tuition = 1520;
        int tuitionNum = 50;
        getNoticeByCategory(tuition, tuitionNum,"등록금 납부");
        log.info("등록금 납부 공지 크롤링 완료");
        int test = 1530;
        int testNum = 52;
        getNoticeByCategory(test, testNum,"교육시험");
        log.info("교육시험공지 크롤링 완료");
    }

    private void getNoticeByCategory(int category,int categoryNum,String categoryName) {
        try {
            String url = "https://www.inu.ac.kr/inu/" + category + "/subview.do?enc=";
            int index = 1;
            boolean outLoop = false;
            while (!outLoop) {
                String postUrl = "fnct1|@@|%2Fbbs%2Finu%2F2" + categoryNum + "%2FartclList.do%3Fpage%3D" + index + "%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%267";
                String encodedUrl = url + encoding(postUrl);
                Document document = Jsoup.connect(encodedUrl).get();
                Elements notice = document.select("tr");
                for (Element ele : notice) {
                    if (ele.select("th.th-num").text().equals("NO")) {
                        continue;
                    }
                    if (isAMonthAgo(ele.select("td.td-date").text())) {
                        outLoop = true;
                        break;
                    }
                    String href = "www.inu.ac.kr" + ele.select("td.td-subject").select("a").attr("href");
                    String pattern = "\\d+";//숫자로 시작하는 패턴
                    Pattern p = Pattern.compile(pattern);
                    Matcher m = p.matcher(href);
                    m.find();
                    m.find();
                    String number = m.group();
                    String baseUrl = "fnct1|@@|%2Fbbs%2Finu%2F2006%2F" + number + "%2FartclView.do%3Fpage%3D3%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%26password%3D%267";
                    noticeRepository.save(Notice.builder().category(categoryName)
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
        }catch (Exception e){
            log.warn("{} 공지 크롤링 실패 : {}",categoryName,e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto<NoticeListResponseDto> getNoticeList(String category, String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8,sort(sort));
        Page<Notice> notices;
        if(category==null) {
            notices = noticeRepository.findAllBy(pageable);
        }
        else{
            notices = noticeRepository.findAllByCategory(category, pageable);
        }
        return ListResponseDto.of(notices.getTotalPages(),notices.getTotalElements(),notices.getContent().stream().map(NoticeListResponseDto::of).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    //@Cacheable(value = "noticeCache",cacheManager = "cacheManager")
    public List<NoticeListResponseDto> getTop(){
        // 레디스 캐시 호출
        try {
            Cache cache = cacheManager.getCache("noticeCache");
            List<NoticeListResponseDto> list = cache.get("noticeTop", List.class);
            if(list!=null){
                return list;
            }
        }catch (Exception e){
            log.warn("메인 화면 공지 - 레디스 캐시 접근 실패, 로컬 캐시 접근");
        }
        // 레디스 캐시 호출 실패 시 로컬 캐시 호출
        try {
            Cache cache = localCacheManager.getCache("noticeCache");
            List<NoticeListResponseDto> list = cache.get("noticeTop", List.class);
            if(list!=null){
                return list;
            }
        }catch (Exception e){
            log.warn("메인 화면 공지 - 로컬 캐시 접근 실패, 데이터베이스 접근");
        }
        // DB 조회
        List<NoticeListResponseDto> notices = noticeRepository.findTop12().stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
        // 레디스 캐시 등록 - 성공 시 로컬 캐시 사용하지 않음
        try {
            cacheManager.getCache("noticeCache").put("noticeTop",notices);
            return notices;
        }catch (Exception e){
            log.warn("메인 화면 공지 - 레디스 캐시 등록 실패");
        }

        //로컬 캐시 등록
        try {
            localCacheManager.getCache("noticeCache").put("noticeTop",notices);
        }catch (Exception e){
            log.warn(e.getMessage());
            log.warn("메인 화면 공지 - 로컬 캐시 등록 실패");
        }
        return notices;
    }

    @Transactional
    public void crawlingDepartmentNotices(Department[] departments, int start, int end)  {

        for (int i = start; i < end; i++) {
            Department department = departments[i];
            if (department == Department.SPORTS_SCIENCE) {
                getNoticeByDepartment(department, new DepartmentCrawlConfig(
                        "td.td_subject a",
                        "td.td_datetime",
                        "td.td_subject a",
                        "td.td_num.td_hit2",
                        ele -> !ele.select("strong.notice_icon").isEmpty(),
                        false
                ));
            } else {
                getNoticeByDepartment(department, new DepartmentCrawlConfig(
                        "td.td-subject a strong",
                        "td.td-date",
                        "td.td-subject a",
                        "td.td-access",
                        ele -> {
                            String num = ele.select("td.td-num").text();
                            return "일반공지".equals(num) || "NO".equals(num) || "전체게시판공지".equals(num);
                        },
                        true
                ));
            }
        }
    }

    private void getNoticeByDepartment(Department department, DepartmentCrawlConfig config) {
        try {
            String url = department.getUrls();
            int index = 1;
            int count = 0;
            int limit = 4;
            boolean outLoop = false;

            while (!outLoop) {
                String pageUrl = url + (url.contains("?") ? "&" : "?") + "page=" + index;
                Document document = Jsoup.connect(pageUrl).get();

                if (document.text().contains("접근 권한이 없습니다.")) {
                    log.warn("접근 권한 없음 메시지 발견, {} 공지 크롤링 중단", department.getDepartmentName());
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
                    String href = config.isUseAbsoluteHref()
                            ? ele.select(config.getLinkSelector()).attr("abs:href")
                            : ele.select(config.getLinkSelector()).attr("href");
                    Long views = Long.parseLong(ele.select(config.getViewsSelector()).text());

                    Optional<DepartmentNotice> existingDepartmentNotice = departmentNoticeRepository
                            .findFirstByDepartmentAndTitleAndCreateDate(department, title, date);

                    if (existingDepartmentNotice.isPresent()) {
                        existingDepartmentNotice.get().updateView(views);
                        continue;
                    } else {
                        DepartmentNotice departmentNotice =
                                departmentNoticeRepository.save(DepartmentNotice.create(department, title, date, views, href));

                        keywordService.departmentNotifyMatchedUsers(departmentNotice, department);
                        keywordService.departmentNotifyMatchedUsersAndKeyword(departmentNotice, department);
                    }

                    count++;
                    if (count >= limit) {
                        outLoop = true;
                        break;
                    }
                }
                index++;
            }
            log.info("{} 공지 크롤링 완료", department.getDepartmentName());
        } catch (Exception e){
            log.warn("{} 공지 크롤링 실패 : {}", department.getDepartmentName(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto<DepartmentNoticeListResponse> getDepartmentNotices(Department department, String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8, sort(sort));
        Page<DepartmentNotice> departmentNotices = departmentNoticeRepository.findAllByDepartment(department, pageable);

        return ListResponseDto.of(departmentNotices.getTotalPages(),departmentNotices.getTotalElements(),departmentNotices.getContent().stream().map(DepartmentNoticeListResponse::of).collect(Collectors.toList()));
    }

    private String encoding(String baseUrl)  {
        return Base64.getEncoder().encodeToString(baseUrl.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAMonthAgo(String date){
        LocalDate currentDate = LocalDate.now();
        LocalDate formedDate = LocalDate.parse(date,DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        LocalDate oneMonthAgo = currentDate.minusMonths(1);
        return formedDate.isBefore(oneMonthAgo);
    }

    private Sort sort(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "createDate","id");
        }
        else if(sort.equals("view")) {
            return Sort.by(Sort.Direction.DESC, "view", "id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    private int getDepartmentIndex() {
        return departmentCrawlerStateRepository.findByDeptKey(DEPT_INDEX_KEY)
                .map(DepartmentCrawlerState::getDeptIndex)
                .orElse(0);
    }

    private void setDepartmentIndex(int deptIndex) {
        DepartmentCrawlerState state = departmentCrawlerStateRepository.findByDeptKey(DEPT_INDEX_KEY)
                .orElse(new DepartmentCrawlerState(DEPT_INDEX_KEY, 0));
        state.updateIndex(deptIndex);
        departmentCrawlerStateRepository.save(state);
    }
}
