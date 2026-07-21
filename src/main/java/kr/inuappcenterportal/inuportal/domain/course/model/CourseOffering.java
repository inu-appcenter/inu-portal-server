package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.Language;
import kr.inuappcenterportal.inuportal.domain.course.enums.Method;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_offering")
public class CourseOffering extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_offering_id")
    private Long id;

    @Column(length = 1024)
    private String syllabus;

    @Column(name = "subject_number")
    private String subjectNumber;

    @Enumerated(EnumType.STRING)
    private Method method;

    private String professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_department")
    private Department targetDepartment;

    @Enumerated(EnumType.STRING)
    private Language language;

    private Integer capacity;

    @Column(name = "enrolled_count")
    private Integer enrolledCount;

    @Column(columnDefinition = "TEXT")
    private String note;
}
