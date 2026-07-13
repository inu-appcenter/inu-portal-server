package kr.inuappcenterportal.inuportal.domain.course.crawler.offering;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseGuideFileDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.DownloadedGuideFileDto;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CourseGuideFileDownloader {

    /**
     * CourseGuideFileFetcher에서 받아온 강의 편람 pdf 다운로드
     */
    public DownloadedGuideFileDto download(CourseGuideFileDto guideFileDto) {
        try {
            byte[] content = Jsoup.connect(guideFileDto.downloadUrl())
                    .userAgent("Mozilla/5.0")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .timeout(10000)
                    .execute()
                    .bodyAsBytes();

            return new DownloadedGuideFileDto(
                    guideFileDto.fileName(),
                    guideFileDto.downloadUrl(),
                    guideFileDto.type(),
                    content
            );
        } catch (IOException e) {
            throw new IllegalStateException("수강신청편람 파일 다운로드에 실패했습니다. url=" + guideFileDto.downloadUrl(), e);
        }
    }
}
