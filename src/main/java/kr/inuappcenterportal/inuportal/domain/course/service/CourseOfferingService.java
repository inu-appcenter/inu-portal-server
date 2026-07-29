package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCommand;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseOfferingService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final SemesterRepository semesterRepository;


    /**
     * 개설 강의 조회
     */
    public CourseOfferingResponseDto getCourseOffering() {
        return null;
    }


    /**
     * 개설 강의 생성
     */
    @Transactional
    public CourseOfferingResponseDto create(CourseOfferingCreateRequestDto request) {

        // 기본 강의 데이터 가져오기
        Course course = resolveCourse(toCourseCommand(request));

        // 현재 학기 가져오기
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        // 학기+학수번호로 중복되는 개설 강의 검증
        if (courseOfferingRepository.existsBySemesterIdAndSubjectNumber(
                semester.getId(),
                request.subjectNumber())
        ) {
            throw new MyException(MyErrorCode.DUPLICATE_COURSE_OFFERING);
        }

        // 개설 강의 생성
        CourseOffering offering = CourseOffering.create(
                request.syllabus(),
                request.subjectNumber(),
                request.method(),
                request.professor(),
                course,
                semester,
                request.targetDepartment(),
                request.language(),
                request.capacity(),
                request.enrolledCount(),
                request.note()
        );

        CourseOffering saved = courseOfferingRepository.save(offering);

        List<CourseMeeting> meetings = saveMeetings(saved, request.meetings());

        return CourseOfferingResponseDto.from(saved, meetings);
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

    /**
     * 해당 개설 강의에 딸려있는 시간 저장하는 메서드
     */
    private List<CourseMeeting> saveMeetings(
            CourseOffering courseOffering,
            List<CourseMeetingRequestDto> meetingRequests
    ) {
        if (meetingRequests == null || meetingRequests.isEmpty()) {
            return List.of();
        }

        List<CourseMeeting> meetings = meetingRequests.stream()
                .map(request -> CourseMeeting.create(
                        courseOffering,
                        request.location(),
                        request.sequence(),
                        request.day(),
                        request.startTime(),
                        request.endTime()
                ))
                .toList();

        return courseMeetingRepository.saveAll(meetings);
    }

    // 개설 강의 생성 dto로 내부 객체 생성
    private CourseCommand toCourseCommand(CourseOfferingCreateRequestDto request) {
        return new CourseCommand(
                request.courseId(),
                null,
                request.courseTitle(),
                request.department(),
                null,
                null,
                null
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
