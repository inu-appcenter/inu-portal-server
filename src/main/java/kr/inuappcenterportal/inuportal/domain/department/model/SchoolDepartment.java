package kr.inuappcenterportal.inuportal.domain.department.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "school_department", uniqueConstraints =
        @UniqueConstraint(name = "uk_school_department_code", columnNames = "department_code"))
public class SchoolDepartment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "school_department_id")
    private Long id;

    @Column(name = "department_code", nullable = false)
    private String code;

    @Column(name = "department_name", nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "source_year", nullable = false)
    private Integer sourceYear;

    @Column(name = "source_term", nullable = false)
    private String sourceTerm;

    private SchoolDepartment(String code, String name, Integer sourceYear, String sourceTerm) {
        this.code = code;
        this.name = name;
        this.active = true;
        this.sourceYear = sourceYear;
        this.sourceTerm = sourceTerm;
    }

    public static SchoolDepartment create(String code, String name, Integer sourceYear, String sourceTerm) {
        return new SchoolDepartment(code, name, sourceYear, sourceTerm);
    }

    public void refresh(String name, Integer sourceYear, String sourceTerm) {
        this.name = name;
        this.sourceYear = sourceYear;
        this.sourceTerm = sourceTerm;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
