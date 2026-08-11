package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.member.enums.Grade;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "grade_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grade_member_semester_course",
                        columnNames = {"member_id", "semester_id", "course_code", "title"}
                )
        }
)
public class GradeRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "course_code")
    private String courseCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer credit;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    @Column(name = "is_major")
    private Boolean isMajor;

    @Column(name = "is_course_repetition")
    private Boolean isCourseRepetition;

    private GradeRecord(
            Member member,
            Semester semester,
            Course course,
            String courseCode,
            String title,
            Integer credit,
            Grade grade,
            Boolean isMajor,
            Boolean courseRepetition
    ) {
        this.member = member;
        this.semester = semester;
        this.course = course;
        this.courseCode = courseCode;
        this.title = title;
        this.credit = credit;
        this.grade = grade;
        this.isMajor = isMajor;
        this.isCourseRepetition = courseRepetition;
    }

    public static GradeRecord create(
            Member member,
            Semester semester,
            Course course,
            String courseCode,
            String title,
            Integer credit,
            Grade grade,
            Boolean isMajor,
            Boolean isCourseRepetition) {
        return new GradeRecord(member, semester, course, courseCode, title, credit, grade, isMajor, isCourseRepetition);
    }

    // 개별 수정을 위한 메서드
    public void update(
            Integer credit,
            Grade grade,
            Boolean isMajor,
            Boolean isCourseRepetition
    ) {
        this.credit = credit;
        this.grade = grade;
        this.isMajor = isMajor;
        this.isCourseRepetition = isCourseRepetition;
    }
}
