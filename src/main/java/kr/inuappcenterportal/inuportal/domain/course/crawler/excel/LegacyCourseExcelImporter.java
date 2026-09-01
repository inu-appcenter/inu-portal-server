package kr.inuappcenterportal.inuportal.domain.course.crawler.excel;

import kr.inuappcenterportal.inuportal.domain.course.dto.LegacyCourseRow;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseMeeting.TimeMapper;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 과거 과목 데이터 삽입을 위한 일회성 액샐 파싱 코드
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyCourseExcelImporter {
    private static final Pattern BLOCK_PATTERN =
            Pattern.compile("\\[([^:\\]]+)\\s*:\\s*([^\\]]+)]");

    private static final Pattern DAY_PATTERN =
            Pattern.compile("([월화수목금토일])((?:\\([^()]+\\))+)");

    private static final Pattern PERIOD_PATTERN =
            Pattern.compile("\\(([^()]+)\\)");

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final SemesterRepository semesterRepository;
    private final PlatformTransactionManager transactionManager;

    public void importArchive(List<MultipartFile> files) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int totalRows = 0;
        int successCount = 0;
        int skippedCount = 0;

        for (MultipartFile file : files) {
            // 엑셀 파싱 후 row에 저장
            List<LegacyCourseRow> rows = parseExcel(file);
            totalRows += rows.size();

            for (LegacyCourseRow row : rows) {
                try {
                    transactionTemplate.executeWithoutResult(status -> importRow(row));
                    successCount++;
                } catch (Exception e) {
                    skippedCount++;
                    log.warn(
                            "과거 강의 적재 스킵. file={}, year={}, term={}, subjectNumber={}, title={}, reason={}",
                            file.getOriginalFilename(),
                            row.year(),
                            row.term(),
                            row.subjectNumber(),
                            row.title(),
                            e.getMessage()
                    );
                }
            }
        }
        log.info(
                "과거 강의 Archive 적재 완료. files={}, totalRows={}, success={}, skipped={}",
                files.size(),
                totalRows,
                successCount,
                skippedCount
        );
    }

    private void importRow(LegacyCourseRow row) {
        Semester semester = findOrCreateClosedSemester(row.year(), row.term());
        Course course = findOrCreateCourse(row);
        CourseOffering courseOffering = upsertCourseOffering(row, course, semester);
        replaceCourseMeetings(courseOffering, row.timetable());
    }

    private List<LegacyCourseRow> parseExcel(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return List.of();
            }

            Map<String, Integer> headerIndex = readHeaderIndex(headerRow);
            List<LegacyCourseRow> rows = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String subjectNumber = getString(row, headerIndex, "학수번호");
                String title = getString(row, headerIndex, "교과목명");

                if (isBlank(subjectNumber) || isBlank(title)) {
                    continue;
                }

                rows.add(new LegacyCourseRow(
                        getInteger(row, headerIndex, "년도"),
                        parseSemesterTerm(getString(row, headerIndex, "학기")),
                        getString(row, headerIndex, "소속분류"),
                        getString(row, headerIndex, "학과(부)"),
                        getString(row, headerIndex, "이수구분"),
                        getString(row, headerIndex, "학년"),
                        subjectNumber.trim(),
                        title.trim(),
                        getString(row, headerIndex, "수업방법"),
                        getString(row, headerIndex, "담당교수"),
                        getString(row, headerIndex, "시간표"),
                        getInteger(row, headerIndex, "학점"),
                        getString(row, headerIndex, "이수영역"),
                        getString(row, headerIndex, "수업구분"),
                        getString(row, headerIndex, "수업유형"),
                        getString(row, headerIndex, "집중이수제구분"),
                        getString(row, headerIndex, "원어\n강의\n여부")
                ));
            }
            return rows;
        } catch (IOException e) {
            throw new MyException(MyErrorCode.INVALID_EXCEL_EXTENDER);
        }
    }

    /**
     * 파싱한 row에서 강의 생성 및 찾기
     */
    private Course findOrCreateCourse(LegacyCourseRow row) {
        // 과목 코드로 찾기
        Optional<Course> courseByCode = courseRepository.findByCourseCode(row.subjectNumber());
        if (courseByCode.isPresent()) {
            return courseByCode.get();
        }

        // 교과목, 학과로 찾기
        Department department = Department.fromApi(null, row.departmentRaw());
        Optional<Course> courseByTitleAndDepartment =
                courseRepository.findByTitleAndDepartment(row.title(), department);

        if (courseByTitleAndDepartment.isPresent()) {
            return courseByTitleAndDepartment.get();
        }

        // 둘 다 못찾으면 생성
        Course course = Course.createFromApi(
                row.subjectNumber(),
                row.title(),
                null,
                department,
                resolveCourseCollege(row, department),
                TargetGrade.from(row.gradeRaw()),
                CompletionDivision.from(row.completionDivisionRaw()),
                row.credit()
        );

        // 새로 추가된 과목은 개설되지 않은 가능성이 큼(과목 데이터는 이미 DB애 들어가 있기 때문)
        course.deactivate();

        return courseRepository.save(course);
    }

    private CourseOffering upsertCourseOffering(
            LegacyCourseRow row,
            Course course,
            Semester semester
    ) {
        Optional<CourseOffering> existing =
                courseOfferingRepository.findBySemesterIdAndSubjectNumber(
                        semester.getId(),
                        row.subjectNumber()
                );

        if (existing.isPresent()) {
            CourseOffering courseOffering = existing.get();

            courseOffering.updateFromApi(
                    course,
                    null,
                    row.departmentRaw(),
                    null,
                    row.collegeRaw(),
                    null,
                    row.gradeRaw(),
                    null,
                    row.completionDivisionRaw(),
                    null,
                    row.isuFldRaw(),
                    null,
                    row.ssupTypeRaw(),
                    null,
                    row.cnctrIsuRaw(),
                    row.englishYn(),
                    null,
                    null,
                    null,
                    safeCredit(row.credit())
            );

            courseOffering.updateFromExcel(
                    row.professor(),
                    null,
                    null,
                    null
            );

            return courseOffering;
        }
        return courseOfferingRepository.save(
                CourseOffering.create(
                        null,
                        row.subjectNumber(),
                        null,
                        row.departmentRaw(),
                        null,
                        row.collegeRaw(),
                        null,
                        row.gradeRaw(),
                        null,
                        row.completionDivisionRaw(),
                        null,
                        row.isuFldRaw(),
                        null,
                        row.ssupTypeRaw(),
                        null,
                        row.cnctrIsuRaw(),
                        row.englishYn(),
                        null,
                        null,
                        null,
                        null,
                        row.professor(),
                        course,
                        semester,
                        null,
                        safeCredit(row.credit()),
                        null,
                        null
                )
        );
    }

    private void replaceCourseMeetings(CourseOffering courseOffering, String timetable) {
        courseMeetingRepository.deleteAllByCourseOfferingId(courseOffering.getId());

        List<MeetingCommand> commands = parseTimetable(timetable);

        if (commands.isEmpty()) {
            return;
        }

        List<CourseMeeting> meetings = commands.stream()
                .map(command -> CourseMeeting.create(
                        courseOffering,
                        command.location(),
                        command.sequence(),
                        null,
                        command.day(),
                        command.startTime(),
                        command.endTime()
                ))
                .toList();

        courseMeetingRepository.saveAll(meetings);
    }

    private List<MeetingCommand> parseTimetable(String timetable) {
        if (isBlank(timetable) || timetable.trim().equals("-")) {
            return List.of();
        }

        List<MeetingCommand> result = new ArrayList<>();
        Matcher blockMatcher = BLOCK_PATTERN.matcher(timetable);

        while (blockMatcher.find()) {
            String location = blockMatcher.group(1).trim();
            String body = blockMatcher.group(2).trim();

            parseTimetableBlock(location, body, result);
        }

        return result;
    }

    private void parseTimetableBlock(
            String location,
            String body,
            List<MeetingCommand> result
    ) {
        Matcher dayMatcher = DAY_PATTERN.matcher(body);

        while (dayMatcher.find()) {
            DayOfWeek day = DayOfWeek.mapDay(dayMatcher.group(1));
            String periods = dayMatcher.group(2);

            Matcher periodMatcher = PERIOD_PATTERN.matcher(periods);

            while (periodMatcher.find()) {
                String sequence = TimeMapper.normalize(periodMatcher.group(1));
                TimeMapper timeMapper = TimeMapper.from(sequence);

                result.add(new MeetingCommand(
                        location,
                        sequence,
                        day,
                        timeMapper.startTime(),
                        timeMapper.endTime()
                ));
            }
        }
    }

    private Semester findOrCreateClosedSemester(Integer year, SemesterTerm term) {
        if (year == null || term == null) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return semesterRepository.findByYearAndTerm(year, term)
                .orElseGet(() -> semesterRepository.save(
                        Semester.create(
                                year,
                                term,
                                SemesterStatus.CLOSED,
                                defaultStartDate(year, term),
                                defaultEndDate(year, term)
                        )
                ));
    }

    private SemesterTerm parseSemesterTerm(String value) {
        if (isBlank(value)) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return switch (value.trim()) {
            case "1학기" -> SemesterTerm.FIRST;
            case "2학기" -> SemesterTerm.SECOND;
            case "여름계절학기" -> SemesterTerm.SUMMER;
            case "겨울계절학기" -> SemesterTerm.WINTER;
            default -> throw new MyException(MyErrorCode.INVALID_INPUT);
        };
    }

    private College resolveCourseCollege(LegacyCourseRow row, Department department) {
        if (department != null && department.getCollegeName() != null) {
            return department.getCollegeName();
        }

        return College.fromApi(null, row.collegeRaw());
    }

    private Integer safeCredit(Integer credit) {
        return credit == null ? 0 : credit;
    }

    private LocalDate defaultStartDate(Integer year, SemesterTerm term) {
        return switch (term) {
            case FIRST -> LocalDate.of(year, 3, 1);
            case SUMMER -> LocalDate.of(year, 6, 22);
            case SECOND -> LocalDate.of(year, 9, 1);
            case WINTER -> LocalDate.of(year, 12, 22);
        };
    }

    private LocalDate defaultEndDate(Integer year, SemesterTerm term) {
        return switch (term) {
            case FIRST -> LocalDate.of(year, 6, 21);
            case SUMMER -> LocalDate.of(year, 8, 31);
            case SECOND -> LocalDate.of(year, 12, 21);
            case WINTER -> LocalDate.of(year + 1, 2, 28);
        };
    }

    private Map<String, Integer> readHeaderIndex(Row headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();

        for (Cell cell : headerRow) {
            String headerName = readCellAsString(cell);

            if (!isBlank(headerName)) {
                headerIndex.put(normalizeHeader(headerName), cell.getColumnIndex());
            }
        }

        validateRequiredHeaders(headerIndex);
        return headerIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of(
                "년도",
                "학기",
                "학과(부)",
                "이수구분",
                "학년",
                "학수번호",
                "교과목명",
                "담당교수",
                "시간표",
                "학점"
        );

        for (String requiredHeader : requiredHeaders) {
            if (!headerIndex.containsKey(normalizeHeader(requiredHeader))) {
                throw new MyException(MyErrorCode.INVALID_INPUT);
            }
        }
    }

    private String getString(Row row, Map<String, Integer> headerIndex, String headerName) {
        Integer columnIndex = headerIndex.get(normalizeHeader(headerName));

        if (columnIndex == null) {
            return null;
        }

        return readCellAsString(row.getCell(columnIndex));
    }

    private Integer getInteger(Row row, Map<String, Integer> headerIndex, String headerName) {
        String value = getString(row, headerIndex, headerName);

        if (isBlank(value)) {
            return null;
        }

        try {
            String normalized = value
                    .replace(",", "")
                    .replace(".0", "")
                    .trim();

            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter(Locale.KOREA);
        String value = formatter.formatCellValue(cell);

        return value == null ? null : value.trim();
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            String value = readCellAsString(cell);

            if (!isBlank(value)) {
                return false;
            }
        }

        return true;
    }

    private String normalizeHeader(String value) {
        return value.replaceAll("\\s+", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MeetingCommand(
            String location,
            String sequence,
            DayOfWeek day,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
    }
}
