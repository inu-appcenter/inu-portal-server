package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCommand;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseOfferingService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final CourseMeetingRepository courseMeetingRepository;

    /**
     * 개설 강의 조건별 조회
     */
    public Page<CourseOfferingResponseDto> getCourseOfferings(
            Integer year,
            SemesterTerm term,
            String deptName,
            String collegeName,
            String hyName,
            String isuName,
            String isuFldName,
            String ssupTypeName,
            String englishName,
            Integer credit,
            String keyword,
            Pageable pageable
    ) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        CourseOfferingSearchCondition condition = new CourseOfferingSearchCondition(
                semester.getId(),
                toDeptName(deptName),
                toCollegeName(collegeName),
                toHyName(hyName),
                toIsuName(isuName),
                toIsuFldName(isuFldName),
                toSsupTypeName(ssupTypeName),
                toEnglishName(englishName),
                credit,
                keyword
        );

        Page<CourseOffering> courseOfferings = courseOfferingRepository.search(condition, pageable);

        List<Long> courseOfferingIds = courseOfferings.getContent().stream()
                .map(CourseOffering::getId)
                .toList();

        Map<Long, List<CourseMeeting>> meetingsByCourseOfferingId =
                courseMeetingRepository.findAllByCourseOfferingIdIn(courseOfferingIds).stream()
                        .collect(Collectors.groupingBy(
                                meeting -> meeting.getCourseOffering().getId()
                        ));

        return courseOfferings.map(courseOffering -> CourseOfferingResponseDto.from(
                courseOffering,
                meetingsByCourseOfferingId.getOrDefault(courseOffering.getId(), List.of())
        ));
    }

    private DEPT_NAME toDeptName(String value) {
        return isBlank(value) ? null : DEPT_NAME.from(value);
    }

    private COLLEGE_NAME toCollegeName(String value) {
        return isBlank(value) ? null : COLLEGE_NAME.from(value);
    }

    private HY_NAME toHyName(String value) {
        return isBlank(value) ? null : HY_NAME.from(value);
    }

    private ISU_NAME toIsuName(String value) {
        return isBlank(value) ? null : ISU_NAME.from(value);
    }

    private ISU_FLD_NAME toIsuFldName(String value) {
        return isBlank(value) ? null : ISU_FLD_NAME.from(value);
    }

    private SSUP_TYPE_NAME toSsupTypeName(String value) {
        return isBlank(value) ? null : SSUP_TYPE_NAME.from(value);
    }

    private ENGLISH_NAME toEnglishName(String value) {
        return isBlank(value) ? null : ENGLISH_NAME.from(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }


    /**
     * 개설 강의 생성
     */
    @Transactional
    public CourseOfferingResponseDto upsertCourseOfferings(CourseOfferingApiItem request) {

        // 학기 정보 가져오기
        Semester semester = semesterRepository.findByYearAndTerm(
                Integer.parseInt(request.year()),
                SemesterTerm.mapToTermCode(request.termCode())
        ).orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        // 개설 강의와 연결할 기본 강의: 있으면 사용, 없으면 생성
        Course course = resolveCourse(toCourseCommand(request));

        // 중복되는 개설 강의가 있으면 업데이트, 없으면 새로 생성
        CourseOffering offering = courseOfferingRepository
                .findBySemesterIdAndSubjectNumber(semester.getId(), request.haksuCode())
                .map(existing -> {
                    existing.updateFromApi(
                            course,
                            CNCTR_ISU_NAME.from(request.cnctrIsuName()),
                            DEPT_NAME.from(request.deptName()),
                            COLLEGE_NAME.from(request.collegeName()),
                            ISU_FLD_NAME.from(request.isuFldName()),
                            ISU_NAME.from(request.isuName()),
                            SSUP_TYPE_NAME.from(request.suupTypeName()),
                            HY_NAME.from(request.hyName()),
                            ENGLISH_NAME.from(request.englishName()),
                            request.credit()
                    );
                    return existing;
                })
                .orElseGet(() -> courseOfferingRepository.save(
                        CourseOffering.create(
                                null,
                                request.haksuCode(),
                                null,
                                course,
                                semester,
                                CNCTR_ISU_NAME.from(request.cnctrIsuName()),
                                DEPT_NAME.from(request.deptName()),
                                COLLEGE_NAME.from(request.collegeName()),
                                ISU_FLD_NAME.from(request.isuFldName()),
                                ISU_NAME.from(request.isuName()),
                                SSUP_TYPE_NAME.from(request.suupTypeName()),
                                HY_NAME.from(request.hyName()),
                                ENGLISH_NAME.from(request.englishName()),
                                request.credit(),
                                null,
                                null,
                                null
                        )
                ));

        return CourseOfferingResponseDto.from(offering, List.of());
    }

    /**
     * 기본 강의 데이터에 강의가 존재하면 그 강의를 사용
     * 존재하지 않으면 기본 강의 데이터 생성
     */
    private Course resolveCourse(CourseCommand command) {

        // courseId가 있으면 그 Course를 바로 사용
        if (command.courseId() != null) {
            return courseRepository.findById(command.courseId())
                    .orElseThrow(() -> new MyException(MyErrorCode.COURSE_NOT_FOUND));
        }

        // courseCode가 있으면 courseCode로 찾음
        if (command.courseCode() != null && !command.courseCode().isBlank()) {
            Optional<Course> byCode = courseRepository.findByCourseCode(command.courseCode());
            if (byCode.isPresent()) {
                Course course = byCode.get();
                course.updateApiInfo(
                        command.courseCode(),
                        command.targetGrade(),
                        command.completionDivision(),
                        command.credit()
                );
                return course;
            }
        }

        // courseCode로 못 찾으면 title + department로 찾음
        Optional<Course> byTitleAndDepartment =
                courseRepository.findByTitleAndDepartment(command.title(), command.department());
        if (byTitleAndDepartment.isPresent()) {
            Course course = byTitleAndDepartment.get();
            course.updateApiInfo(
                    command.courseCode(),
                    command.targetGrade(),
                    command.completionDivision(),
                    command.credit()
            );
            return course;
        }

        // 그래도 없으면 Course를 새로 생성
        Course course = Course.create(
                command.title(),
                command.englishTitle(),
                command.department(),
                command.department().getCollegeName(),
                command.targetGrade(),
                null,
                command.completionDivision(),
                command.credit(),
                null
        );
        course.updateApiInfo(command.courseCode(), command.targetGrade(), command.completionDivision(), command.credit());
        return courseRepository.save(course);
    }

    // 개설 강의 생성 dto로 내부 객체 생성
    private CourseCommand toCourseCommand(CourseOfferingApiItem item) {
        return new CourseCommand(
                null,
                item.courseCode(),
                item.courseNameKor(),
                item.courseNameEng(),
                Department.from(item.deptName()),
                TargetGrade.from(item.hyName()),
                CompletionDivision.from(item.isuName()),
                item.credit()
        );
    }

    /**
     * 개설 강의 수정
     */
    @Transactional
    public CourseOfferingResponseDto update() {
        return null;
    }


    /**
     * 개설 강의 삭제
     */
    @Transactional
    public CourseOfferingResponseDto delete() {
        return null;
    }
}
