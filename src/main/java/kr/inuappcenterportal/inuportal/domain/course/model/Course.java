package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.TargetTerm;
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
        name = "course",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_title_department",
                        columnNames = {"title", "department"}
                ),
                @UniqueConstraint(
                        name = "uk_course_course_code",
                        columnNames = {"course_code"}
                )
        }
)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id", nullable = false)
    private Long id;

    @Column(name = "course_code")
    private String courseCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String englishTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private College college;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_grade", length = 30)
    private TargetGrade targetGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_term", length = 30)
    private TargetTerm targetTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_division", length = 30)
    private CompletionDivision completionDivision;

    private Integer credit;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean active;


    private Course(
            String title,
            String englishTitle,
            Department department,
            College college,
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            Integer credit,
            String content
    ) {
        this.title = title;
        this.englishTitle = englishTitle;
        this.department = department;
        this.college = college;
        this.targetGrade = targetGrade;
        this.targetTerm = targetTerm;
        this.completionDivision = completionDivision;
        this.credit = credit;
        this.content = content;
        this.active = true;
    }

    // 생성용 정적 팩토리 메서드
    public static Course create(
            String title,
            String englishTitle,
            Department department,
            College college,
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            Integer credit,
            String content
    ) {
        return new Course(title, englishTitle, department, college, targetGrade, targetTerm, completionDivision, credit, content);
    }

    // 교육과정 크롤링에서 가져올 필수 데이터들에 대한 정적 팩토리 메서드
    public static Course create(
            String title,
            Department department,
            College college
    ) {
        return new Course(title, null, department, college, null, null, null, null, null);
    }


    public void updateBaseInfo(
            TargetGrade targetGrade,
            TargetTerm targetTerm,
            CompletionDivision completionDivision,
            Integer credit
    ) {
        if (targetGrade != null) {
            this.targetGrade = targetGrade;
        }
        if (targetTerm != null) {
            this.targetTerm = targetTerm;
        }
        if (completionDivision != null) {
            this.completionDivision = completionDivision;
        }
        if (credit != null) {
            this.credit = credit;
        }
        this.active = true;
    }

    public void updateContent(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }

    public void updateApiInfo(
            String courseCode,
            String englishTitle,
            TargetGrade targetGrade,
            CompletionDivision completionDivision,
            Integer credit
    ) {
        if (courseCode != null && !courseCode.isBlank()) {
            this.courseCode = courseCode;
        }
        if (englishTitle != null && !englishTitle.isBlank()) {
            this.englishTitle = englishTitle;
        }
        updateBaseInfo(targetGrade, null, completionDivision, credit);
    }

    public void deactivate() {
        this.active = false;
    }
}
