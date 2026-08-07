package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.ISU_FLD_NAME;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.ISU_NAME;
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

    @Column(name = "course_code")
    private String courseCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer credit;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "isu_name")
    private ISU_NAME isuName;

    @Enumerated(EnumType.STRING)
    @Column(name = "isu_fld_name")
    private ISU_FLD_NAME isuFldName;

    private String note;

    private GradeRecord(
            Member member,
            Semester semester,
            String courseCode,
            String title,
            Integer credit,
            Grade grade,
            ISU_NAME isuName,
            ISU_FLD_NAME isuFldName,
            String note
    ) {
        this.member = member;
        this.semester = semester;
        this.courseCode = courseCode;
        this.title = title;
        this.credit = credit;
        this.grade = grade;
        this.isuName = isuName;
        this.isuFldName = isuFldName;
        this.note = note;
    }

    public static GradeRecord create(
            Member member,
            Semester semester,
            String courseCode,
            String title,
            Integer credit,
            Grade grade,
            ISU_NAME isuName,
            ISU_FLD_NAME isuFldName,
            String note) {
        return new GradeRecord(member, semester, courseCode, title, credit, grade, isuName, isuFldName, note);
    }

    // 개별 수정을 위한 메서드
    public void update(
            String title,
            Integer credit,
            Grade grade,
            ISU_NAME isuName,
            ISU_FLD_NAME isuFldName,
            String note
    ) {
        this.title = title;
        this.credit = credit;
        this.grade = grade;
        this.isuName = isuName;
        this.isuFldName = isuFldName;
        this.note = note;
    }
}
