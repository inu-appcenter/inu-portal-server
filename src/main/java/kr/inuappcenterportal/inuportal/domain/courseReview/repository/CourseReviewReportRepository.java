package kr.inuappcenterportal.inuportal.domain.courseReview.repository;

import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewReportRepository extends JpaRepository<CourseReviewReport, Long> {
    boolean existsByCourseReviewIdAndMemberId(Long courseReviewId, Long memberId);
}
