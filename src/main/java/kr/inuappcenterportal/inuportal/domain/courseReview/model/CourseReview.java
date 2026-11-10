package kr.inuappcenterportal.inuportal.domain.courseReview.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "course_review",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_review_member_course_prof",
                        columnNames = {"course_id", "member_id", "professor"}
                )
        }
)
public class CourseReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id")
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String professor;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer term;

    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_satisfaction", nullable = false, length = 30)
    private GradeSatisfaction gradeSatisfaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_amount", nullable = false, length = 30)
    private AssignmentAmount assignmentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_project", nullable = false, length = 30)
    private TeamProject teamProject;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_count", nullable = false, length = 30)
    private ExamCount examCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 30)
    private AttendanceType attendanceType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status;

    @Builder
    private CourseReview(
            Course course,
            CourseOffering courseOffering,
            Member member,
            String professor,
            Integer year,
            Integer term,
            Integer rating,
            GradeSatisfaction gradeSatisfaction,
            AssignmentAmount assignmentAmount,
            TeamProject teamProject,
            ExamCount examCount,
            AttendanceType attendanceType,
            String content,
            boolean isVerified
    ) {
        this.course = course;
        this.courseOffering = courseOffering;
        this.member = member;
        this.professor = professor;
        this.year = year;
        this.term = term;
        this.rating = rating;
        this.gradeSatisfaction = gradeSatisfaction;
        this.assignmentAmount = assignmentAmount;
        this.teamProject = teamProject;
        this.examCount = examCount;
        this.attendanceType = attendanceType;
        this.content = content;
        this.isVerified = isVerified;
        this.likeCount = 0;
        this.reportCount = 0;
        this.status = ReviewStatus.ACTIVE;
    }

    public static CourseReview create(
            Course course,
            CourseOffering courseOffering,
            Member member,
            String professor,
            Integer year,
            Integer term,
            Integer rating,
            GradeSatisfaction gradeSatisfaction,
            AssignmentAmount assignmentAmount,
            TeamProject teamProject,
            ExamCount examCount,
            AttendanceType attendanceType,
            String content,
            boolean isVerified
    ) {
        return CourseReview.builder()
                .course(course)
                .courseOffering(courseOffering)
                .member(member)
                .professor(professor)
                .year(year)
                .term(term)
                .rating(rating)
                .gradeSatisfaction(gradeSatisfaction)
                .assignmentAmount(assignmentAmount)
                .teamProject(teamProject)
                .examCount(examCount)
                .attendanceType(attendanceType)
                .content(content)
                .isVerified(isVerified)
                .build();
    }

    public void update(
            Integer rating,
            GradeSatisfaction gradeSatisfaction,
            AssignmentAmount assignmentAmount,
            TeamProject teamProject,
            ExamCount examCount,
            AttendanceType attendanceType,
            String content
    ) {
        this.rating = rating;
        this.gradeSatisfaction = gradeSatisfaction;
        this.assignmentAmount = assignmentAmount;
        this.teamProject = teamProject;
        this.examCount = examCount;
        this.attendanceType = attendanceType;
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void increaseReportCount() {
        this.reportCount++;
        if (this.reportCount >= 5) {
            this.status = ReviewStatus.BLINDED;
        }
    }

    public void delete() {
        this.status = ReviewStatus.DELETED;
    }
}
