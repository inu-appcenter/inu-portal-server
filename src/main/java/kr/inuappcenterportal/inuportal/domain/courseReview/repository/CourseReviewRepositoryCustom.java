package kr.inuappcenterportal.inuportal.domain.courseReview.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewSearchCondition;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewSummaryDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewRepositoryCustom {
    Page<CourseReview> searchReviews(Long courseId, CourseReviewSearchCondition condition, Pageable pageable);
    CourseReviewSummaryDto getCourseReviewSummary(Course course, String professor);
}
