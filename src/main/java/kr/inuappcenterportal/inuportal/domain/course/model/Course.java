package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "courses")
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
}
