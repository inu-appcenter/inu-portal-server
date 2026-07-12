package kr.inuappcenterportal.inuportal.domain.course.crawler.base;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CoursePageFetcher {

    // 브라우저 요청처럼 보이게
    private static final String USER_AGENT = "Mozilla/5.0";

    // 요청 시간제한: 5초
    private static final int TIMEOUT_MILLIS = 5000;

    /**
     * 크롤링 메서드(실패하면 총 3회 재시도)
     */
    public Document fetch(String url) {
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MILLIS)
                        .get();
            } catch (IOException e) {
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("강의 페이지를 가져오지 못했습니다. url=" + url, e);
                }

                log.warn("강의 페이지 요청 실패. 재시도합니다. attempt={}, url={}", attempt, url);
            }
        }

        throw new IllegalStateException("강의 페이지를 가져오지 못했습니다. url=" + url);
    }
}
