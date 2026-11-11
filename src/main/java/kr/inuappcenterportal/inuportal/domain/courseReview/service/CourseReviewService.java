package kr.inuappcenterportal.inuportal.domain.courseReview.service;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.*;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.ReviewStatus;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReviewLike;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReviewReport;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewLikeRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewReportRepository;
import kr.inuappcenterportal.inuportal.domain.courseReview.repository.CourseReviewRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewLikeRepository courseReviewLikeRepository;
    private final CourseReviewReportRepository courseReviewReportRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final TimeTableItemRepository timeTableItemRepository;

    /**
     * 강의평 등록
     */
    @Transactional
    public CourseReviewResponseDto createReview(Long courseId, CourseReviewCreateDto dto, Member member) {
        Course course = findCourseById(courseId);

        String trimmedProfessor = dto.getProfessor().trim();
        List<CourseReview> existingReviews = courseReviewRepository.findByCourseIdAndMemberIdAndProfessorAndStatusNot(
                courseId, member.getId(), trimmedProfessor, ReviewStatus.DELETED
        );

        // 수강생 인증 판별: 회원의 시간표에 해당 과목이 담겨 있는지 여부
        boolean isVerified = timeTableItemRepository.existsByMemberIdAndCourseId(member.getId(), courseId);

        if (!existingReviews.isEmpty()) {
            // 동일 학기(연도+학기)에 이미 작성한 후기가 있는지 확인
            boolean sameTermExists = existingReviews.stream().anyMatch(r ->
                    r.getYear().equals(dto.getYear()) && r.getTerm().equals(dto.getTerm())
            );
            if (sameTermExists) {
                throw new MyException(MyErrorCode.COURSE_REVIEW_SAME_TERM_EXISTS);
            }

            // 다른 학기(재수강)인 경우: 포털 시간표 인증된 수강자만 추가 작성 가능
            if (!isVerified) {
                throw new MyException(MyErrorCode.COURSE_REVIEW_RETAKE_UNVERIFIED);
            }
        }

        CourseOffering offering = null;
        if (dto.getCourseOfferingId() != null) {
            offering = courseOfferingRepository.findById(dto.getCourseOfferingId()).orElse(null);
        }

        CourseReview review = CourseReview.create(
                course,
                offering,
                member,
                trimmedProfessor,
                dto.getYear(),
                dto.getTerm(),
                dto.getRating(),
                dto.getGradeSatisfaction(),
                dto.getAssignmentAmount(),
                dto.getTeamProject(),
                dto.getExamCount(),
                dto.getAttendanceType(),
                dto.getContent(),
                isVerified
        );

        courseReviewRepository.save(review);

        return CourseReviewResponseDto.of(review, member.getId(), false);
    }

    /**
     * 강의평 목록 페이징 조회
     */
    public Page<CourseReviewResponseDto> getReviews(
            Long courseId,
            CourseReviewSearchCondition condition,
            Pageable pageable,
            Member currentMember
    ) {
        findCourseById(courseId);
        Page<CourseReview> page = courseReviewRepository.searchReviews(courseId, condition, pageable);

        Long currentMemberId = currentMember != null ? currentMember.getId() : null;

        return page.map(review -> {
            boolean isLiked = currentMemberId != null &&
                    courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(review.getId(), currentMemberId);
            return CourseReviewResponseDto.of(review, currentMemberId, isLiked);
        });
    }

    /**
     * 강의평 요약 통계 조회
     */
    public CourseReviewSummaryDto getSummary(Long courseId, String professor) {
        Course course = findCourseById(courseId);
        return courseReviewRepository.getCourseReviewSummary(course, professor);
    }

    /**
     * 강의평 수정 (에브리타임 정책: 일반 사용자 수정 불가, 관리자만 가능)
     */
    @Transactional
    public CourseReviewResponseDto updateReview(Long reviewId, CourseReviewUpdateDto dto, Member member) {
        CourseReview review = findActiveReviewById(reviewId);

        if (!member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.COURSE_REVIEW_CANNOT_EDIT);
        }

        review.update(
                dto.getRating(),
                dto.getGradeSatisfaction(),
                dto.getAssignmentAmount(),
                dto.getTeamProject(),
                dto.getExamCount(),
                dto.getAttendanceType(),
                dto.getContent()
        );

        boolean isLiked = courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(review.getId(), member.getId());
        return CourseReviewResponseDto.of(review, member.getId(), isLiked);
    }

    /**
     * 강의평 삭제 (에브리타임 정책: 일반 사용자 삭제 불가, 관리자만 가능)
     */
    @Transactional
    public void deleteReview(Long reviewId, Member member) {
        CourseReview review = findActiveReviewById(reviewId);

        if (!member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.COURSE_REVIEW_CANNOT_DELETE);
        }

        review.delete();
    }

    /**
     * 추천(도움돼요) 토글
     * @return 추천 성공 여부 (true: 추천됨, false: 추천 취소됨)
     */
    @Transactional
    public boolean toggleLike(Long reviewId, Member member) {
        CourseReview review = findActiveReviewById(reviewId);

        if (review.getMember().getId().equals(member.getId())) {
            throw new MyException(MyErrorCode.CANNOT_LIKE_MY_COURSE_REVIEW);
        }

        if (courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(reviewId, member.getId())) {
            courseReviewLikeRepository.deleteByCourseReviewIdAndMemberId(reviewId, member.getId());
            review.decreaseLikeCount();
            return false;
        } else {
            CourseReviewLike like = new CourseReviewLike(review, member);
            courseReviewLikeRepository.save(like);
            review.increaseLikeCount();
            return true;
        }
    }

    /**
     * 강의평 신고 접수
     */
    @Transactional
    public void reportReview(Long reviewId, String reason, String detail, Member member) {
        CourseReview review = findActiveReviewById(reviewId);

        if (courseReviewReportRepository.existsByCourseReviewIdAndMemberId(reviewId, member.getId())) {
            throw new MyException(MyErrorCode.COURSE_REVIEW_ALREADY_REPORTED);
        }

        CourseReviewReport report = new CourseReviewReport(review, member, reason, detail);
        courseReviewReportRepository.save(report);

        review.increaseReportCount();
    }

    /**
     * 내가 작성한 강의평 목록 조회
     */
    public List<CourseReviewResponseDto> getMyReviews(Member member) {
        List<CourseReview> list = courseReviewRepository.findByMemberIdAndStatusNotOrderByCreateDateDesc(
                member.getId(),
                ReviewStatus.DELETED
        );

        return list.stream()
                .map(review -> {
                    boolean isLiked = courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(review.getId(), member.getId());
                    return CourseReviewResponseDto.of(review, member.getId(), isLiked);
                })
                .collect(Collectors.toList());
    }

    /**
     * 최근 등록된 강의평 피드 조회
     */
    public List<CourseReviewRecentDto> getRecentReviews(int limit, Member currentMember) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(0, safeLimit);

        List<CourseReview> list = courseReviewRepository.findByStatusOrderByCreateDateDesc(
                ReviewStatus.ACTIVE,
                pageRequest
        );

        Long currentMemberId = currentMember != null ? currentMember.getId() : null;

        return list.stream()
                .map(review -> {
                    boolean isLiked = currentMemberId != null &&
                            courseReviewLikeRepository.existsByCourseReviewIdAndMemberId(review.getId(), currentMemberId);
                    return CourseReviewRecentDto.of(review, currentMemberId, isLiked);
                })
                .collect(Collectors.toList());
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new MyException(MyErrorCode.COURSE_NOT_FOUND));
    }

    private CourseReview findActiveReviewById(Long reviewId) {
        return courseReviewRepository.findByIdAndStatusNot(reviewId, ReviewStatus.DELETED)
                .orElseThrow(() -> new MyException(MyErrorCode.COURSE_REVIEW_NOT_FOUND));
    }
}
