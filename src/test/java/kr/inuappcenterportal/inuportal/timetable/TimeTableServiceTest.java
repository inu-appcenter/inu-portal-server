package kr.inuappcenterportal.inuportal.timetable;


import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableVisibilityUpdateRequestDto;
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

        Member member = createMember(1L, "20241234");
        Semester semester = createSemester(1L);

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
    @DisplayName("다른 사용자의 시간표 이름은 수정할 수 없다")
    void 다른_사용자_시간표_이름_수정_검증_테스트() {

        // given
        Long requestMemberId = 1L;
        Long ownerMemberId = 2L;
        Long timeTableId = 1L;

        Member owner = createMember(ownerMemberId, "20240002");
        Semester semester = createSemester(1L);
        TimeTable timeTable = createTimeTable(1L, "2026-1학기", true, owner, semester);

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


    @Test
    @DisplayName("다른 사용자의 시간표 공개범위는 수정할 수 없다")
    void 다른_사용자_시간표_공개범위_수정_검증_테스트() {

        // given
        Long requestMemberId = 1L;
        Long ownerMemberId = 2L;
        Long timeTableId = 1L;

        Member owner = createMember(ownerMemberId, "20240002");
        Semester semester = createSemester(1L);
        TimeTable timeTable = createTimeTable(1L, "2026-1학기", true, owner, semester);

        when(timeTableRepository.findById(timeTableId)).thenReturn(Optional.of(timeTable));

        // when
        TimeTableVisibilityUpdateRequestDto request = new TimeTableVisibilityUpdateRequestDto(Visibility.PRIVATE);

        // then
        assertThatThrownBy(() ->
                timeTableService.setVisibility(requestMemberId, timeTableId, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 시간표에 접근할 권한이 없습니다.");

        assertThat(timeTable.getVisibility()).isEqualTo(Visibility.PUBLIC);

        verify(timeTableRepository, times(1)).findById(timeTableId);
    }


    @Test
    @DisplayName("다른 사용자의 대표 시간표를 수정할 수 없다")
    void 다른_사용자_대표_시간표_수정_검증_테스트() {
        // given
        Long requestMemberId = 1L;
        Long ownerMemberId = 2L;
        Long timeTableId = 1L;

        Member owner = createMember(ownerMemberId, "20240002");
        Semester semester = createSemester(1L);
        TimeTable timeTable = createTimeTable(1L, "2026-1학기", false, owner, semester);

        when(timeTableRepository.findById(timeTableId)).thenReturn(Optional.of(timeTable));

        // when & then
        assertThatThrownBy(() ->
                timeTableService.setIsPrimary(requestMemberId, timeTableId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 시간표에 접근할 권한이 없습니다.");

        assertThat(timeTable.isPrimary()).isFalse();

        verify(timeTableRepository, times(1)).findById(timeTableId);
        verify(timeTableRepository, never())
                .findByMemberIdAndSemesterIdAndIsPrimaryTrue(anyLong(), anyLong());

    }


    @Test
    @DisplayName("내 시간표의 이름을 변경합니다.")
    void 내_시간표_이름_수정_테스트() {
        // given
        Long requestMemberId = 1L;
        Long timeTableId = 1L;

        Member member = createMember(1L, "20241234");
        Semester semester = createSemester(1L);
        TimeTable timeTable = createTimeTable(1L, "2026-1학기", true, member, semester);

        when(timeTableRepository.findById(timeTableId)).thenReturn(Optional.of(timeTable));

        TimeTableNameUpdateRequestDto request =
                new TimeTableNameUpdateRequestDto("2026-첫번째 학기");

        when(timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
                requestMemberId,
                semester.getId(),
                request.timeTableName(),
                timeTableId
        )).thenReturn(false);

        // when
        TimeTableResponseDto response =
                timeTableService.setTimeTableName(requestMemberId, timeTableId, request);

        // then
        assertThat(response.id()).isEqualTo(timeTableId);
        assertThat(response.timeTableName()).isEqualTo("2026-첫번째 학기");
        assertThat(timeTable.getTimeTableName()).isEqualTo("2026-첫번째 학기");

        verify(timeTableRepository, times(1)).findById(timeTableId);
        verify(timeTableRepository, times(1))
                .existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
                        requestMemberId,
                        semester.getId(),
                        request.timeTableName(),
                        timeTableId
                );
    }

    @Test
    @DisplayName("내 시간표의 공개범위를 변경합니다.")
    void 내_시간표_공개범위_테스트() {
        // given
        Long requestMemberId = 1L;
        Long timeTableId = 1L;

        Member member = createMember(1L, "20241234");
        Semester semester = createSemester(1L);
        TimeTable timeTable = createTimeTable(1L, "2026-1학기", true, member, semester);

        when(timeTableRepository.findById(timeTableId)).thenReturn(Optional.of(timeTable));

        TimeTableVisibilityUpdateRequestDto request = new TimeTableVisibilityUpdateRequestDto(Visibility.PRIVATE);

        // when
        TimeTableResponseDto response = timeTableService.setVisibility(requestMemberId, timeTableId, request);

        // then
        assertThat(response.id()).isEqualTo(timeTableId);
        assertThat(response.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(timeTable.getVisibility()).isEqualTo(Visibility.PRIVATE);

        verify(timeTableRepository, times(1)).findById(timeTableId);
    }

    @Test
    @DisplayName("내 시간표의 대표 시간표를 변경합니다.")
    void 내_시간표_대표_시간표_수정_테스트() {
        // given
        Long requestMemberId = 1L;
        Long semesterId = 1L;
        Long currentPrimaryTimeTableId = 1L;
        Long newPrimaryTimeTableId = 2L;

        Member member = createMember(1L, "20241234");
        Semester semester = createSemester(semesterId);
        TimeTable currentPrimaryTimeTable =
                createTimeTable(currentPrimaryTimeTableId, "기존 대표 시간표", true, member, semester);

        TimeTable newPrimaryTimeTable =
                createTimeTable(newPrimaryTimeTableId, "새 대표 시간표", false, member, semester);

        when(timeTableRepository.findById(newPrimaryTimeTableId))
                .thenReturn(Optional.of(newPrimaryTimeTable));

        when(timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(
                requestMemberId,
                semesterId
        )).thenReturn(Optional.of(currentPrimaryTimeTable));


        // when
        TimeTableResponseDto response =
                timeTableService.setIsPrimary(requestMemberId, newPrimaryTimeTableId);

        // then
        assertThat(response.id()).isEqualTo(newPrimaryTimeTableId);
        assertThat(response.isPrimary()).isTrue();

        assertThat(currentPrimaryTimeTable.isPrimary()).isFalse();
        assertThat(newPrimaryTimeTable.isPrimary()).isTrue();

        verify(timeTableRepository, times(1)).findById(newPrimaryTimeTableId);
        verify(timeTableRepository, times(1))
                .findByMemberIdAndSemesterIdAndIsPrimaryTrue(requestMemberId, semesterId);
    }


    @Test
    @DisplayName("시간표를 삭제합니다")
    void 시간표_삭제_테스트() {

    }


    // helper 메서드
    private Member createMember(Long id, String studentId) {
        Member member = Member.builder()
                .studentId(studentId)
                .roles(List.of("ROLE_USER"))
                .build();

        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Semester createSemester(Long id) {
        Semester semester = Semester.create(
                2026,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 21)
        );

        ReflectionTestUtils.setField(semester, "id", id);
        return semester;
    }

    private TimeTable createTimeTable(
            Long id,
            String name,
            boolean isPrimary,
            Member member,
            Semester semester
    ) {
        TimeTable timeTable = TimeTable.create(name, isPrimary, member, semester);
        ReflectionTestUtils.setField(timeTable, "id", id);
        return timeTable;
    }

}
