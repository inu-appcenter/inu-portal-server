package kr.inuappcenterportal.inuportal.domain.course.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CoursePageFetcher {

    // 브라우저 요청처럼 보이게
    private static final String USER_AGENT = "Mozilla/5.0";

    // 요청 시간제한: 5초
    private static final int TIMEOUT_MILLIS = 5000;

    public Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .get();
        } catch (IOException e) {
            throw new IllegalStateException("강의 페이지를 가져오지 못했습니다. url=" + url, e);
        }
    }
}
