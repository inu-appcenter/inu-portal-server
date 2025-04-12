package kr.inuappcenterportal.inuportal.domain.notice.service;

import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private static long id = 0;
    private final CacheManager cacheManager;
    @Qualifier("localCacheManager")
    private final CacheManager localCacheManager;
    public NoticeService(@Qualifier("cacheManager") CacheManager cacheManager, @Qualifier("localCacheManager") CacheManager localCacheManager, NoticeRepository noticeRepository){
        this.noticeRepository = noticeRepository;
        this.cacheManager = cacheManager;
        this.localCacheManager = localCacheManager;
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


    @Transactional
    public void crawlingNotices() throws IOException {
        id = 0;
        noticeRepository.deleteAllInBatch();
        int bachelor = 1516;
        int bachelorNum = 46;
        getNoticeByCategory(bachelor, bachelorNum,"학사");
        log.info("학사공지 크롤링 완료");
        int recruitment = 1518;
        int recruitmentNum = 611;
        getNoticeByCategory(recruitment, recruitmentNum,"모집");
        log.info("모집공지 크롤링 완료");
        int exchange = 1517;
        int exchangeNum = 47;
        getNoticeByCategory(exchange, exchangeNum,"학점교류");
        log.info("학점교류공지 크롤링 완료");
        int test = 1530;
        int testNum = 52;
        getNoticeByCategory(test, testNum,"교육시험");
        log.info("교육시험공지 크롤링 완료");
    }

    private void getNoticeByCategory(int category,int categoryNum,String categoryName) throws IOException {
        String url = "https://www.inu.ac.kr/inu/" + category + "/subview.do?enc=";
        int index = 1;
        boolean outLoop = false;
        while (!outLoop) {
            String postUrl = "fnct1|@@|%2Fbbs%2Finu%2F2" + categoryNum  + "%2FartclList.do%3Fpage%3D" + index + "%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%267";
            String encodedUrl = url + encoding(postUrl);
            Document document = Jsoup.connect(encodedUrl).get();
            Elements notice = document.select("tr");
            for (Element ele : notice){
                if(ele.select("td.td-num").text().equals("일반공지")||ele.select("th.th-num").text().equals("NO")){
                    continue;
                }
                if(category==1518&&!ele.select("td.td-category").text().equals("[모집]")){
                    continue;
                }
                if(isAMonthAgo(ele.select("td.td-date").text())){
                    outLoop = true;
                    break;
                }String href = "www.inu.ac.kr"+ele.select("td.td-subject").select("a").attr("href");
                String pattern = "\\d+";//숫자로 시작하는 패턴
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(href);
                m.find();
                m.find();
                String number = m.group();
                String baseUrl = "fnct1|@@|%2Fbbs%2Finu%2F2006%2F"+number+"%2FartclView.do%3Fpage%3D3%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%26password%3D%267";
                noticeRepository.save(Notice.builder().category(categoryName)
                        .title(Objects.requireNonNull(Objects.requireNonNull(ele.select("td.td-subject").first()).selectFirst("strong").text()))
                        .url("www.inu.ac.kr/inu/"+category+"/subview.do?enc="+encoding(baseUrl))
                        .writer(ele.select("td.td-write").text())
                        .createDate(ele.select("td.td-date").text())
                        .view(Long.parseLong(ele.select("td.td-access").text()))
                        .id(++id)
                        .build());
            }
            index++;
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


}
