package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordSaveRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.GradeRecordRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.GradeRecordService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeRecordServiceTest {

    @InjectMocks
    private GradeRecordService gradeRecordService;

    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private GradeRecordRepository gradeRecordRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CourseRepository courseRepository;

    private static final Long MEMBER_ID = 1L;
    private static final Long SEMESTER_ID = 10L;
    private static final int YEAR = 2024;
    private static final SemesterTerm TERM = SemesterTerm.FIRST;

    private Member member;
    private Semester semester;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        semester = mock(Semester.class);

        lenient().when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        lenient().when(semesterRepository.findByYearAndTerm(YEAR, TERM)).thenReturn(Optional.of(semester));
        lenient().when(semester.getId()).thenReturn(SEMESTER_ID);
        lenient().when(semester.getYear()).thenReturn(YEAR);
        lenient().when(semester.getTerm()).thenReturn(TERM);
    }

    private GradeRecordRequestDto record(String courseCode, String title) {
        return new GradeRecordRequestDto(courseCode, title, 3, "A+", true, false, null, null);
    }

    private GradeRecordSaveRequestDto request(GradeRecordRequestDto... records) {
        return new GradeRecordSaveRequestDto(YEAR, TERM, List.of(records));
    }

    @Nested
    @DisplayName("요청 body 내 중복 검증")
    class DuplicateValidation {

        @Test
        @DisplayName("동일한 (과목코드, 과목명) 조합이 두 번 들어오면 DUPLICATE_GRADE_RECORD 예외")
        void sameCourseCodeAndTitle_throws() {
            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "자료구조"),
                    record("ABC123", "자료구조")
            );

            MyException ex = assertThrows(MyException.class,
                    () -> gradeRecordService.replaceGradeRecord(request, MEMBER_ID));

            assertEquals(MyErrorCode.DUPLICATE_GRADE_RECORD, ex.getErrorCode());
        }

        @Test
        @DisplayName("중복이 3개 이상 섞여 있어도 예외")
        void multipleDuplicates_throws() {
            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "자료구조"),
                    record("DEF456", "알고리즘"),
                    record("ABC123", "자료구조"),
                    record("DEF456", "알고리즘")
            );

            MyException ex = assertThrows(MyException.class,
                    () -> gradeRecordService.replaceGradeRecord(request, MEMBER_ID));

            assertEquals(MyErrorCode.DUPLICATE_GRADE_RECORD, ex.getErrorCode());
        }

        @Test
        @DisplayName("과목코드가 없어도 과목명이 같으면 중복으로 처리한다")
        void nullCourseCodeSameTitle_throws() {
            GradeRecordSaveRequestDto request = request(
                    record(null, "교양세미나"),
                    record(null, "교양세미나")
            );

            MyException ex = assertThrows(MyException.class,
                    () -> gradeRecordService.replaceGradeRecord(request, MEMBER_ID));

            assertEquals(MyErrorCode.DUPLICATE_GRADE_RECORD, ex.getErrorCode());
        }

        @Test
        @DisplayName("중복이면 삭제/저장을 전혀 수행하지 않는다")
        void duplicate_doesNotTouchRepository() {
            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "자료구조"),
                    record("ABC123", "자료구조")
            );

            assertThrows(MyException.class,
                    () -> gradeRecordService.replaceGradeRecord(request, MEMBER_ID));

            verify(gradeRecordRepository, never()).deleteAllByMemberIdAndSemesterId(anyLong(), anyLong());
            verify(gradeRecordRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("과목명이 같아도 과목코드가 다르면 중복이 아니다")
        void sameTitleDifferentCode_ok() {
            when(gradeRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "실험"),
                    record("XYZ999", "실험")
            );

            List<GradeRecordResponseDto> result =
                    gradeRecordService.replaceGradeRecord(request, MEMBER_ID);

            assertEquals(2, result.size());
            verify(gradeRecordRepository).deleteAllByMemberIdAndSemesterId(MEMBER_ID, SEMESTER_ID);
            verify(gradeRecordRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("과목코드가 같아도 과목명이 다르면 중복이 아니다")
        void sameCodeDifferentTitle_ok() {
            when(gradeRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "자료구조"),
                    record("ABC123", "자료구조 및 실습")
            );

            List<GradeRecordResponseDto> result =
                    gradeRecordService.replaceGradeRecord(request, MEMBER_ID);

            assertEquals(2, result.size());
            verify(gradeRecordRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("이수구분/이수영역 값은 저장 및 응답에 모두 반영된다")
        void isuFields_areMappedToEntityAndResponse() {
            when(gradeRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GradeRecordSaveRequestDto request = request(
                    new GradeRecordRequestDto("ABC123", "자료구조", 3, "A+", true, false, "전공핵심", "전공심화")
            );

            List<GradeRecordResponseDto> result =
                    gradeRecordService.replaceGradeRecord(request, MEMBER_ID);

            ArgumentCaptor<List<GradeRecord>> captor = ArgumentCaptor.forClass(List.class);
            verify(gradeRecordRepository).saveAll(captor.capture());

            GradeRecord savedRecord = captor.getValue().get(0);
            assertEquals("전공핵심", savedRecord.getIsuName());
            assertEquals("전공심화", savedRecord.getIsuFldName());
            assertEquals("전공핵심", result.get(0).isuName());
            assertEquals("전공심화", result.get(0).isuFldName());
        }
    }

    @Nested
    @DisplayName("삭제 후 재삽입 순서")
    class ReplaceOrdering {

        @Test
        @DisplayName("정상 요청이면 삭제를 먼저 호출하고 그 다음 저장한다")
        void deletesThenSaves() {
            when(gradeRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GradeRecordSaveRequestDto request = request(
                    record("ABC123", "자료구조"),
                    record("DEF456", "알고리즘")
            );

            gradeRecordService.replaceGradeRecord(request, MEMBER_ID);

            // 서비스 코드의 호출 순서 보장. (실제 SQL flush 순서 보장은 @Modifying 네이티브
            //  DELETE 가 담당하며, 그 검증은 DB 통합 테스트 영역이다.)
            InOrder inOrder = inOrder(gradeRecordRepository);
            inOrder.verify(gradeRecordRepository).deleteAllByMemberIdAndSemesterId(MEMBER_ID, SEMESTER_ID);
            inOrder.verify(gradeRecordRepository).saveAll(anyList());
        }
    }
}
