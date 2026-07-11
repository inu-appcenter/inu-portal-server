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
                        <thead>
                          <tr>
                            <th>학년</th>
                            <th>학기</th>
                            <th>이수구분</th>
                            <th>교과목명</th>
                            <th>학점</th>
                          </tr>
                        </thead>
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

    @Test
    @DisplayName("rowspan과 추가 컬럼이 있는 교육과정을 헤더 기준으로 파싱합니다")
    void 교육과정_rowspan_파싱_테스트() {
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <table class="curTable">
                      <thead>
                        <tr>
                          <th rowspan="2"></th>
                          <th rowspan="2">순번</th>
                          <th rowspan="2">편성<br>학기</th>
                          <th rowspan="2">과목코드</th>
                          <th rowspan="2">교과목명</th>
                          <th rowspan="2">이수구분</th>
                          <th rowspan="2">수업유형</th>
                          <th rowspan="2">학점</th>
                        </tr>
                        <tr></tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td class="tdRot">▶</td>
                          <td>1</td>
                          <td>1학기</td>
                          <td>HD06104</td>
                          <td>체조</td>
                          <td>전공심화</td>
                          <td>체육실기</td>
                          <td>1</td>
                        </tr>
                      </tbody>
                    </table>
                  </body>
                </html>
                """);

        List<CurriculumItemDto> result = curriculumParser.parse(document);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetGrade()).isBlank();
        assertThat(result.get(0).targetTerm()).isEqualTo("1학기");
        assertThat(result.get(0).completionDivision()).isEqualTo("전공심화");
        assertThat(result.get(0).title()).isEqualTo("체조");
        assertThat(result.get(0).credit()).isEqualTo("1");
    }

    @Test
    @DisplayName("caption 컬럼 설명과 1-1 학기 값을 이용해 교육과정을 파싱합니다")
    void 교육과정_caption_학년학기_파싱_테스트() {
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <table>
                      <caption>데이터과학과 전공교육과정 편성표 - 학기, 이수구분, 교과목, 학점</caption>
                      <tbody>
                        <tr>
                          <td>1-1</td>
                          <td>전공기초</td>
                          <td>데이터과학개론</td>
                          <td>3</td>
                        </tr>
                      </tbody>
                    </table>
                  </body>
                </html>
                """);

        List<CurriculumItemDto> result = curriculumParser.parse(document);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetGrade()).isEqualTo("1학년");
        assertThat(result.get(0).targetTerm()).isEqualTo("1학기");
        assertThat(result.get(0).completionDivision()).isEqualTo("전공기초");
        assertThat(result.get(0).title()).isEqualTo("데이터과학개론");
        assertThat(result.get(0).credit()).isEqualTo("3");
    }

    @Test
    @DisplayName("섹션 탭의 학년 정보를 이용해 교육과정을 파싱합니다")
    void 교육과정_섹션학년_파싱_테스트() {
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <ul id="bo_cate_ul">
                      <li><a href="#curr_04">4학년</a></li>
                    </ul>
                    <div id="curr_04">
                      <table>
                        <thead>
                          <tr>
                            <th>학기</th>
                            <th>이수구분</th>
                            <th>교과목명</th>
                            <th>학점</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>2학기</td>
                            <td>전공심화</td>
                            <td>캡스톤디자인</td>
                            <td>3</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </body>
                </html>
                """);

        List<CurriculumItemDto> result = curriculumParser.parse(document);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetGrade()).isEqualTo("4학년");
        assertThat(result.get(0).targetTerm()).isEqualTo("2학기");
        assertThat(result.get(0).completionDivision()).isEqualTo("전공심화");
        assertThat(result.get(0).title()).isEqualTo("캡스톤디자인");
        assertThat(result.get(0).credit()).isEqualTo("3");
    }
}
