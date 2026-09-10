package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.dto.SyllabusContent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "syllabus",
        uniqueConstraints = @UniqueConstraint(name = "uk_syllabus_offering",
                columnNames = "course_offering_id"))
public class Syllabus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabus_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "professor")
    private String professor;

    // 본문 전체
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "json", nullable = false)
    private SyllabusContent content;

    private Syllabus(CourseOffering courseOffering, SyllabusContent content) {
        this.courseOffering = courseOffering;
        this.subjectCode = content.subjectCode();
        this.professor = content.professor();
        this.content = content;
    }

    public static Syllabus create(CourseOffering courseOffering, SyllabusContent content) {
        return new Syllabus(courseOffering, content);
    }

    public void updateContent(SyllabusContent content) {
        this.subjectCode = content.subjectCode();
        this.professor = content.professor();
        this.content = content;
    }
}
