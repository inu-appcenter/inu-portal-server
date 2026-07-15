package kr.inuappcenterportal.inuportal.domain.course.crawler.offering;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseGuideFileDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.CourseOfferingGuideType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
public class CourseGuidePageParser {
    private static final String INU_BASE_URL = "https://www.inu.ac.kr";

    /**
     * 편람 공지 본문 전체 HTML에서 첨부파일 다운로드 링크만 골라서 DTO 리스트로 바꾸는 메서드
     */
    public List<CourseGuideFileDto> parseGuideFiles(Document document) {
        Elements links = document.select("dd.insert a[href*=download.do]");

        return links.stream()
                .map(this::toGuideFile)
                .filter(guideFile -> guideFile.type() != CourseOfferingGuideType.UNKNOWN)
                .filter(guideFile -> guideFile.fileName().toLowerCase().endsWith(".pdf"))
                .toList();
    }

    /**
     * 학사공지 DB에서 꺼낸 강의 편람의 파일 이름과 다운로드 링크 파싱
     */
    private CourseGuideFileDto toGuideFile(Element link) {
        String fileName = link.text().trim();
        String href = link.attr("href").trim();

        String downloadUrl = URI.create(INU_BASE_URL)
                .resolve(href)
                .toString();

        return new CourseGuideFileDto(
                fileName,
                downloadUrl,
                classify(fileName)
        );
    }

    /**
     * 파일명을 기준으로 구분
     */
    private CourseOfferingGuideType classify(String fileName) {
        if (fileName.contains("기초교양")) {
            return CourseOfferingGuideType.BASIC_GENERAL;
        }
        if (fileName.contains("핵심") || fileName.contains("심화교양")) {
            return CourseOfferingGuideType.CORE_DEEP_GENERAL;
        }
        if (fileName.contains("기초과학") || fileName.contains("공학")) {
            return CourseOfferingGuideType.BASIC_SCIENCE_ENGINEERING;
        }
        if (fileName.contains("교직")) {
            return CourseOfferingGuideType.TEACHING;
        }
        if (fileName.contains("연계전공")) {
            return CourseOfferingGuideType.LINKED_MAJOR;
        }
        if (fileName.contains("기타") || fileName.contains("일반선택") || fileName.contains("군사학")) {
            return CourseOfferingGuideType.COMMON_MILITARY;
        }
        if (fileName.contains("학과별 개설과목") || fileName.contains("전공")) {
            return CourseOfferingGuideType.MAJOR;
        }

        return CourseOfferingGuideType.UNKNOWN;
    }

}
