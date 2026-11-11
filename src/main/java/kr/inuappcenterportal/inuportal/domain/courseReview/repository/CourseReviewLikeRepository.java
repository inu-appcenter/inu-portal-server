package kr.inuappcenterportal.inuportal.domain.courseReview.repository;

import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseReviewLikeRepository extends JpaRepository<CourseReviewLike, Long> {
    Optional<CourseReviewLike> findByCourseReviewIdAndMemberId(Long courseReviewId, Long memberId);
    boolean existsByCourseReviewIdAndMemberId(Long courseReviewId, Long memberId);
    void deleteByCourseReviewIdAndMemberId(Long courseReviewId, Long memberId);
}
