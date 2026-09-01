package kr.inuappcenterportal.inuportal.semester;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.semester.service.SemesterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SemesterServiceTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate TEST_TODAY = LocalDate.of(2026, 7, 1);
    private static final int TEST_YEAR = TEST_TODAY.getYear();

    private SemesterService semesterService;

    // 테스트에서 사용할 가짜 DB
    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @BeforeEach
        // 각 테스트 실행 전에 매번 실행되는 코드
    void setUp() {

        // 테스트용 고정 시간을 만든다
        Clock fixedClock = Clock.fixed(
                TEST_TODAY.atStartOfDay(TEST_ZONE).toInstant(),
                TEST_ZONE
        );

        // 위에서 만든 고정 시간과 DB 주입해 semesterService 생성
        semesterService = new SemesterService(
                semesterRepository,
                scheduleRepository,
                fixedClock
        );
    }

    // 테스트용 학기 생성 메서드
    private Schedule createSchedule(LocalDate startDate, LocalDate endDate, String content) {
        return Schedule.builder()
                .startDate(startDate)
                .endDate(endDate)
                .content(content)
                .aiGenerated(false)
                .build();
    }

    private Schedule createSchedule(int startMonth, int startDay, int endMonth, int endDay, String content) {
        return createSchedule(
                LocalDate.of(TEST_YEAR, startMonth, startDay),
                LocalDate.of(TEST_YEAR, endMonth, endDay),
                content
        );
    }

    @Test
    @DisplayName("학사일정에서 학기 시작 후보를 찾아 학기를 생성한다.")
    void 학기_저장_테스트() {

        // Given
        // 학사일정 크롤링한 데이터로 가정
        List<Schedule> schedules = List.of(
                createSchedule(3, 2, 3, 2, "1학기 개강"),
                createSchedule(6, 22, 7, 12, "하계 계절학기"),
                createSchedule(9, 1, 9, 1, "2학기 개강"),
                createSchedule(
                        LocalDate.of(TEST_YEAR, 12, 22),
                        LocalDate.of(TEST_YEAR + 1, 1, 13),
                        "동계 계절학기"
                )
        );

        // 레포지토리의 동작을 가짜로 지정
        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);

        when(semesterRepository.findByYearAndTerm(any(Integer.class), any(SemesterTerm.class)))
                .thenReturn(Optional.empty());

        // When
        // 실제 학기 서비스 로직 테스트 실행
        semesterService.syncSemestersByYear();

        // Then
        // semesterRepository.save(...)에 들어간 Semester 객체들을 잡아둘 도구
        ArgumentCaptor<Semester> semesterCaptor = ArgumentCaptor.forClass(Semester.class);

        // save가 4번 실행됬는지 테스트 및 테스트한 객체를 semesterCaptor에 저장
        verify(semesterRepository, times(4)).save(semesterCaptor.capture());

        // semesterCaptor에 저장된 객체를 꺼냄
        List<Semester> savedSemesters = semesterCaptor.getAllValues();
        assertThat(savedSemesters)
                .extracting(Semester::getTerm)
                .containsExactly(
                        SemesterTerm.FIRST,
                        SemesterTerm.SECOND,
                        SemesterTerm.SUMMER,
                        SemesterTerm.WINTER
                );

        // semesterCaptor에서 꺼낸 객체가 실제 들어간 데이터와 동일한지 테스트
        assertThat(savedSemesters.get(0).getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 3, 2));

        assertThat(savedSemesters.get(0).getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 6, 21));

        assertThat(savedSemesters.get(1).getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 9, 1));

        assertThat(savedSemesters.get(1).getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 12, 21));

        assertThat(savedSemesters.get(2).getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 6, 22));

        assertThat(savedSemesters.get(2).getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 7, 12));

        assertThat(savedSemesters.get(3).getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 12, 22));

        assertThat(savedSemesters.get(3).getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR + 1, 1, 13));
    }


    @Test
    @DisplayName("이미 같은 연도와 학기의 학기가 있으면 기간과 상태를 갱신한다.")
    void 중복_학기_검사_로직_테스트() {

        // Given
        // 크롤링된 학사일정에 1학기, 하계 계절학가 있다고 가정
        List<Schedule> schedules = List.of(
                createSchedule(3, 2, 3, 2, "1학기 개강"),
                createSchedule(6, 22, 7, 12, "하계 계절학기")
        );

        // 이미 DB에 1학기가 저장되어 있음을 가정
        Semester existingSemester = Semester.create(
                TEST_YEAR,
                SemesterTerm.FIRST,
                SemesterStatus.UPCOMING,
                LocalDate.of(TEST_YEAR, 3, 1),
                LocalDate.of(TEST_YEAR, 6, 20)
        );

        // 학사일정을 조회하면 위에서 만든 schedules을 반환
        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);

        // FIRST를 조회하는 요청이오면 위에서 existingSemester를 반환
        when(semesterRepository.findByYearAndTerm(TEST_YEAR, SemesterTerm.FIRST))
                .thenReturn(Optional.of(existingSemester));
        when(semesterRepository.findByYearAndTerm(TEST_YEAR, SemesterTerm.SUMMER))
                .thenReturn(Optional.empty());

        // When
        semesterService.syncSemestersByYear();

        // Then
        ArgumentCaptor<Semester> semesterCaptor = ArgumentCaptor.forClass(Semester.class);
        verify(semesterRepository, times(1)).save(semesterCaptor.capture());

        Semester savedSemester = semesterCaptor.getValue();

        assertThat(existingSemester.getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 3, 2));
        assertThat(existingSemester.getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 6, 21));
        assertThat(existingSemester.getStatus())
                .isEqualTo(SemesterStatus.CLOSED);

        assertThat(savedSemester.getTerm())
                .isEqualTo(SemesterTerm.SUMMER);
        assertThat(savedSemester.getStartDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 6, 22));
        assertThat(savedSemester.getEndDate())
                .isEqualTo(LocalDate.of(TEST_YEAR, 7, 12));
        assertThat(savedSemester.getStatus())
                .isEqualTo(SemesterStatus.OPEN);
    }


    @Test
    @DisplayName("종료일을 계산할 수 없으면 학기를 생성하지 않는다.")
    void 종료일_null_검사_테스트() {

        // Given
        List<Schedule> schedules = List.of(
                createSchedule(3, 2, 3, 2, "1학기 개강")
        );

        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);

        // When
        semesterService.syncSemestersByYear();

        // Then
        // 1학기의 종료일은 하계 계절학기의 -1일인데 하계 계절학기가 주어지지 않아서 생성되지 못하는 걸 체크
        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    @DisplayName("학기 목록은 모든 상태를 상태 우선순위와 최신 학기순으로 조회한다.")
    void 학기_목록_조회_정렬_테스트() {
        // Given
        Semester openFirst = createSemester(
                2026,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 21)
        );

        Semester openSummer = createSemester(
                2026,
                SemesterTerm.SUMMER,
                SemesterStatus.OPEN,
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 7, 12)
        );

        Semester closedSecond = createSemester(
                2025,
                SemesterTerm.SECOND,
                SemesterStatus.CLOSED,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 12, 21)
        );

        Semester closedWinter = createSemester(
                2025,
                SemesterTerm.WINTER,
                SemesterStatus.CLOSED,
                LocalDate.of(2025, 12, 22),
                LocalDate.of(2026, 1, 13)
        );

        Semester upcomingFirst = createSemester(
                2027,
                SemesterTerm.FIRST,
                SemesterStatus.UPCOMING,
                LocalDate.of(2027, 3, 2),
                LocalDate.of(2027, 6, 21)
        );

        when(semesterRepository.findAllByYearGreaterThanEqual(2020))
                .thenReturn(List.of(closedSecond, upcomingFirst, openFirst, closedWinter, openSummer));

        // When
        List<SemesterResponseDto> response = semesterService.getSemesters();

        // Then
        assertThat(response)
                .extracting(SemesterResponseDto::term)
                .containsExactly(
                        SemesterTerm.SUMMER,
                        SemesterTerm.FIRST,
                        SemesterTerm.WINTER,
                        SemesterTerm.SECOND,
                        SemesterTerm.FIRST
                );

        assertThat(response)
                .extracting(SemesterResponseDto::status)
                .containsExactly(
                        SemesterStatus.OPEN,
                        SemesterStatus.OPEN,
                        SemesterStatus.CLOSED,
                        SemesterStatus.CLOSED,
                        SemesterStatus.UPCOMING
                );

        verify(semesterRepository).findAllByYearGreaterThanEqual(2020);
    }


    @Test
    @DisplayName("TEST_TODAY 기준으로 학기 상태를 업데이트한다.")
    void 학기_업데이트_로직_테스트() {
        // TEST_TODAY = 2026/07/01
        // UPCOMING: 시작일이 기준 날짜보다 5주 뒤라 아직 열리지 않음
        // OPEN: 시작일이 기준 날짜보다 1주 전이고 종료일이 남아 있음
        // CLOSED: 종료일이 기준 날짜보다 하루 전이라 이미 종료됨


        // Given
        // 시작일이 기준 날짜보다 5주 뒤이므로 UPCOMING
        Semester upcomingSemester = Semester.create(
                TEST_YEAR,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                TEST_TODAY.plusWeeks(5),
                TEST_TODAY.plusWeeks(20)
        );

        // 시작일이 기준 날짜보다 1주 전이고 종료일이 남아 있으므로 OPEN
        Semester openSemester = Semester.create(
                TEST_YEAR,
                SemesterTerm.SECOND,
                SemesterStatus.UPCOMING,
                TEST_TODAY.minusWeeks(1),
                TEST_TODAY.plusWeeks(10)
        );

        // 종료일이 기준 날짜보다 하루 전이므로 CLOSED
        Semester closedSemester = Semester.create(
                TEST_YEAR,
                SemesterTerm.SUMMER,
                SemesterStatus.OPEN,
                TEST_TODAY.minusWeeks(10),
                TEST_TODAY.minusDays(1)
        );

        // DB 조회하면 위에서 저장한 upcomingSemester, openSemester, closedSemester을 리스트로 반환
        when(semesterRepository.findAll())
                .thenReturn(List.of(upcomingSemester, openSemester, closedSemester));

        // When
        semesterService.updateSemesterStatus();

        // Then
        // 의도대로 학기 상태 확인
        assertThat(upcomingSemester.getStatus())
                .isEqualTo(SemesterStatus.UPCOMING);

        assertThat(openSemester.getStatus())
                .isEqualTo(SemesterStatus.OPEN);

        assertThat(closedSemester.getStatus())
                .isEqualTo(SemesterStatus.CLOSED);
    }

    private Semester createSemester(
            Integer year,
            SemesterTerm term,
            SemesterStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Semester.create(
                year,
                term,
                status,
                startDate,
                endDate
        );
    }
}
