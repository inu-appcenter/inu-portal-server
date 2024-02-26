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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {
    private final NoticeRepository noticeRepository;

    @PostConstruct
    public void getNotice() throws IOException {
        noticeRepository.deleteAll();
        String url = "https://www.inu.ac.kr/inu/1534/subview.do";
        Document document = Jsoup.connect(url).get();
        Elements notice = document.select("tr.notice");
        log.info("공지사항 크롤링 가져온 공지사항:{}",notice.size());
        for(Element ele:notice){
            noticeRepository.save(Notice.builder().category(ele.select("td.td-category").select("span").text())
                    .title(Objects.requireNonNull(Objects.requireNonNull(ele.select("td.td-subject").first()).selectFirst("strong").text()))
                    .url(ele.select("td.td-subject").select("a").attr("href"))
                    .writer(ele.select("td.td-write").text())
                    .date(ele.select("td.td-date").text())
                    .build());
        }
    }

    @Transactional
    @Scheduled(cron = "0 0/30 * * * *")
    public void getNewNotice() throws IOException {
        noticeRepository.deleteAll();
        String url = "https://www.inu.ac.kr/inu/1534/subview.do";
        Document document = Jsoup.connect(url).get();
        Elements notice = document.select("tr.notice");
        log.info("공지사항 크롤링 가져온 공지사항:{}",notice.size());
        for(Element ele:notice){
            noticeRepository.save(Notice.builder().category(ele.select("td.td-category").select("span").text())
                    .title(Objects.requireNonNull(Objects.requireNonNull(ele.select("td.td-subject").first()).selectFirst("strong").text()))
                    .url("www.inu.ac.kr/"+ele.select("td.td-subject").select("a").attr("href"))
                    .writer(ele.select("td.td-write").text())
                    .date(ele.select("td.td-date").text())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<NoticeListResponseDto> getNoticeList(){
        return noticeRepository.findAll().stream().map(NoticeListResponseDto::new).collect(Collectors.toList());
    }
}
