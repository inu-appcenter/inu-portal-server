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
@Table(
        name = "course_offering",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_courseOffering_semester_subjectNumber",
                        columnNames = {"semester_id", "subject_number"}
                )
        }
)
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

    private String note;

    // 생성자
    private CourseOffering(
            String syllabus,
            String subjectNumber,
            Method method,
            String professor,
            Course course,
            Semester semester,
            Department targetDepartment,
            Language language,
            Integer capacity,
            Integer enrolledCount,
            String note
    ) {
        this.syllabus = syllabus;
        this.subjectNumber = subjectNumber;
        this.method = method;
        this.professor = professor;
        this.course = course;
        this.semester = semester;
        this.targetDepartment = targetDepartment;
        this.language = language;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.note = note;
    }

    // 정적 팩토리 메서드
    public static CourseOffering create(
            String syllabus,
            String subjectNumber,
            Method method,
            String professor,
            Course course,
            Semester semester,
            Department targetDepartment,
            Language language,
            Integer capacity,
            Integer enrolledCount,
            String note
    ) {
        return new CourseOffering(
                syllabus,
                subjectNumber,
                method,
                professor,
                course,
                semester,
                targetDepartment,
                language,
                capacity,
                enrolledCount,
                note
        );
    }

    // api에서 개설 강의로 정보 업데이트 하는 메서드
    public void updateFromApi(
            Course course,
            Department department,
            Language language,
            Method method
    ) {
        this.course = course;
        this.targetDepartment = department;
        this.language = language;
        this.method = method;
    }
}
