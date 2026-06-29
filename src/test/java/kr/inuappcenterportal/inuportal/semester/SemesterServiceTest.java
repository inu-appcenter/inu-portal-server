package kr.inuappcenterportal.inuportal.semester;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.semester.service.SemesterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SemesterServiceTest {

    @InjectMocks
    private SemesterService semesterService;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("학사일정에서 학기 시작 후보를 찾아 학기를 생성한다.")
    void 데모_학사일정_크롤링() {

        /// Given
        int currentYear = LocalDate.now().getYear();

        // 학사일정 크롤링한 데이터로 가정
        List<Schedule> schedules = List.of(
                학기_생성(currentYear, 3, 2, "1학기 개강"),
                학기_생성(currentYear, 6, 22, "하계 계절학기"),
                학기_생성(currentYear, 9, 1, "2학기 개강"),
                학기_생성(currentYear, 12, 22, "동계 계절학기")
        );

        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);

        when(semesterRepository.findByYearAndTerm(any(Integer.class), any(SemesterTerm.class)))
                .thenReturn(Optional.empty());

        /// When
        semesterService.syncSemestersByYear();

        /// Then
        verify(semesterRepository, times(4)).save(any(Semester.class));
    }

    private Schedule 학기_생성(int year, int month, int day, String content) {
        LocalDate date = LocalDate.of(year, month, day);

        return Schedule.builder()
                .startDate(date)
                .endDate(date)
                .content(content)
                .aiGenerated(false)
                .build();
    }


    @Test
    @DisplayName("중복일정 검사 로직 테스트")
    void 중복_학기_검사_로직_테스트() {

        int currentYear = LocalDate.now().getYear();

        // 학사일정 크롤링한 데이터로 가정
        List<Schedule> schedules = List.of(
                학기_생성(currentYear, 3, 2, "1학기 개강")
        );

        Semester existingSemester = Semester.create(
                currentYear,
                SemesterTerm.FIRST,
                LocalDate.of(currentYear, 3, 2),
                null
        );

        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);

        when(semesterRepository.findByYearAndTerm(currentYear, SemesterTerm.FIRST))
                .thenReturn(Optional.of(existingSemester));

        // when
        semesterService.syncSemestersByYear();

        // then
        verify(semesterRepository, never()).save(any(Semester.class));
    }

}
