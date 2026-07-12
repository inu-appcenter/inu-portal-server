package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CourseOverviewParser;
import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CoursePageFetcher;
import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CurriculumParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCrawledItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlerService {

    private final CourseService courseService;
    private final CoursePageFetcher coursePageFetcher;
    private final CourseOverviewParser courseOverviewParser;
    private final CurriculumParser curriculumParser;

    /**
     * 강의 생성(동기화) 메서드
     * (한 학과의 교과목개요와 교육과정을 크롤링해서, 강의명을 기준으로 합친 뒤 DB에 없는 강의만 저장하는 메서드)
     * 스케쥴러 대상 메서드
     */
    public void syncBaseCourses() {

        // 학과별로 동기화 로직 호출
        for (Department department : Department.values()) {
            try {
                if (department.getCourseOverviewUrl() == null || department.getCurriculumUrl() == null) {
                    continue;
                }

                // 크롤링,파싱 후 merge(크롤링 실패해도 내부에서 재시작)
                List<CourseOverviewItemDto> overviewItems = crawlCourseOverview(department);
                List<CurriculumItemDto> curriculumItems = crawlCurriculum(department);
                List<CourseCrawledItemDto> crawledCourses = mergeCrawledCourses(overviewItems, curriculumItems);

                // DB 동기화 로직 호출
                courseService.applyCrawledCourses(department, crawledCourses);

            } catch (Exception e) {
                log.warn("강의 기본 정보 동기화 실패. department={}", department, e);
            }
        }
    }


    /**
     * 교과목개요 크롤링 및 파싱 (title, content를 가져옴)
     */
    private List<CourseOverviewItemDto> crawlCourseOverview(Department department) {
        String url = department.getCourseOverviewUrl();

        Document document = coursePageFetcher.fetch(url);

        return courseOverviewParser.parse(document);
    }


    /**
     * 교육과정 크롤링 및 파싱 (targetGrade, targetTerm, completionDivision, credit, title을 가져옴)
     */
    private List<CurriculumItemDto> crawlCurriculum(Department department) {
        String url = department.getCurriculumUrl();

        Document document = coursePageFetcher.fetch(url);

        return curriculumParser.parse(document);
    }


    /**
     * 파싱한 두 Items들을 합쳐서 하나의 Item으로 변경(리스트)
     */
    private List<CourseCrawledItemDto> mergeCrawledCourses(
            List<CourseOverviewItemDto> overviewItems,
            List<CurriculumItemDto> curriculumItems
    ) {
        Map<String, CourseOverviewItemDto> overviewMap = toOverviewMap(overviewItems);

        return curriculumItems.stream()
                .map(curriculumItem -> {
                    CourseOverviewItemDto overviewItem = overviewMap.get(curriculumItem.title());

                    if (overviewItem == null) {
                        return null;
                    }

                    return new CourseCrawledItemDto(
                            curriculumItem.title(),
                            overviewItem.content(),
                            curriculumItem.targetGrade(),
                            curriculumItem.targetTerm(),
                            curriculumItem.completionDivision(),
                            curriculumItem.credit()
                    );
                })
                .filter(course -> course != null)
                .toList();
    }


    /**
     * 교과목개요를 강의명으로 빠르게 찾기 위해 Map으로 변환하는 메서드
     * ex) 운영체제 -> CourseOverviewDto("운영체제", "본 강의는...")
     */
    private Map<String, CourseOverviewItemDto> toOverviewMap(List<CourseOverviewItemDto> overviewItems) {
        return overviewItems.stream()
                .collect(Collectors.toMap(
                        CourseOverviewItemDto::title,                                          // key
                        overviewItem -> overviewItem,                   // value
                        (first, second) -> first // 중복 키 처리
                ));
    }
}
