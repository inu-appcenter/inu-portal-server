package kr.inuappcenterportal.inuportal.domain.course.crawler.offering;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseGuideFileDto;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseGuideFileFetcher {
    private final NoticeRepository noticeRepository;
    private final CourseGuidePageParser courseGuidePageParser;

    /**
     * 가장 최근 "수강신청편람" 학사 공지를 찾아서 그 안에 있는 강의 다운로드 url을 가진 Dto를 만들어서 가져오는 메서드
     */
    public List<CourseGuideFileDto> fetchCourseGuideFiles() {
        // 학사 공지 DB에서 가장 최근 "수강신청편람" 을 포함한 공지를 찾아서 반환
        Notice notice = noticeRepository.findFirstByTitleContainingOrderByCreateDateDesc("수강신청편람")
                .orElseThrow(() -> new IllegalStateException("수강신청편람 공지를 찾을 수 없습니다."));

        // 수강신청편람의 url 크롤링
        Document document = fetchCourseGuidePageDocument(notice.getUrl());

        // 강의 편람 다운로드 url을 포함한 dto를 수강신청편람의 url을 크롤링한 후 파싱해서 생성
        return courseGuidePageParser.parseGuideFiles(document);
    }

    /**
     * 수강신청편람 url을 크롤링한 전체 html 파일 가져오는 메서드
     */
    private Document fetchCourseGuidePageDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            throw new IllegalStateException("수강신청편람 공지 페이지 조회에 실패했습니다. url=" + url, e);
        }
    }
}
