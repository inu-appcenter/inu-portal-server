package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.course.CourseCrawledItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.response.CourseResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private final CourseRepository courseRepository;

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
     * 크롤링 후 통합한 데이터를 DB에 적용하기 위한 메서드 호출 메서드
     */
    @Transactional
    public void applyCrawledCourses(
            Department department,
            List<CourseCrawledItemDto> crawledCourses
    ) {
        upsertCourses(department, crawledCourses);
        deactivateMissingCourses(department, crawledCourses);
    }


    /**
     * 크롤링된 강의가 DB에 있으면 수정하고, 없으면 새로 저장한다.
     */
    private void upsertCourses(
            Department department,
            List<CourseCrawledItemDto> crawledCourses
    ) {
        for (CourseCrawledItemDto crawledCourse : crawledCourses) {

            TargetGrade targetGrade = parseTargetGrade(crawledCourse.targetGrade());
            TargetTerm targetTerm = parseTargetTerm(crawledCourse.targetTerm());
            CompletionDivision completionDivision = parseCompletionDivision(crawledCourse.completionDivision());

            Optional<Course> existingCourse =
                    courseRepository.findByTitleAndDepartment(crawledCourse.title(), department);

            if (existingCourse.isPresent()) {
                existingCourse.get().updateBaseInfo(
                        targetGrade,
                        targetTerm,
                        completionDivision,
                        crawledCourse.credit()
                );

                if (crawledCourse.content() != null) {
                    existingCourse.get().updateContent(crawledCourse.content());
                }

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

    private TargetGrade parseTargetGrade(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return TargetGrade.from(value);
    }

    private TargetTerm parseTargetTerm(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return TargetTerm.from(value);
    }

    private CompletionDivision parseCompletionDivision(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return CompletionDivision.from(value);
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


    /**
     * 강의 조회 메서드
     */
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
