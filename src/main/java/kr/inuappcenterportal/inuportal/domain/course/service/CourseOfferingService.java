package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.ExcelParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCommand;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CourseExcelRow;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
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

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseOfferingService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CourseMeetingService courseMeetingService;
    private final TimeTableItemRepository timeTableItemRepository;
    private final ExcelParser excelParser;

    /**
     * 개설 강의 조건별 조회
     */
    public Page<CourseOfferingResponseDto> getCourseOfferings(
            Integer year,
            SemesterTerm term,
            String deptName,
            String collegeName,
            List<String> hyName,
            List<String> isuName,
            List<String> isuFldName,
            List<String> ssupTypeName,
            List<Integer> credit,
            String keyword,
            MeetingFilterMode filterMode,
            List<String> meetings,
            CourseOfferingSort sort,
            Pageable pageable,
            boolean exposeProfessor
    ) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        CourseOfferingSearchCondition condition = new CourseOfferingSearchCondition(
                semester.getId(),
                toDeptName(deptName),
                toCollegeName(collegeName),
                toHyNames(hyName),
                toIsuNames(isuName),
                toIsuFldNames(isuFldName),
                toSsupTypeNames(ssupTypeName),
                toCredits(credit),
                keyword,
                resolveMeetingFilterMode(filterMode, meetings),
                toMeetingFilters(meetings),
                sort == null ? CourseOfferingSort.DEFAULT : sort
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

        Map<Long, Long> savedCountMap = courseOfferingIds.isEmpty() ? Map.of() :
                timeTableItemRepository.countDistinctMemberByCourseOfferingIdIn(courseOfferingIds).stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        return courseOfferings.map(courseOffering -> CourseOfferingResponseDto.from(
                courseOffering,
                courseMeetingService.mergeContinuousMeetings
                        (meetingsByCourseOfferingId.getOrDefault(courseOffering.getId(), List.of())),
                exposeProfessor,
                savedCountMap.getOrDefault(courseOffering.getId(), 0L)
        ));
    }

    // DeptName 필터
    private DEPT_NAME toDeptName(String value) {
        if (isBlank(value)) {
            return null;
        }

        DEPT_NAME deptName = DEPT_NAME.from(value);
        if (deptName == DEPT_NAME.UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return deptName;
    }

    // CollegeName 필터
    private COLLEGE_NAME toCollegeName(String value) {
        if (isBlank(value)) {
            return null;
        }

        COLLEGE_NAME collegeName = COLLEGE_NAME.from(value);
        if (collegeName == COLLEGE_NAME.UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return collegeName;
    }

    // HyNames 필터
    // 리스트로 들어온 값을 분해하고, 각각의 HyName을 하나하나 값 검증
    private List<HY_NAME> toHyNames(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> {
                    HY_NAME hyName = HY_NAME.from(value);
                    if (hyName == HY_NAME.UNKNOWN) {
                        throw new MyException(MyErrorCode.INVALID_INPUT);
                    }
                    return hyName;
                })
                .toList();
    }

    // IsuNames 필터
    // 리스트로 들어온 값을 분해하고, 각각의 IsuName을 하나하나 값 검증
    private List<ISU_NAME> toIsuNames(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> {
                    ISU_NAME isuName = ISU_NAME.from(value);
                    if (isuName == ISU_NAME.UNKNOWN) {
                        throw new MyException(MyErrorCode.INVALID_INPUT);
                    }

                    return isuName;
                })
                .toList();
    }

    // IsuFldNames 필터
    // 리스트로 들어온 값을 분해하고, 각각의 IsuFldName을 하나하나 값 검증
    private List<ISU_FLD_NAME> toIsuFldNames(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> {
                    ISU_FLD_NAME isuFldName = ISU_FLD_NAME.from(value);
                    if (isuFldName == ISU_FLD_NAME.UNKNOWN) {
                        throw new MyException(MyErrorCode.INVALID_INPUT);
                    }

                    return isuFldName;
                })
                .toList();
    }

    // SSUPTypeNames 필터
    // 리스트로 들어온 값을 분해하고, 각각의 SSUPTypeName을 하나하나 검증
    private List<SSUP_TYPE_NAME> toSsupTypeNames(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> {
                    SSUP_TYPE_NAME ssupTypeName = SSUP_TYPE_NAME.from(value);
                    if (ssupTypeName == SSUP_TYPE_NAME.UNKNOWN) {
                        throw new MyException(MyErrorCode.INVALID_INPUT);
                    }

                    return ssupTypeName;
                })
                .toList();
    }

    // 학점 필터
    private List<Integer> toCredits(List<Integer> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values;
    }

    // 시간 필터 모드
    private MeetingFilterMode resolveMeetingFilterMode(
            MeetingFilterMode meetingFilterMode,
            List<String> meetings
    ) {
        // meetings 파라미터가 null일때
        if (meetings == null || meetings.isEmpty()) {
            return null;
        }

        // 기본값은 HAS_CLASS 모드
        return meetingFilterMode == null ? MeetingFilterMode.HAS_CLASS : meetingFilterMode;
    }


    // toMeetingsFilters에서 분해된 시간 객체를 분해하는 메서드
    private CourseOfferingMeetingFilter toMeetingTimeFilter(String value) {
        String[] parts = value.contains("|") ? value.split("\\|") : value.split(",");

        if (parts.length != 3) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        DayOfWeek day = toDayOfWeek(parts[0].trim());
        LocalTime startTime;
        LocalTime endTime;

        try {
            startTime = LocalTime.parse(parts[1].trim());
            endTime = LocalTime.parse(parts[2].trim());
        } catch (DateTimeParseException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        if (!startTime.isBefore(endTime)) {
            throw new MyException(MyErrorCode.FASTER_THAN_ENDTIME);
        }

        return new CourseOfferingMeetingFilter(day, startTime, endTime);
    }

    // 요청으로 들어온 시간 리스트를 분해하는 메서드
    private List<CourseOfferingMeetingFilter> toMeetingFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> filteredValues = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();

        if (filteredValues.isEmpty()) {
            return List.of();
        }

        if (filteredValues.stream().noneMatch(value -> value.contains("|") || value.contains(","))) {
            if (filteredValues.size() % 3 != 0) {
                throw new MyException(MyErrorCode.INVALID_INPUT);
            }

            List<CourseOfferingMeetingFilter> meetingFilters = new ArrayList<>();
            for (int i = 0; i < filteredValues.size(); i += 3) {
                meetingFilters.add(toMeetingTimeFilter(String.join("|", filteredValues.subList(i, i + 3))));
            }
            return meetingFilters;
        }

        return filteredValues.stream().map(this::toMeetingTimeFilter).toList();
    }


    // 요일 필터
    private DayOfWeek toDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new MyException(MyErrorCode.INVALID_DAY_OF_WEEK);
        }
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
                        command.englishTitle(),
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
                    command.englishTitle(),
                    command.targetGrade(),
                    command.completionDivision(),
                    command.credit()
            );
            return course;
        }

        // 그래도 없으면 Course를 새로 생성
        Course course = Course.createFromApi(
                command.courseCode(),
                command.title(),
                command.englishTitle(),
                command.department(),
                command.department().getCollegeName(),
                command.targetGrade(),
                command.completionDivision(),
                command.credit()
        );

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
     * Excel 편람 파일에서 가져온 교수명 업데이트
     */
    @Transactional
    public void updateProfessorsFromExcel(
            Integer year,
            SemesterTerm term,
            InputStream inputStream
    ) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        List<CourseExcelRow> rows = excelParser.parse(inputStream);
        Map<String, String> professorBySubjectNumber = rows.stream()
                .filter(row -> row.subjectNumber() != null && !row.subjectNumber().isBlank())
                .filter(row -> row.professor() != null && !row.professor().isBlank())
                .collect(Collectors.toMap(
                        row -> row.subjectNumber().trim(),
                        row -> row.professor().trim(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        int updatedCount = 0;
        int skippedCount = rows.size() - professorBySubjectNumber.size();

        for (Map.Entry<String, String> entry : professorBySubjectNumber.entrySet()) {
            Optional<CourseOffering> courseOffering =
                    courseOfferingRepository.findBySemesterIdAndSubjectNumber(semester.getId(), entry.getKey());

            if (courseOffering.isPresent()) {
                courseOffering.get().updateProfessor(entry.getValue());
                updatedCount++;
            } else {
                skippedCount++;
                log.warn("교수명 업데이트 대상 강의를 찾을 수 없습니다. year={}, term={}, subjectNumber={}",
                        year, term, entry.getKey());
            }
        }

        log.info("교수명 엑셀 반영 완료. year={}, term={}, total={}, updated={}, skipped={}",
                year,
                term,
                rows.size(),
                updatedCount,
                skippedCount
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
