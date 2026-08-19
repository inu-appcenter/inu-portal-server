package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseOfferingServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CourseMeetingRepository courseMeetingRepository;

    @Mock
    private CourseMeetingService courseMeetingService;

    @Mock
    private kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository timeTableItemRepository;

    @Mock
    private kr.inuappcenterportal.inuportal.domain.course.crawler.excel.ExcelParser excelParser;

    private CourseOfferingService service;

    @BeforeEach
    void setup() {
        service = new CourseOfferingService(
            courseRepository,
            courseOfferingRepository,
            semesterRepository,
            courseMeetingRepository,
            courseMeetingService,
            timeTableItemRepository,
            excelParser
        );
    }

    @Test
    void getCourseOfferingsByCourseCodes_emptyInput_returnsEmpty() {
        List<?> result = service.getCourseOfferingsByCourseCodes(null, false);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = service.getCourseOfferingsByCourseCodes(List.of(), false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCourseOfferingsByCourseCodes_returnsMappedDto() {
        // prepare course and semester
        Course course = Course.createFromApi("C001", "Title", "EngTitle", Department.COMPUTER_ENGINEERING, College.COLLEGE_OF_INFORMATION_TECHNOLOGY, null, null, 3);
        Semester semester = Semester.create(2026, SemesterTerm.FIRST, kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus.OPEN, LocalDate.now(), LocalDate.now().plusMonths(4));

        CourseOffering offering = CourseOffering.create(
                null,
                "SUB-001",
                "DEPT",
                "DeptRaw",
                "COL",
                "ColRaw",
                "HY",
                "HyRaw",
                "ISU",
                "IsuRaw",
                "ISUFLD",
                "IsuFldRaw",
                "SSUP",
                "SsupRaw",
                "CNCTR",
                "CnctrRaw",
                "N",
                "ENG",
                "EngRaw",
                "Y",
                null,
                "Prof",
                course,
                semester,
                CNCTR_ISU_NAME.NORMAL,
                DEPT_NAME.COMPUTER_SCIENCE_ENGINEERING,
                COLLEGE_NAME.INFORMATION_TECHNOLOGY,
                ISU_FLD_NAME.BASIC_SCIENCE_ENGINEERING,
                ISU_NAME.MAJOR_CORE,
                SSUP_TYPE_NAME.UNKNOWN,
                HY_NAME.GRADE1,
                ENGLISH_NAME.UNKNOWN,
                null,
                3,
                null,
                null
        );

        when(courseOfferingRepository.findAllByCourseCourseCodeIn(List.of("C001"))).thenReturn(List.of(offering));
        when(courseMeetingRepository.findAllByCourseOfferingIdIn(ArgumentMatchers.anyList())).thenReturn(List.of());
        when(courseMeetingService.mergeContinuousMeetings(ArgumentMatchers.anyList())).thenReturn(List.of());

        List<kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto> result =
                service.getCourseOfferingsByCourseCodes(List.of("C001"), false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("C001", result.get(0).courseCode());
        // exposeProfessor=false면 교수명은 응답에 안 실린다.
        assertNull(result.get(0).professor());
    }
}
