package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;
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

    private String professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnctr_isu_name", nullable = false)
    private CNCTR_ISU_NAME cnctrIsuName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dept_name", nullable = false)
    private DEPT_NAME deptName;

    @Enumerated(EnumType.STRING)
    @Column(name = "college_name", nullable = false)
    private COLLEGE_NAME collegeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "isu_fld_name", nullable = false)
    private ISU_FLD_NAME isuFldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "isu_name", nullable = false)
    private ISU_NAME isuName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ssup_type_name", nullable = false)
    private SSUP_TYPE_NAME ssupTypeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "hy_name", nullable = false)
    private HY_NAME hyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "english_name", nullable = false)
    private ENGLISH_NAME englishName;

    @Column(nullable = false)
    private Integer credit;

    private Integer capacity;

    @Column(name = "enrolled_count")
    private Integer enrolledCount;

    private String note;

    // 생성자
    private CourseOffering(
            String syllabus,
            String subjectNumber,
            String professor,
            Course course,
            Semester semester,
            CNCTR_ISU_NAME cnctrIsuName,
            DEPT_NAME deptName,
            COLLEGE_NAME collegeName,
            ISU_FLD_NAME isuFldName,
            ISU_NAME isuName,
            SSUP_TYPE_NAME ssupTypeName,
            HY_NAME hyName,
            ENGLISH_NAME englishName,
            Integer credit,
            Integer capacity,
            Integer enrolledCount,
            String note
    ) {
        this.syllabus = syllabus;
        this.subjectNumber = subjectNumber;
        this.professor = professor;
        this.course = course;
        this.semester = semester;
        this.cnctrIsuName = cnctrIsuName;
        this.deptName = deptName;
        this.collegeName = collegeName;
        this.isuFldName = isuFldName;
        this.isuName = isuName;
        this.ssupTypeName = ssupTypeName;
        this.hyName = hyName;
        this.englishName = englishName;
        this.credit = credit;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.note = note;
    }

    // 정적 팩토리 메서드
    public static CourseOffering create(
            String syllabus,
            String subjectNumber,
            String professor,
            Course course,
            Semester semester,
            CNCTR_ISU_NAME cnctrIsuName,
            DEPT_NAME deptName,
            COLLEGE_NAME collegeName,
            ISU_FLD_NAME isuFldName,
            ISU_NAME isuName,
            SSUP_TYPE_NAME ssupTypeName,
            HY_NAME hyName,
            ENGLISH_NAME englishName,
            Integer credit,
            Integer capacity,
            Integer enrolledCount,
            String note
    ) {
        return new CourseOffering(
                syllabus,
                subjectNumber,
                professor,
                course,
                semester,
                cnctrIsuName,
                deptName,
                collegeName,
                isuFldName,
                isuName,
                ssupTypeName,
                hyName,
                englishName,
                credit,
                capacity,
                enrolledCount,
                note
        );
    }

    /**
     * 중복되는 개설 강의가 있을 떄(semester+haksu) api에서 개설 강의의 정보 업데이트 하는 메서드
     * (예를 들어 편람이 업데이트가 된 경우)
     */
    public void updateFromApi(
            Course course,
            CNCTR_ISU_NAME cnctrIsuName,
            DEPT_NAME deptName,
            COLLEGE_NAME collegeName,
            ISU_FLD_NAME isuFldName,
            ISU_NAME isuName,
            SSUP_TYPE_NAME ssupTypeName,
            HY_NAME hyName,
            ENGLISH_NAME englishName,
            Integer credit
    ) {
        this.course = course;
        this.cnctrIsuName = cnctrIsuName;
        this.deptName = deptName;
        this.collegeName = collegeName;
        this.isuFldName = isuFldName;
        this.isuName = isuName;
        this.ssupTypeName = ssupTypeName;
        this.hyName = hyName;
        this.englishName = englishName;
        this.credit = credit;
    }
}
