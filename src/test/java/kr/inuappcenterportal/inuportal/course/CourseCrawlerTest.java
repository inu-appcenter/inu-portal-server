package kr.inuappcenterportal.inuportal.course;


import kr.inuappcenterportal.inuportal.domain.course.crawler.CourseOverviewParser;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CoursePageFetcher;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CurriculumParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCrawlerTest {
    private final CoursePageFetcher coursePageFetcher = new CoursePageFetcher();
    private final CourseOverviewParser courseOverviewParser = new CourseOverviewParser();
    private final CurriculumParser curriculumParser = new CurriculumParser();

    @Test
    @DisplayName("교과목개요 페이지 HTML을 가져온다")
    void 강의정보_패치_테스트() {
        // Given
        String url = "https://english.inu.ac.kr/ui/1973/subview.do";

        // When
        Document document = coursePageFetcher.fetch(url);

        // Then
        assertThat(document).isNotNull();
        assertThat(document.title()).isNotBlank();

//        System.out.println(document.title());
    }

    @Test
    @DisplayName("교과목개요의 정보를 가져옵니다")
    void 교과목개요_파싱_테스트() {
        // Given
        String url = "https://english.inu.ac.kr/ui/1973/subview.do";

        // When
        Document document = coursePageFetcher.fetch(url);
        List<CourseOverviewItemDto> courseOverviewItems = courseOverviewParser.parse(document);

        // Then
        assertThat(courseOverviewItems).isNotEmpty();
        assertThat(courseOverviewItems)
                .allSatisfy(item -> {
                    assertThat(item.title()).isNotBlank();
                    assertThat(item.content()).isNotBlank();
                });

//        courseOverviewItems.stream()
//                .limit(20)
//                .forEach(System.out::println);
    }

    @Test
    @DisplayName("교육과정 정보를 가져옵니다")
    void 교육과정_파싱_테스트() {
        // Given
        String url = "https://inufrance.inu.ac.kr/inufrance/1936/subview.do";

        // When
        Document document = coursePageFetcher.fetch(url);
        List<CurriculumItemDto> curriculumItems = curriculumParser.parse(document);

        // Then
        assertThat(curriculumItems).isNotEmpty();
        assertThat(curriculumItems)
                .allSatisfy(item -> {
                    assertThat(item.title()).isNotBlank();
                    assertThat(item.targetGrade()).isNotBlank();
                    assertThat(item.targetTerm()).isNotBlank();
                    assertThat(item.completionDivision()).isNotBlank();
                    assertThat(item.credit()).isNotBlank();
                });

//        curriculumItems.stream()
//                .limit(20)
//                .forEach(System.out::println);
    }
}
