package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.ExcelParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCommand;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem.CourseOverviewExcelRow;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingOptionsResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.CourseOfferingSort;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.GradeEvaluation;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseOfferingService {

    private static final List<String> ISU_ORDER = List.of(
            "공통필수", "공통선택", "교양필수", "기초교양", "기초과학", "교양선택", "핵심교양", "심화교양",
            "전공기초", "전공필수", "전공핵심", "전공선택", "전공심화", "전공선수", "선수", "교직선수",
            "교직", "부전공", "복수전공", "연계전공", "군사학", "일반선택", "논문", "융합전공"
    );
    private static final List<String> ISU_FIELD_ORDER = List.of(
            "기초교양", "학문의기초", "기초과학·공학", "(핵심)INU세미나", "(핵심)인문", "(핵심)사회",
            "(핵심)과학기술", "(핵심)예술체육", "(핵심)외국어", "인문", "사회", "과학기술", "예술체육", "외국어",
            "INU핵심리더십", "INU핵심창의융합", "INU핵심문제해결", "INU핵심의사소통", "INU핵심글로벌", "기초과학",
            "INU인성", "언어와문학", "과학과기술", "역사와문화", "인간과사회", "예술과스포츠", "전공기초",
            "전공필수", "전공선택", "전공핵심", "전공심화", "교직", "부전공", "복수전공", "연계전공", "군사학",
            "일반선택", "논문", "융합전공"
    );

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CourseMeetingService courseMeetingService;
    private final TimeTableItemRepository timeTableItemRepository;
    private final ExcelParser excelParser;

    public List<CourseOfferingResponseDto> getOpenCourseOfferings(
            String deptCode,
            String isuCode,
            String isuFldCode,
            String cnctrIsuCode,
            Boolean hussOnly,
            Boolean majorOnly,
            String keyword,
            boolean exposeProfessor
    ) {
        Semester semester = getOpenSemester();
        List<CourseOffering> offerings = courseOfferingRepository.findAllBySemesterId(semester.getId()).stream()
                .filter(item -> matches(deptCode, item.getDeptCode()))
                .filter(item -> matches(isuCode, item.getIsuCode()))
                .filter(item -> matches(isuFldCode, item.getIsuFldCode()))
                .filter(item -> matches(cnctrIsuCode, item.getCnctrIsuCode()))
                .filter(item -> !Boolean.TRUE.equals(hussOnly) || "Y".equalsIgnoreCase(item.getHussCourseYn()))
                .filter(item -> !Boolean.TRUE.equals(majorOnly) || (item.getIsuNameRaw() != null && item.getIsuNameRaw().contains("전공")))
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(openCourseComparator())
                .toList();

        Map<Long, List<CourseMeeting>> meetings = courseMeetingRepository
                .findAllByCourseOfferingIdIn(offerings.stream().map(CourseOffering::getId).toList()).stream()
                .collect(Collectors.groupingBy(item -> item.getCourseOffering().getId()));
        return offerings.stream()
                .map(item -> CourseOfferingResponseDto.from(
                        item,
                        courseMeetingService.mergeContinuousMeetings(meetings.getOrDefault(item.getId(), List.of())),
                        exposeProfessor
                ))
                .toList();
    }

    public CourseOfferingOptionsResponseDto getOpenCourseOfferingOptions() {
        Semester semester = getOpenSemester();
        List<CourseOffering> offerings = courseOfferingRepository.findAllBySemesterId(semester.getId());

        List<CourseOfferingOptionsResponseDto.CodeNameOption> departments = distinctOptions(
                offerings, CourseOffering::getDeptCode, CourseOffering::getDeptNameRaw,
                Comparator.comparing(CourseOfferingOptionsResponseDto.CodeNameOption::name)
                        .thenComparing(CourseOfferingOptionsResponseDto.CodeNameOption::code));
        List<CourseOfferingOptionsResponseDto.CodeNameOption> connectedMajors = distinctOptions(
                offerings, CourseOffering::getCnctrIsuCode, CourseOffering::getCnctrIsuNameRaw,
                Comparator.comparing(CourseOfferingOptionsResponseDto.CodeNameOption::name)
                        .thenComparing(CourseOfferingOptionsResponseDto.CodeNameOption::code));

        Map<String, CourseOfferingOptionsResponseDto.CodeNameOption> categories = new LinkedHashMap<>();
        Map<String, Map<String, CourseOfferingOptionsResponseDto.CodeNameOption>> fields = new HashMap<>();
        for (CourseOffering item : offerings) {
            if (blank(item.getIsuCode()) || blank(item.getIsuNameRaw())) continue;
            categories.putIfAbsent(item.getIsuCode(), new CourseOfferingOptionsResponseDto.CodeNameOption(item.getIsuCode(), item.getIsuNameRaw()));
            if (!blank(item.getIsuFldCode()) && !blank(item.getIsuFldNameRaw())) {
                fields.computeIfAbsent(item.getIsuCode(), ignored -> new LinkedHashMap<>())
                        .putIfAbsent(item.getIsuFldCode(), new CourseOfferingOptionsResponseDto.CodeNameOption(item.getIsuFldCode(), normalizeFieldName(item.getIsuFldNameRaw())));
            }
        }
        Comparator<CourseOfferingOptionsResponseDto.CodeNameOption> categoryComparator = optionComparator(ISU_ORDER);
        Comparator<CourseOfferingOptionsResponseDto.CodeNameOption> fieldComparator = optionComparator(ISU_FIELD_ORDER);
        List<CourseOfferingOptionsResponseDto.CompletionOption> completionCategories = categories.values().stream()
                .sorted(categoryComparator)
                .map(category -> new CourseOfferingOptionsResponseDto.CompletionOption(
                        category.code(), category.name(),
                        fields.getOrDefault(category.code(), Map.of()).values().stream().sorted(fieldComparator).toList()))
                .toList();

        return new CourseOfferingOptionsResponseDto(
                new CourseOfferingOptionsResponseDto.SemesterOption(semester.getId(), semester.getYear(), semester.getTerm(), semester.getTerm().getDisplayName()),
                departments,
                completionCategories,
                connectedMajors
        );
    }

    private Semester getOpenSemester() {
        return semesterRepository.findFirstByStatusOrderByStartDateDesc(kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus.OPEN)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));
    }

    private boolean matches(String expected, String actual) {
        return blank(expected) || Objects.equals(expected, actual);
    }

    private boolean matchesKeyword(CourseOffering offering, String keyword) {
        if (blank(keyword)) return true;
        String trimmed = keyword.trim();
        return contains(offering.getSubjectNumber(), trimmed)
                || contains(offering.getCourse().getCourseCode(), trimmed)
                || contains(offering.getCourse().getTitle(), trimmed)
                || contains(offering.getCourse().getEnglishTitle(), trimmed);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private Comparator<CourseOffering> openCourseComparator() {
        return Comparator.comparingInt((CourseOffering item) -> gradeOrder(item.getHyNameRaw()))
                .thenComparing(item -> nullSafe(item.getIsuCode()))
                .thenComparing(item -> nullSafe(item.getSubjectNumber()))
                .thenComparing(CourseOffering::getId);
    }

    private int gradeOrder(String grade) {
        if ("전학년".equals(grade)) return 0;
        if ("1".equals(grade)) return 1;
        if ("2".equals(grade)) return 2;
        if ("3".equals(grade)) return 3;
        if ("4".equals(grade)) return 4;
        return 99;
    }

    private List<CourseOfferingOptionsResponseDto.CodeNameOption> distinctOptions(
            List<CourseOffering> offerings,
            Function<CourseOffering, String> code,
            Function<CourseOffering, String> name,
            Comparator<CourseOfferingOptionsResponseDto.CodeNameOption> comparator
    ) {
        Map<String, CourseOfferingOptionsResponseDto.CodeNameOption> result = new LinkedHashMap<>();
        offerings.forEach(item -> {
            String itemCode = code.apply(item);
            String itemName = name.apply(item);
            if (!blank(itemCode) && !blank(itemName))
                result.putIfAbsent(itemCode, new CourseOfferingOptionsResponseDto.CodeNameOption(itemCode, itemName));
        });
        return result.values().stream().sorted(comparator).toList();
    }

    private Comparator<CourseOfferingOptionsResponseDto.CodeNameOption> optionComparator(List<String> order) {
        return Comparator.comparingInt((CourseOfferingOptionsResponseDto.CodeNameOption option) -> {
                    int index = order.indexOf(normalizeFieldName(option.name()));
                    return index < 0 ? Integer.MAX_VALUE : index;
                })
                .thenComparing(CourseOfferingOptionsResponseDto.CodeNameOption::code);
    }

    private String normalizeFieldName(String name) {
        return name == null ? null : name.replace('ㆍ', '·');
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

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
                toCleanList(hyName),
                toCleanList(isuName),
                toCleanList(isuFldName),
                toCleanList(ssupTypeName),
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

    // DeptName 필터 (프론트에서 학과명/코드 문자열로 넘어옴)
    private Department toDeptName(String value) {
        if (isBlank(value)) {
            return null;
        }

        Department department = Department.fromApi(null, value);
        if (department == Department.UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return department;
    }

    // CollegeName 필터 (프론트에서 단과대명/코드 문자열로 넘어옴)
    private College toCollegeName(String value) {
        if (isBlank(value)) {
            return null;
        }

        College college = College.fromApi(null, value);
        if (college == College.UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return college;
    }

    // HyNames, IsuNames, IsuFldNames, SSUPTypeNames 필터
    // 리스트로 들어온 값을 분해하고, 각각의 값을 하나하나 정제(
    private List<String> toCleanList(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
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
     * (학교 API에서 받은 개설강의 기본값을 먼저 DB에 생성/앱데이트 하는 메서드)
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
                            request.deptCode(),
                            request.deptName(),
                            request.collegeCode(),
                            request.collegeName(),
                            request.hyCode(),
                            request.hyName(),
                            request.isuCode(),
                            request.isuName(),
                            request.isuFldCode(),
                            request.isuFldName(),
                            request.suupTypeCode(),
                            request.suupTypeName(),
                            request.cnctrIsuCode(),
                            request.cnctrIsuName(),
                            request.englishYn(),
                            request.englishCode(),
                            request.englishName(),
                            request.hussCourseYn(),
                            request.credit()
                    );
                    return existing;
                })
                .orElseGet(() -> courseOfferingRepository.save(
                        CourseOffering.create(
                                null,
                                request.haksuCode(),
                                request.deptCode(),
                                request.deptName(),
                                request.collegeCode(),
                                request.collegeName(),
                                request.hyCode(),
                                request.hyName(),
                                request.isuCode(),
                                request.isuName(),
                                request.isuFldCode(),
                                request.isuFldName(),
                                request.suupTypeCode(),
                                request.suupTypeName(),
                                request.cnctrIsuCode(),
                                request.cnctrIsuName(),
                                request.englishYn(),
                                request.englishCode(),
                                request.englishName(),
                                request.hussCourseYn(),
                                null,
                                null,
                                course,
                                semester,
                                null,
                                request.credit(),
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
                Department.fromApi(item.deptCode(), item.deptName()),
                TargetGrade.from(item.hyName()),
                CompletionDivision.from(item.isuName()),
                item.credit()
        );
    }


    /**
     * Excel 편람 파일에서 필드 업데이트
     */
    @Transactional
    public void updateFieldFromExcel(
            Integer year,
            SemesterTerm term,
            InputStream inputStream
    ) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        List<CourseOverviewExcelRow> rows = excelParser.parse(inputStream);

        Map<String, CourseOverviewExcelRow> rowBySubjectNumber = rows.stream()
                .filter(row -> row.subjectNumber() != null && !row.subjectNumber().isBlank())
                .collect(Collectors.toMap(
                        row -> row.subjectNumber().trim(),
                        row -> row,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        int updatedCount = 0;
        int skippedCount = rows.size() - rowBySubjectNumber.size();

        for (Map.Entry<String, CourseOverviewExcelRow> entry : rowBySubjectNumber.entrySet()) {

            String subjectNumber = entry.getKey();
            CourseOverviewExcelRow row = entry.getValue();

            Optional<CourseOffering> courseOffering =
                    courseOfferingRepository.findBySemesterIdAndSubjectNumber(semester.getId(), subjectNumber);

            if (courseOffering.isPresent()) {
                courseOffering.get().updateFromExcel(
                        row.professor(),
                        row.capacity(),
                        row.gradeEvaluation(),
                        parseGradeEvaluation(row.gradeEvaluation())
                );
                updatedCount++;
            } else {
                skippedCount++;
                log.warn("엑셀 업데이트 대상 강의를 찾을 수 없습니다. year={}, term={}, subjectNumber={}",
                        year, term, subjectNumber);
            }
        }

        log.info("편람 엑셀 반영 완료. year={}, term={}, total={}, updated={}, skipped={}",
                year,
                term,
                rows.size(),
                updatedCount,
                skippedCount
        );
    }

    private GradeEvaluation parseGradeEvaluation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return GradeEvaluation.from(value.trim());
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
