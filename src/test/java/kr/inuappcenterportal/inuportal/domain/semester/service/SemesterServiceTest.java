package kr.inuappcenterportal.inuportal.domain.semester.service;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

    @InjectMocks
    private SemesterService semesterService;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("학사일정에서 학기 시작 후보를 찾아 학기를 생성한다.")
    void syncSemestersByYearCreatesSemestersFromSchedules() {
        int currentYear = LocalDate.now().getYear();
        List<Schedule> schedules = List.of(
                createSchedule(currentYear, 3, 2, "1학기 개강"),
                createSchedule(currentYear, 6, 22, "여름 계절학기"),
                createSchedule(currentYear, 9, 1, "2학기 개강"),
                createSchedule(currentYear, 12, 22, "겨울 계절학기")
        );

        when(scheduleRepository.findAcademicSemesterSchedules(
                any(LocalDate.class),
                any(LocalDate.class),
                eq("개강"),
                eq("계절학기")
        )).thenReturn(schedules);
        when(semesterRepository.findByYearAndTerm(any(Integer.class), any(SemesterTerm.class)))
                .thenReturn(Optional.empty());

        semesterService.syncSemestersByYear();

        ArgumentCaptor<Semester> semesterCaptor = ArgumentCaptor.forClass(Semester.class);
        verify(semesterRepository, times(4)).save(semesterCaptor.capture());

        List<Semester> savedSemesters = semesterCaptor.getAllValues();
        assertThat(savedSemesters)
                .extracting(Semester::getTerm)
                .containsExactly(
                        SemesterTerm.FIRST,
                        SemesterTerm.SECOND,
                        SemesterTerm.SUMMER,
                        SemesterTerm.WINTER
                );
    }

    private Schedule createSchedule(int year, int month, int day, String content) {
        LocalDate date = LocalDate.of(year, month, day);
        return Schedule.builder()
                .startDate(date)
                .endDate(date)
                .content(content)
                .aiGenerated(false)
                .build();
    }
}
