package kr.inuappcenterportal.inuportal.domain.course.crawler.offering;

import kr.inuappcenterportal.inuportal.domain.course.dto.DownloadedGuideFileDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CourseGuideFileParser {

    /**
     * 다운로드된 pdf파일 파싱
     */
    public String extractText(DownloadedGuideFileDto downloadedGuideFileDto) {
        try (PDDocument document = PDDocument.load(downloadedGuideFileDto.content())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("강의 편람 PDF 파싱 실패. fileName=" + downloadedGuideFileDto.fileName(), e);
        }
    }
}
