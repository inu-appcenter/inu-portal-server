package kr.inuappcenterportal.inuportal.course;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCrawledItemDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseService;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CourseService.class)
@ActiveProfiles("test")
public class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("크롤링된 강의가 DB에 없으면 새로 저장한다")
    void 신규_강의_저장_테스트() {
        List<CourseCrawledItemDto> crawledCourses = List.of(
                crawledCourse("운영체제", "운영체제 설명", "2학년", "1학기", "전필", "3")
        );

        courseService.applyCrawledCourses(Department.COMPUTER_ENGINEERING, crawledCourses);

        Course course = courseRepository.findByTitleAndDepartment("운영체제", Department.COMPUTER_ENGINEERING)
                .orElseThrow();

        assertThat(course.getTitle()).isEqualTo("운영체제");
        assertThat(course.getTargetGrade()).isEqualTo(TargetGrade.SECOND);
        assertThat(course.getTargetTerm()).isEqualTo(TargetTerm.FIRST);
        assertThat(course.getCompletionDivision()).isEqualTo(CompletionDivision.ESSENTIAL_MAJOR);
        assertThat(course.getCredit()).isEqualTo("3");
        assertThat(course.getContent()).isEqualTo("운영체제 설명");
        assertThat(course.isActive()).isTrue();
    }


    @Test
    @DisplayName("이미 존재하는 강의는 크롤링된 값으로 수정한다")
    void 기존_강의_수정_테스트() {
        Course existingCourse = courseRepository.save(
                course("운영체제", Department.COMPUTER_ENGINEERING, TargetGrade.SECOND, TargetTerm.FIRST,
                        CompletionDivision.ESSENTIAL_MAJOR, "2", "기존 설명")
        );

        List<CourseCrawledItemDto> crawledCourses = List.of(
                crawledCourse("운영체제", "변경된 설명", "3학년", "2학기", "전선", "3")
        );

        courseService.applyCrawledCourses(Department.COMPUTER_ENGINEERING, crawledCourses);

        Course updatedCourse = courseRepository.findByTitleAndDepartment("운영체제", Department.COMPUTER_ENGINEERING)
                .orElseThrow();

        assertThat(updatedCourse.getId()).isEqualTo(existingCourse.getId());
        assertThat(updatedCourse.getTargetGrade()).isEqualTo(TargetGrade.THIRD);
        assertThat(updatedCourse.getTargetTerm()).isEqualTo(TargetTerm.SECOND);
        assertThat(updatedCourse.getCompletionDivision()).isEqualTo(CompletionDivision.SELECT_MAJOR);
        assertThat(updatedCourse.getCredit()).isEqualTo("3");
        assertThat(updatedCourse.getContent()).isEqualTo("변경된 설명");
        assertThat(updatedCourse.isActive()).isTrue();

        assertThat(courseRepository.findAllByDepartment(Department.COMPUTER_ENGINEERING)).hasSize(1);
    }


    @Test
    @DisplayName("DB에는 있지만 크롤링 결과에 없는 강의는 비활성화한다")
    void 누락된_강의_비활성화_테스트() {
        courseRepository.save(
                course("운영체제", Department.COMPUTER_ENGINEERING, TargetGrade.SECOND, TargetTerm.FIRST,
                        CompletionDivision.ESSENTIAL_MAJOR, "3", "운영체제 설명")
        );
        courseRepository.save(
                course("자료구조", Department.COMPUTER_ENGINEERING, TargetGrade.FIRST, TargetTerm.SECOND,
                        CompletionDivision.SELECT_MAJOR, "3", "자료구조 설명")
        );

        List<CourseCrawledItemDto> crawledCourses = List.of(
                crawledCourse("운영체제", "운영체제 설명", "2학년", "1학기", "전필", "3")
        );

        courseService.applyCrawledCourses(Department.COMPUTER_ENGINEERING, crawledCourses);

        Course activeCourse = courseRepository.findByTitleAndDepartment("운영체제", Department.COMPUTER_ENGINEERING)
                .orElseThrow();
        Course deactivatedCourse = courseRepository.findByTitleAndDepartment("자료구조", Department.COMPUTER_ENGINEERING)
                .orElseThrow();

        assertThat(activeCourse.isActive()).isTrue();
        assertThat(deactivatedCourse.isActive()).isFalse();
    }


    @Test
    @DisplayName("비활성화된 강의가 다시 크롤링되면 활성화한다")
    void 비활성화된_강의_재활성화_테스트() {
        Course inactiveCourse = course(
                "운영체제",
                Department.COMPUTER_ENGINEERING,
                TargetGrade.SECOND,
                TargetTerm.FIRST,
                CompletionDivision.ESSENTIAL_MAJOR,
                "3",
                "기존 설명"
        );
        inactiveCourse.deactivate();
        courseRepository.save(inactiveCourse);

        List<CourseCrawledItemDto> crawledCourses = List.of(
                crawledCourse("운영체제", "다시 열린 강의", "2학년", "1학기", "전필", "3")
        );

        courseService.applyCrawledCourses(Department.COMPUTER_ENGINEERING, crawledCourses);

        Course reactivatedCourse = courseRepository.findByTitleAndDepartment("운영체제", Department.COMPUTER_ENGINEERING)
                .orElseThrow();

        assertThat(reactivatedCourse.isActive()).isTrue();
        assertThat(reactivatedCourse.getContent()).isEqualTo("다시 열린 강의");
    }


    @Test
    @DisplayName("다른 학과의 강의는 동기화 대상이 아니므로 변경하지 않는다")
    void 다른_학과_강의는_변경하지_않는다() {
        Department otherDepartment = Department.ENGLISH;

        courseRepository.save(
                course("운영체제", Department.COMPUTER_ENGINEERING, TargetGrade.SECOND, TargetTerm.FIRST,
                        CompletionDivision.ESSENTIAL_MAJOR, "3", "운영체제 설명")
        );
        Course otherDepartmentCourse = courseRepository.save(
                course("영문학개론", otherDepartment, TargetGrade.FIRST, TargetTerm.FIRST,
                        CompletionDivision.ESSENTIAL_MAJOR, "3", "영문학 설명")
        );

        courseService.applyCrawledCourses(Department.COMPUTER_ENGINEERING, List.of());

        Course currentDepartmentCourse = courseRepository.findByTitleAndDepartment("운영체제", Department.COMPUTER_ENGINEERING)
                .orElseThrow();
        Course unchangedCourse = courseRepository.findByTitleAndDepartment("영문학개론", otherDepartment)
                .orElseThrow();

        assertThat(currentDepartmentCourse.isActive()).isFalse();
        assertThat(unchangedCourse.getId()).isEqualTo(otherDepartmentCourse.getId());
        assertThat(unchangedCourse.isActive()).isTrue();
    }


    private CourseCrawledItemDto crawledCourse(
            String title,
            String content,
            String targetGrade,
            String targetTerm,
            String completionDivision,
            String credit
    ) {
        return new CourseCrawledItemDto(
                title,
                content,
                targetGrade,
                targetTerm,
                completionDivision,
                credit
        );
    }

    private Course course(
            String title,
            Department department,
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            String credit,
            String content
    ) {
        return Course.create(
                title,
                department,
                department.getCollegeName(),
                targetGrade,
                targetTerm,
                completionDivision,
                credit,
                content
        );
    }
}
