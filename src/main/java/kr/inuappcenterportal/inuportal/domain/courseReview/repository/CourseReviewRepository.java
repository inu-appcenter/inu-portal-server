package kr.inuappcenterportal.inuportal.domain.courseReview.repository;

import kr.inuappcenterportal.inuportal.domain.courseReview.enums.ReviewStatus;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long>, CourseReviewRepositoryCustom {
    boolean existsByCourseIdAndMemberIdAndProfessor(Long courseId, Long memberId, String professor);
    List<CourseReview> findByCourseIdAndMemberIdAndProfessorAndStatusNot(Long courseId, Long memberId, String professor, ReviewStatus status);
    Optional<CourseReview> findByIdAndStatusNot(Long id, ReviewStatus status);
    List<CourseReview> findByMemberIdAndStatusNotOrderByCreateDateDesc(Long memberId, ReviewStatus status);
    List<CourseReview> findByStatusOrderByCreateDateDesc(ReviewStatus status, org.springframework.data.domain.Pageable pageable);
}
