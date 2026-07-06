package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.CourseOverviewParser;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CoursePageFetcher;
import kr.inuappcenterportal.inuportal.domain.course.crawler.CurriculumParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCrawledItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private final CourseRepository courseRepository;
    private final CoursePageFetcher coursePageFetcher;
    private final CourseOverviewParser courseOverviewParser;
    private final CurriculumParser curriculumParser;

    // 강의는 기본적으로 크롤링 후 파싱된 데이터를 가지고 다뤄짐.
    // 교과목개요와 교육과정을 주기적으로 크롤링 및 파싱해 DB에 있는 강의 데이터와 대조 후 추가 및 삭제 및 수정 사항을 파악해
    // 자동으로 DB안의 강의 목록이 업데이트 될 수 있도록 하는게 1차 목표
    // 강의 생성, 수정, 삭제, 조회, 크롤링 및 파싱
    //  1. URL 없으면 종료
    //  2. 교과목개요 크롤링
    //  3. 교육과정 크롤링
    //  4. title 기준으로 두 데이터 합치기
    //  5. 있으면 update
    //  6. 없으면 create
    //  7. 크롤링 결과에 없는 기존 Course는 deactivate


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
     * 파싱한 두 Items를 합쳐서 하나의 Item으로 변경
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
     * 강의 생성(동기화) 메서드
     * (한 학과의 교과목개요와 교육과정을 크롤링해서, 둘을 강의명으로 합친 뒤 DB에 없는 강의만 저장하는 메서드)
     */
    @Transactional
    public void syncBaseCourses() {
        for (Department department : Department.values()) {
            try {
                syncBaseCoursesByDepartment(department);
            } catch (Exception e) {
                log.warn("강의 기본 정보 동기화 실패");
            }
        }
    }


    /**
     * 한 학과의 교과목개요와 교육과정을 기준으로 기본 강의 목록을 동기화한다.
     */
    private void syncBaseCoursesByDepartment(Department department) {
        // null 체크
        if (department.getCourseOverviewUrl() == null || department.getCurriculumUrl() == null) {
            return;
        }

        // 크롤링 후 파싱된 데이터
        List<CourseOverviewItemDto> overviewItems = crawlCourseOverview(department);
        List<CurriculumItemDto> curriculumItems = crawlCurriculum(department);

        // 교과목개요를 강의명 기준으로 빠르게 찾기 위해 Map으로 변환
        List<CourseCrawledItemDto> crawledCoursesItems = mergeCrawledCourses(overviewItems, curriculumItems);

        // 강의 추가, 변경, 비활성화
        upsertCourses(department, crawledCoursesItems);
        deactivateMissingCourses(department, crawledCoursesItems);
    }


    /**
     * 교과목개요를 강의명으로 빠르게 찾기 위해 Map으로 변환
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


    /**
     * 크롤링된 강의가 DB에 있으면 수정하고, 없으면 새로 저장한다.
     */
    private void upsertCourses(
            Department department,
            List<CourseCrawledItemDto> crawledCourses
    ) {
        for (CourseCrawledItemDto crawledCourse : crawledCourses) {

            TargetGrade targetGrade = TargetGrade.from(crawledCourse.targetGrade());
            TargetTerm targetTerm = TargetTerm.from(crawledCourse.targetTerm());
            CompletionDivision completionDivision = CompletionDivision.from(crawledCourse.completionDivision());

            Optional<Course> existingCourse =
                    courseRepository.findByTitleAndDepartment(crawledCourse.title(), department);

            if (existingCourse.isPresent()) {
                existingCourse.get().update(
                        targetGrade,
                        targetTerm,
                        completionDivision,
                        crawledCourse.credit(),
                        crawledCourse.content()
                );
                continue;
            }

            Course course = Course.create(
                    crawledCourse.title(),
                    department,
                    department.getCollegeName(),
                    targetGrade,
                    targetTerm,
                    completionDivision,
                    crawledCourse.credit(),
                    crawledCourse.content()
            );

            courseRepository.save(course);
        }
    }


    /**
     * DB에는 있지만 현재 크롤링 결과에 없는 강의를 비활성화한다.
     */
    private void deactivateMissingCourses(
            Department department,
            List<CourseCrawledItemDto> crawledCourses
    ) {
        Set<String> crawledTitles = crawledCourses.stream()
                .map(CourseCrawledItemDto::title)
                .collect(Collectors.toSet());

        List<Course> savedCourses = courseRepository.findAllByDepartment(department);

        for (Course savedCourse : savedCourses) {
            if (!crawledTitles.contains(savedCourse.getTitle())) {
                savedCourse.deactivate();
            }
        }
    }


    @Transactional(readOnly = true)
    public List<CourseResponseDto> getCourses(Department department) {
        List<Course> courses;

        if (department == null) {
            courses = courseRepository.findAllByActiveTrue();
        } else {
            courses = courseRepository.findAllByDepartmentAndActiveTrue(department);
        }

        return courses.stream()
                .map(CourseResponseDto::from)
                .toList();
    }
}
