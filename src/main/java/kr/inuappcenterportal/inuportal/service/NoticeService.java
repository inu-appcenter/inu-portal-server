package kr.inuappcenterportal.inuportal.service;

import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.domain.Notice;
import kr.inuappcenterportal.inuportal.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.dto.NoticePageResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {
    private final NoticeRepository noticeRepository;

    @PostConstruct
    @Transactional
    public void getNotice() throws IOException {
        crawlingNotices();
    }

    @Scheduled(cron = "0 0/30 * * * *")
    @CacheEvict(value = "noticeCache",cacheManager = "cacheManager")
    @Transactional
    public void getNewNotice() throws IOException {
        crawlingNotices();
    }
    @Transactional
    public void crawlingNotices() throws IOException {
        noticeRepository.truncateTable();
        int bachelor = 1516;
        int bachelorNum = 46;
        getNoticeByCategory(bachelor, bachelorNum,"학사");
        int recruitment = 1518;
        int recruitmentNum = 611;
        getNoticeByCategory(recruitment, recruitmentNum,"모집");
        int exchange = 1517;
        int exchangeNum = 47;
        getNoticeByCategory(exchange, exchangeNum,"학점교류");
        int test = 1530;
        int testNum = 52;
        getNoticeByCategory(test, testNum,"교육시험");
    }

    public void getNoticeByCategory(int category,int categoryNum,String categoryName) throws IOException {
        String url = "https://www.inu.ac.kr/inu/" + category + "/subview.do?enc=";
        int i = 1;
        boolean outLoop = false;
        while (!outLoop) {
            String postUrl = "fnct1|@@|%2Fbbs%2Finu%2F2" + categoryNum  + "%2FartclList.do%3Fpage%3D" + i + "%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%267";
            String encodedUrl = url + encoding(postUrl);
            Document document = Jsoup.connect(encodedUrl).get();
            Elements notice = document.select("tr");
            log.info("가져온 크기 :{}, 게시판 번호 : {} , 현재 인덱스 : {}",notice.size(), category,i);
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
                        .build());
            }
            i++;
        }
    }

    @Transactional(readOnly = true)
    public NoticePageResponseDto getNoticeList(String category, String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        List<NoticeListResponseDto> notices;
        long pages;
        if(category==null) {
            if (sort == null||sort.equals("date")) {
                notices =  noticeRepository.findAllByOrderByCreateDateDesc(pageable).stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
            } else if (sort.equals("view")) {
                notices = noticeRepository.findAllByOrderByViewDesc(pageable).stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
            } else {
                throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
            }
            pages = (long)Math.ceil((double)noticeRepository.count()/8);
        }
        else{
            if(sort==null||sort.equals("date")) {
                notices = noticeRepository.findAllByCategory(category, pageable).stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
            }
            else if(sort.equals("view")){
                notices = noticeRepository.findAllByCategoryOrderByViewDesc(category, pageable).stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
            }
            else{
                throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
            }
            pages = (long)Math.ceil((double)noticeRepository.countAllByCategory(category)/8);

        }
        return NoticePageResponseDto.of(pages,notices);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "noticeCache",cacheManager = "cacheManager")
    public List<NoticeListResponseDto> getTop(){
        return noticeRepository.findTop12().stream().map(NoticeListResponseDto::of).collect(Collectors.toList());
    }


    public String encoding(String baseUrl)  {
        return Base64.getEncoder().encodeToString(baseUrl.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isAMonthAgo(String date){
        LocalDate currentDate = LocalDate.now();
        LocalDate formedDate = LocalDate.parse(date,DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        LocalDate oneMonthAgo = currentDate.minusMonths(1);
        return formedDate.isBefore(oneMonthAgo);
    }


}
