package kr.inuappcenterportal.inuportal.domain.courseReview.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "course_review_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_review_report",
                        columnNames = {"course_review_id", "member_id"}
                )
        }
)
public class CourseReviewReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_review_id", nullable = false)
    private CourseReview courseReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 500)
    private String detail;

    public CourseReviewReport(CourseReview courseReview, Member member, String reason, String detail) {
        this.courseReview = courseReview;
        this.member = member;
        this.reason = reason;
        this.detail = detail;
    }
}
