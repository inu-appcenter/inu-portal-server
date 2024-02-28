package kr.inuappcenterportal.inuportal.service;

import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.domain.Notice;
import kr.inuappcenterportal.inuportal.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        getNotices();
    }

    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void getNewNotice() throws IOException {
        getNotices();
    }
    @Transactional
    public void getNotices() throws IOException {
        noticeRepository.truncateTable();
        String url = "https://www.inu.ac.kr/inu/1534/subview.do";
        Document document = Jsoup.connect(url).get();
        Elements notice = document.select("tr.notice");
        log.info("공지사항 크롤링 가져온 공지사항:{}",notice.size());
        for(Element ele:notice){
            String href = "www.inu.ac.kr"+ele.select("td.td-subject").select("a").attr("href");
            String pattern = "\\d+";//숫자로 시작하는 패턴
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(href);
            m.find();
            m.find();
            String number = m.group();
            String postUrl = "fnct1|@@|%2Fbbs%2Finu%2F2006%2F"+number+"%2FartclView.do%3Fpage%3D3%26srchColumn%3D%26srchWrd%3D%26bbsClSeq%3D%26bbsOpenWrdSeq%3D%26rgsBgndeStr%3D%26rgsEnddeStr%3D%26isViewMine%3Dfalse%26password%3D%267";
            noticeRepository.save(Notice.builder().category(ele.select("td.td-category").select("span").text())
                    .title(Objects.requireNonNull(Objects.requireNonNull(ele.select("td.td-subject").first()).selectFirst("strong").text()))
                    .url("www.inu.ac.kr/inu/1534/subview.do?enc="+encoding(postUrl))
                    .writer(ele.select("td.td-write").text())
                    .date(ele.select("td.td-date").text())
                    .view(Integer.parseInt(ele.select("td.td-access").text()))
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<NoticeListResponseDto> getNoticeList(){
        return noticeRepository.findAll().stream().map(NoticeListResponseDto::new).collect(Collectors.toList());
    }

    public String encoding(String baseUrl) throws UnsupportedEncodingException {
        String encodeResult = URLEncoder.encode(baseUrl, StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().encodeToString(encodeResult.getBytes());
    }


}
