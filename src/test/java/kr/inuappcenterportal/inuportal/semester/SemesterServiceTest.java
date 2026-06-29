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
    void 학사일정_크롤링_학기_저장_테스트() {

        /// Given
        int currentYear = LocalDate.now().getYear();

        // 학사일정 크롤링한 데이터로 가정
        List<Schedule> schedules = List.of(
                학기_생성(currentYear, 3, 2, "1학기 개강"),
                학기_생성(currentYear, 6, 22, "하계 계절학기"),
                학기_생성(currentYear, 9, 1, "2학기 개강"),
                학기_생성(currentYear, 12, 22, "동계 계절학기")
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

        /// When

        // 실제 학기 서비스 로직 테스트 실행
        semesterService.syncSemestersByYear();

        /// Then

        // semesterRepository.save(...)에 들어간 Semester 객체들을 잡아둘 도구
        ArgumentCaptor<Semester> semesterCaptor = ArgumentCaptor.forClass(Semester.class);

        // save가 4번 실행됬는지 테스트 및 테스트한 객체를 semesterCaptor에 저장
        verify(semesterRepository, times(4)).save(semesterCaptor.capture());

        // 위에서 저장된 객체를 꺼냄
        List<Semester> savedSemesters = semesterCaptor.getAllValues();
        assertThat(savedSemesters)
                .extracting(Semester::getTerm)
                .containsExactly(
                        SemesterTerm.FIRST,
                        SemesterTerm.SECOND,
                        SemesterTerm.SUMMER,
                        SemesterTerm.WINTER
                );

        // 위에서 꺼낸 객체가 실제 들어간 데이터와 동일한지 테스트
        assertThat(savedSemesters.get(0).getStartDate())
                .isEqualTo(LocalDate.of(currentYear, 3, 2));

        assertThat(savedSemesters.get(0).getEndDate())
                .isEqualTo(LocalDate.of(currentYear, 6, 21));

        assertThat(savedSemesters.get(2).getStartDate())
                .isEqualTo(LocalDate.of(currentYear, 6, 22));

        assertThat(savedSemesters.get(2).getEndDate())
                .isEqualTo(LocalDate.of(currentYear, 7, 12));
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

        /// Given
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

        /// When
        semesterService.syncSemestersByYear();

        /// Then
        verify(semesterRepository, never()).save(any(Semester.class));
    }
}
