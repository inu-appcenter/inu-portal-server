package kr.inuappcenterportal.inuportal.course;


import kr.inuappcenterportal.inuportal.domain.course.crawler.CourseOverviewParser;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CoursePageFetcher;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CurriculumParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCrawlerTest {
    private final CoursePageFetcher coursePageFetcher = new CoursePageFetcher();
    private final CourseOverviewParser courseOverviewParser = new CourseOverviewParser();
    private final CurriculumParser curriculumParser = new CurriculumParser();

    @Disabled("외부 학교 페이지에 의존하는 수동 확인용 테스트")
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
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <ul class="hBox6">
                      <li>
                        <div class="tit">운영체제</div>
                        <div class="cont">운영체제 설명</div>
                      </li>
                      <li>
                        <div class="tit">자료구조</div>
                        <div class="cont">자료구조 설명</div>
                      </li>
                    </ul>
                  </body>
                </html>
                """);

        // When
        List<CourseOverviewItemDto> result = courseOverviewParser.parse(document);


        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("운영체제");
        assertThat(result.get(0).content()).isEqualTo("운영체제 설명");
        assertThat(result.get(1).title()).isEqualTo("자료구조");
        assertThat(result.get(1).content()).isEqualTo("자료구조 설명");
    }

    @Test
    @DisplayName("교육과정 정보를 가져옵니다")
    void 교육과정_파싱_테스트() {
        // Given
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <div class="func-table">
                      <table>
                        <tbody>
                          <tr>
                            <td>2학년</td>
                            <td>1학기</td>
                            <td>전필</td>
                            <td>운영체제</td>
                            <td>3</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </body>
                </html>
                """);

        // When
        List<CurriculumItemDto> result = curriculumParser.parse(document);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetGrade()).isEqualTo("2학년");
        assertThat(result.get(0).targetTerm()).isEqualTo("1학기");
        assertThat(result.get(0).completionDivision()).isEqualTo("전필");
        assertThat(result.get(0).title()).isEqualTo("운영체제");
        assertThat(result.get(0).credit()).isEqualTo("3");
    }
}
