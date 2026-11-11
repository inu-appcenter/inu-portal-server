package kr.inuappcenterportal.inuportal.domain.courseReview;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewCreateDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewRecentDto;
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
    private Member adminMember;
    private Course course;

    @BeforeEach
    void setUp() {
        author = new Member("202301234", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(author, "id", 1L);

        anotherMember = new Member("202209999", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(anotherMember, "id", 2L);

        adminMember = new Member("admin", List.of("ROLE_ADMIN"));
        ReflectionTestUtils.setField(adminMember, "id", 999L);

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
    @DisplayName("강의평 등록 성공 - 첫 등록이고 시간표 수강 이력이 있으면 수강생 인증(isVerified=true) 처리된다")
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
        given(courseReviewRepository.findByCourseIdAndMemberIdAndProfessorAndStatusNot(10L, 1L, "홍길동", ReviewStatus.DELETED))
                .willReturn(List.of());
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
    @DisplayName("동일 교과목 + 동일 교수 + 동일 수강 학기 후기가 이미 있으면 예외 발생")
    void createReview_duplicate_same_term_throwsException() {
        // given
        CourseReview existing = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2026,
                1,
                4,
                GradeSatisfaction.NORMAL,
                AssignmentAmount.NORMAL,
                TeamProject.NONE,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "20자 이상의 기존 강의평 내용입니다.",
                true
        );

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
                "20자 이상의 새로운 강의평 내용입니다. 동일 학기에 중복 작성 시도합니다."
        );

        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseReviewRepository.findByCourseIdAndMemberIdAndProfessorAndStatusNot(10L, 1L, "홍길동", ReviewStatus.DELETED))
                .willReturn(List.of(existing));

        // when & then
        assertThatThrownBy(() -> courseReviewService.createReview(10L, dto, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.COURSE_REVIEW_SAME_TERM_EXISTS);
    }

    @Test
    @DisplayName("재수강생 추가 후기 등록 성공 - 다른 학기이고 시간표 수강 인증된 학생은 추가 후기를 남길 수 있다")
    void createReview_retake_success_with_verification() {
        // given
        CourseReview existing = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2024,
                1,
                3,
                GradeSatisfaction.STRICT,
                AssignmentAmount.MANY,
                TeamProject.MANY,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "과거 학기에 작성했던 강의평 내용입니다.",
                true
        );

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
                "재수강 후 이번 학기에 다시 들은 20자 이상의 새로운 강의평 내용입니다."
        );

        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseReviewRepository.findByCourseIdAndMemberIdAndProfessorAndStatusNot(10L, 1L, "홍길동", ReviewStatus.DELETED))
                .willReturn(List.of(existing));
        given(timeTableItemRepository.existsByMemberIdAndCourseId(1L, 10L)).willReturn(true);

        // when
        CourseReviewResponseDto response = courseReviewService.createReview(10L, dto, author);

        // then
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getTerm()).isEqualTo(1);
        assertThat(response.isVerified()).isTrue();
        verify(courseReviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("재수강생 추가 후기 등록 실패 - 다른 학기더라도 포털 시간표 미인증 학생은 추가 작성 불가")
    void createReview_retake_fail_without_verification() {
        // given
        CourseReview existing = CourseReview.create(
                course,
                null,
                author,
                "홍길동",
                2024,
                1,
                3,
                GradeSatisfaction.STRICT,
                AssignmentAmount.MANY,
                TeamProject.MANY,
                ExamCount.TWICE,
                AttendanceType.ELECTRONIC,
                "과거 학기에 작성했던 강의평 내용입니다.",
                false
        );

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
                "포털 미인증 상태에서 재수강 후기 20자 이상 작성을 시도합니다."
        );

        given(courseRepository.findById(10L)).willReturn(Optional.of(course));
        given(courseReviewRepository.findByCourseIdAndMemberIdAndProfessorAndStatusNot(10L, 1L, "홍길동", ReviewStatus.DELETED))
                .willReturn(List.of(existing));
        given(timeTableItemRepository.existsByMemberIdAndCourseId(1L, 10L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> courseReviewService.createReview(10L, dto, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.COURSE_REVIEW_RETAKE_UNVERIFIED);
    }

    @Test
    @DisplayName("강의평 수정 실패 - 일반 사용자는 강의평을 수정할 수 없다 (에브리타임 방식)")
    void updateReview_user_throwsException() {
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
                "기존 20자 이상의 강의평 내용입니다.",
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

        // when & then
        assertThatThrownBy(() -> courseReviewService.updateReview(100L, updateDto, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.COURSE_REVIEW_CANNOT_EDIT);
    }

    @Test
    @DisplayName("강의평 수정 성공 - 관리자(ROLE_ADMIN)는 강의평을 수정할 수 있다")
    void updateReview_admin_success() {
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
                "기존 20자 이상의 강의평 내용입니다.",
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
                "관리자에 의해 수정된 20자 이상의 강의평 내용입니다."
        );

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when
        CourseReviewResponseDto response = courseReviewService.updateReview(100L, updateDto, adminMember);

        // then
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getGradeSatisfactionText()).isEqualTo("보통");
        assertThat(response.getAssignmentAmountText()).isEqualTo("적음");
        assertThat(response.getContent()).isEqualTo("관리자에 의해 수정된 20자 이상의 강의평 내용입니다.");
    }

    @Test
    @DisplayName("강의평 삭제 실패 - 일반 사용자는 강의평을 삭제할 수 없다 (에브리타임 방식)")
    void deleteReview_user_throwsException() {
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
                "기존 20자 이상의 강의평 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> courseReviewService.deleteReview(100L, author))
                .isInstanceOf(MyException.class)
                .extracting(ex -> ((MyException) ex).getErrorCode())
                .isEqualTo(MyErrorCode.COURSE_REVIEW_CANNOT_DELETE);
    }

    @Test
    @DisplayName("강의평 삭제 성공 - 관리자(ROLE_ADMIN)는 강의평을 삭제할 수 있다")
    void deleteReview_admin_success() {
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
                "기존 20자 이상의 강의평 내용입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 100L);

        given(courseReviewRepository.findByIdAndStatusNot(100L, ReviewStatus.DELETED))
                .willReturn(Optional.of(review));

        // when
        courseReviewService.deleteReview(100L, adminMember);

        // then
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
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

    @Test
    @DisplayName("최근 등록된 강의평 피드 조회 성공 - ACTIVE 상태인 강의평 목록이 반환된다")
    void getRecentReviews_success() {
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
                "정말 유익했던 강의입니다.",
                true
        );
        ReflectionTestUtils.setField(review, "id", 101L);

        given(courseReviewRepository.findByStatusOrderByCreateDateDesc(any(), any()))
                .willReturn(List.of(review));

        // when
        List<CourseReviewRecentDto> recentReviews = courseReviewService.getRecentReviews(10, author);

        // then
        assertThat(recentReviews).hasSize(1);
        CourseReviewRecentDto dto = recentReviews.get(0);
        assertThat(dto.getCourseTitle()).isEqualTo("자료구조");
        assertThat(dto.getProfessor()).isEqualTo("홍길동");
        assertThat(dto.getRating()).isEqualTo(5);
    }
}
