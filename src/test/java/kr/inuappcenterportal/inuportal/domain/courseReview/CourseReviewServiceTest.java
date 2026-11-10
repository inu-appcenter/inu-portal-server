package kr.inuappcenterportal.inuportal.domain.courseReview;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewCreateDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewUpdateDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.*;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReviewLike;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewLikeRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewReportRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.service.CourseReviewService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    @InjectMocks
    private CourseReviewService courseReviewService;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewLikeRepository courseReviewLikeRepository;

    @Mock
    private CourseReviewReportRepository courseReviewReportRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private TimeTableItemRepository timeTableItemRepository;

    private Member author;
    private Member anotherMember;
    private Course course;

    @BeforeEach
    void setUp() {
        author = new Member("202301234", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(author, "id", 1L);

        anotherMember = new Member("202209999", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(anotherMember, "id", 2L);

        course = Course.create(
                "자료구조",
                "Data Structure",
                null,
                null,
                null,
                null,
                null,
                3,
                "자료구조 수업"
        );
        ReflectionTestUtils.setField(course, "id", 10L);
    }

    @Test
    @DisplayName("강의평 등록 성공 - 시간표 수강 이력이 있으면 수강생 인증(isVerified=true) 처리된다")
    void createReview_success_with_verification() {
        // given
        CourseReviewCreateDto dto = new CourseReviewCreateDto(
                null,
                "홍길동",
                2026,
                1,
                5,
                GradeSatisfaction.GENEROUS,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "이 강의는 정말 유익하고 시험 대비가 명확합니다. 강력 추천합니다."
        );

        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseReviewRepository.existsByCourseIdAndMemberIdAndProfessor(10L, 1L, "홍길동")).willReturn(false);
        given(timeTableItemRepository.existsByMemberIdAndCourseId(1L, 10L)).willReturn(true);

        // when
        CourseReviewResponseDto response = courseReviewService.createReview(10L, dto, author);

        // then
        assertThat(response.getProfessor()).isEqualTo("홍길동");
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.isVerified()).isTrue();
        assertThat(response.getWriterDisplay()).isEqualTo("23학번");
        verify(courseReviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("동일 교과목 + 동일 교수에 대해 이미 작성한 후기가 있으면 예외 발생")
    void createReview_duplicate_throwsException() {
        // given
        CourseReviewCreateDto dto = new CourseReviewCreateDto(
                null,
                "홍길동",
                2026,
                1,
                4,
                GradeSatisfaction.NORMAL,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "20자 이상의 강의평 내용입니다. 다시 작성하려고 시도합니다."
        );

        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseReviewRepository.existsByCourseIdAndMemberIdAndProfessor(10L, 1L, "홍길동")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> courseReviewService.createReview(10L, dto, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.COURSE_REVIEW_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("강의평 수정 성공 - 작성자 본인은 평점과 지표를 수정할 수 있다")
    void updateReview_success() {
        // given
        CourseReview review = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                5,
                GradeSatisfaction.GENEROUS,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "기존 20자 이상의 솔직한 강의평 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        CourseReviewUpdateDto updateDto = new CourseReviewUpdateDto(
                4,
                GradeSatisfaction.NORMAL,
                AssignmentAmount.FEW,
                TeamProject.NONE,
                ExamCount.ONCE,
                AttendanceType.CALL,
                "수정된 20자 이상의 새로운 강의평 내용입니다."
        );

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when
        CourseReviewResponseDto response = courseReviewService.updateReview(100L, updateDto, author);

        // then
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getGradeSatisfactionText()).isEqualTo("보통");
        assertThat(response.getAssignmentAmountText()).isEqualTo("적음");
        assertThat(response.getContent()).isEqualTo("수정된 20자 이상의 새로운 강의평 내용입니다.");
    }

    @Test
    @DisplayName("타인의 강의평 수정 시 권한 없음 예외 발생")
    void updateReview_notAuthor_throwsException() {
        // given
        CourseReview review = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                5,
                GradeSatisfaction.GENEROUS,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "기존 20자 이상의 솔직한 강의평 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        CourseReviewUpdateDto updateDto = new CourseReviewUpdateDto(
                4,
                GradeSatisfaction.NORMAL,
                AssignmentAmount.FEW,
                TeamProject.NONE,
                ExamCount.ONCE,
                AttendanceType.CALL,
                "수정 시도 내용입니다. 권한이 없어야 합니다."
        );

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> courseReviewService.updateReview(100L, updateDto, anotherMember))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.HAS_NOT_COURSE_REVIEW_AUTHORIZATION);
    }

    @Test
    @DisplayName("본인이 작성한 강의평에는 도움돼요(추천)를 누를 수 없다")
    void toggleLike_myReview_throwsException() {
        // given
        CourseReview review = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                5,
                GradeSatisfaction.GENEROUS,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "내 강의평 20자 이상 작성 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> courseReviewService.toggleLike(100L, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.CANNOT_LIKE_MY_COURSE_REVIEW);
    }

    @Test
    @DisplayName("타인 강의평 추천 토글 - 추천 시 카운트 증가, 취소 시 카운트 감소")
    void toggleLike_toggle_success() {
        // given
        CourseReview review = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                5,
                GradeSatisfaction.GENEROUS,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "내 강의평 20자 이상 작성 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));
        given(courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(100L, 2L))
                .willReturn(false)
                .willReturn(true);

        // when: 첫 번째 클릭 (추천)
        boolean firstClick = courseReviewService.toggleLike(100L, anotherMember);
        // then: 추천됨
        assertThat(firstClick).isTrue();
        assertThat(review.getLikeCount()).isEqualTo(1);
        verify(courseReviewLikeRepository).save(any(CourseReviewLike.class));

        // when: 두 번째 클릭 (취소)
        boolean secondClick = courseReviewService.toggleLike(100L, anotherMember);
        // then: 취소됨
        assertThat(secondClick).isFalse();
        assertThat(review.getLikeCount()).isEqualTo(0);
        verify(courseReviewLikeRepository).deleteByCourseReviewIdAndMemberId(100L, 2L);
    }

    @Test
    @DisplayName("강의평 신고 5회 누적 시 자동으로 BLINDED 상태로 전환된다")
    void reportReview_blinded_when_reach_threshold() {
        // given
        CourseReview review = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                1,
                GradeSatisfaction.STRICT,
                AssignmentAmount.MANY,
                TeamProject.MANY,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "신고 누적 테스트용 강의평 20자 이상 본문입니다.",
                false
        );
        ReflectionTestUtils.setField(review, "id", 100L);
        ReflectionTestUtils.setField(review, "reportCount", 4); // 이미 4회 신고됨

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));
        given(courseReviewReportRepository.existsByCourseReviewIdAndMemberId(100L, 2L))
                .willReturn(false);

        // when
        courseReviewService.reportReview(100L, "비방/욕설", "지나치게 공격적인 표현입니다.", anotherMember);

        // then
        assertThat(review.getReportCount()).isEqualTo(5);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.BLINDED);
    }
}
