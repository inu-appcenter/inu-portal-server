package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.GradeEvaluation;
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

    @Column(name = "dept_code")
    private String deptCode;

    @Column(name = "dept_name_raw")
    private String deptNameRaw;

    @Column(name = "college_code")
    private String collegeCode;

    @Column(name = "college_name_raw")
    private String collegeNameRaw;

    @Column(name = "hy_code")
    private String hyCode;

    @Column(name = "hy_name_raw")
    private String hyNameRaw;

    @Column(name = "isu_code")
    private String isuCode;

    @Column(name = "isu_name_raw")
    private String isuNameRaw;

    @Column(name = "isu_fld_code")
    private String isuFldCode;

    @Column(name = "isu_fld_name_raw")
    private String isuFldNameRaw;

    @Column(name = "ssup_type_code")
    private String ssupTypeCode;

    @Column(name = "ssup_type_name_raw")
    private String ssupTypeNameRaw;

    @Column(name = "cnctr_isu_code")
    private String cnctrIsuCode;

    @Column(name = "cnctr_isu_name_raw")
    private String cnctrIsuNameRaw;

    @Column(name = "english_code")
    private String englishCode;

    @Column(name = "english_yn")
    private String englishYn;

    @Column(name = "english_name_raw")
    private String englishNameRaw;

    @Column(name = "grade_evaluation_raw")
    private String gradeEvaluationRaw;

    @Column(name = "huss_course_yn")
    private String hussCourseYn;

    private String professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false)
    private Integer credit;

    private Integer capacity;

    @Column(name = "enrolled_count")
    private Integer enrolledCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_evaluation")
    private GradeEvaluation gradeEvaluation;

    // 생성자
    private CourseOffering(
            String syllabus,
            String subjectNumber,
            String deptCode,
            String deptNameRaw,
            String collegeCode,
            String collegeNameRaw,
            String hyCode,
            String hyNameRaw,
            String isuCode,
            String isuNameRaw,
            String isuFldCode,
            String isuFldNameRaw,
            String ssupTypeCode,
            String ssupTypeNameRaw,
            String cnctrIsuCode,
            String cnctrIsuNameRaw,
            String englishYn,
            String englishCode,
            String englishNameRaw,
            String hussCourseYn,
            String gradeEvaluationRaw,
            String professor,
            Course course,
            Semester semester,
            GradeEvaluation gradeEvaluation,
            Integer credit,
            Integer capacity,
            Integer enrolledCount
    ) {
        this.syllabus = syllabus;
        this.subjectNumber = subjectNumber;
        this.deptCode = deptCode;
        this.deptNameRaw = deptNameRaw;
        this.collegeCode = collegeCode;
        this.collegeNameRaw = collegeNameRaw;
        this.hyCode = hyCode;
        this.hyNameRaw = hyNameRaw;
        this.isuCode = isuCode;
        this.isuNameRaw = isuNameRaw;
        this.isuFldCode = isuFldCode;
        this.isuFldNameRaw = isuFldNameRaw;
        this.ssupTypeCode = ssupTypeCode;
        this.ssupTypeNameRaw = ssupTypeNameRaw;
        this.cnctrIsuCode = cnctrIsuCode;
        this.cnctrIsuNameRaw = cnctrIsuNameRaw;
        this.englishYn = englishYn;
        this.englishCode = englishCode;
        this.englishNameRaw = englishNameRaw;
        this.hussCourseYn = hussCourseYn;
        this.gradeEvaluationRaw = gradeEvaluationRaw;
        this.professor = professor;
        this.course = course;
        this.semester = semester;
        this.gradeEvaluation = gradeEvaluation;
        this.credit = credit;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
    }

    // 정적 팩토리 메서드
    public static CourseOffering create(
            String syllabus,
            String subjectNumber,
            String deptCode,
            String deptNameRaw,
            String collegeCode,
            String collegeNameRaw,
            String hyCode,
            String hyNameRaw,
            String isuCode,
            String isuNameRaw,
            String isuFldCode,
            String isuFldNameRaw,
            String ssupTypeCode,
            String ssupTypeNameRaw,
            String cnctrIsuCode,
            String cnctrIsuNameRaw,
            String englishYn,
            String englishCode,
            String englishNameRaw,
            String hussCourseYn,
            String gradeEvaluationRaw,
            String professor,
            Course course,
            Semester semester,
            GradeEvaluation gradeEvaluation,
            Integer credit,
            Integer capacity,
            Integer enrolledCount
    ) {
        return new CourseOffering(
                syllabus,
                subjectNumber,
                deptCode,
                deptNameRaw,
                collegeCode,
                collegeNameRaw,
                hyCode,
                hyNameRaw,
                isuCode,
                isuNameRaw,
                isuFldCode,
                isuFldNameRaw,
                ssupTypeCode,
                ssupTypeNameRaw,
                cnctrIsuCode,
                cnctrIsuNameRaw,
                englishYn,
                englishCode,
                englishNameRaw,
                hussCourseYn,
                gradeEvaluationRaw,
                professor,
                course,
                semester,
                gradeEvaluation,
                credit,
                capacity,
                enrolledCount
        );
    }

    /**
     * 중복되는 개설 강의가 있을 떄(semester+haksu) api에서 개설 강의의 정보 업데이트 하는 메서드
     * (예를 들어 편람이 업데이트가 된 경우)
     */
    public void updateFromApi(
            Course course,
            String deptCode,
            String deptNameRaw,
            String collegeCode,
            String collegeNameRaw,
            String hyCode,
            String hyNameRaw,
            String isuCode,
            String isuNameRaw,
            String isuFldCode,
            String isuFldNameRaw,
            String ssupTypeCode,
            String ssupTypeNameRaw,
            String cnctrIsuCode,
            String cnctrIsuNameRaw,
            String englishYn,
            String englishCode,
            String englishNameRaw,
            String hussCourseYn,
            Integer credit
    ) {
        this.course = course;
        this.deptCode = deptCode;
        this.deptNameRaw = deptNameRaw;
        this.collegeCode = collegeCode;
        this.collegeNameRaw = collegeNameRaw;
        this.hyCode = hyCode;
        this.hyNameRaw = hyNameRaw;
        this.isuCode = isuCode;
        this.isuNameRaw = isuNameRaw;
        this.isuFldCode = isuFldCode;
        this.isuFldNameRaw = isuFldNameRaw;
        this.ssupTypeCode = ssupTypeCode;
        this.ssupTypeNameRaw = ssupTypeNameRaw;
        this.cnctrIsuCode = cnctrIsuCode;
        this.cnctrIsuNameRaw = cnctrIsuNameRaw;
        this.englishYn = englishYn;
        this.englishCode = englishCode;
        this.englishNameRaw = englishNameRaw;
        this.hussCourseYn = hussCourseYn;
        this.credit = credit;
    }


    /**
     * 엑셀 파일에서 파싱한 교수명 업데이트 메서드
     * (public void updateFromExcel(...)) 나중에 이런 식으로 확장
     */
    public void updateFromExcel(
            String professor,
            Integer capacity,
            String gradeEvaluationRaw,
            GradeEvaluation gradeEvaluation
    ) {
        if (professor != null && !professor.isBlank()) {
            this.professor = professor.trim();
        }

        if (capacity != null) {
            this.capacity = capacity;
        }

        if (gradeEvaluation != null) {
            this.gradeEvaluation = gradeEvaluation;
        }

        if (gradeEvaluationRaw != null && !gradeEvaluationRaw.isBlank()) {
            this.gradeEvaluationRaw = gradeEvaluationRaw.trim();
        }
    }
}
