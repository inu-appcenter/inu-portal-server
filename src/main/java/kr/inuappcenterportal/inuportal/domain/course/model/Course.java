package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_title_department",
                        columnNames = {"title", "department"}
                )
        }
)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private College college;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetGrade targetGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_term")
    private TargetTerm targetTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_division", nullable = false)
    private CompletionDivision completionDivision;

    @Column(nullable = false)
    private String credit;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean active;


    private Course(
            String title,
            Department department,
            College college,
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            String credit,
            String content
    ) {
        this.title = title;
        this.department = department;
        this.college = college;
        this.targetGrade = targetGrade;
        this.targetTerm = targetTerm;
        this.completionDivision = completionDivision;
        this.credit = credit;
        this.content = content;
        this.active = true;
    }

    public static Course create(
            String title,
            Department department,
            College college,
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            String credit,
            String content
    ) {
        return new Course(title, department, college, targetGrade, targetTerm, completionDivision, credit, content);
    }

    public void update(
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            String credit,
            String content
    ) {
        this.targetGrade = targetGrade;
        this.targetTerm = targetTerm;
        this.completionDivision = completionDivision;
        this.credit = credit;
        this.content = content;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
