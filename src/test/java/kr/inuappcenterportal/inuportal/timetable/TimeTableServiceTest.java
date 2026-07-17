package kr.inuappcenterportal.inuportal.timetable;


import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimeTableServiceTest {
    @InjectMocks
    private TimeTableService timeTableService;

    @Mock
    private TimeTableRepository timeTableRepository;

    @Mock
    private TimeTableItemRepository timeTableItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Test
    @DisplayName("학기 첫 시간표를 생성하면 대표 시간표로 생성됩니다.")
    void 시간표_생성_테스트() {
        // given
        Long memberId = 1L;
        Long semesterId = 1L;

        Member member = Member.builder()
                .studentId("20241234")
                .roles(List.of("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        Semester semester = Semester.create(
                2026,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 21)
        );
        ReflectionTestUtils.setField(semester, "id", semesterId);

        TimeTableCreateRequestDto request = new TimeTableCreateRequestDto("2026-1학기");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
        when(timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableName(
                memberId, semesterId, request.timeTableName()
        )).thenReturn(false);
        when(timeTableRepository.existsByMemberIdAndSemesterId(memberId, semesterId
        )).thenReturn(false);

        when(timeTableRepository.save(any(TimeTable.class)))
                .thenAnswer(invocation -> {
                    TimeTable timeTable = invocation.getArgument(0);
                    ReflectionTestUtils.setField(timeTable, "id", 1L);
                    return timeTable;
                });

        // when
        TimeTableResponseDto response = timeTableService.createTimeTable(memberId, semesterId, request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.semesterId()).isEqualTo(semesterId);
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.term()).isEqualTo(SemesterTerm.FIRST);
        assertThat(response.timeTableName()).isEqualTo("2026-1학기");
        assertThat(response.isPrimary()).isTrue();
        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    @DisplayName("다른 사용자의 시간표는 수정할 수 없다")
    void 다른_사용자_시간표_수정_검증_테스트() {
        // given
        Long requestMemberId = 1L;
        Long ownerMemberId = 2L;
        Long timeTableId = 1L;

        Member owner = Member.builder()
                .studentId("20240002")
                .roles(List.of("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(owner, "id", ownerMemberId);

        Semester semester = Semester.create(
                2026,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 21)
        );
        ReflectionTestUtils.setField(semester, "id", 1L);

        TimeTable timeTable = TimeTable.create("기존 시간표", true, owner, semester);
        ReflectionTestUtils.setField(timeTable, "id", timeTableId);

        when(timeTableRepository.findById(timeTableId)).thenReturn(Optional.of(timeTable));

        TimeTableNameUpdateRequestDto request =
                new TimeTableNameUpdateRequestDto("수정할 이름");

        // when & then
        assertThatThrownBy(() ->
                timeTableService.setTimeTableName(requestMemberId, timeTableId, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 시간표에 접근할 권한이 없습니다.");

        verify(timeTableRepository, never())
                .existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
                        anyLong(), anyLong(), anyString(), anyLong()
                );
    }
}
