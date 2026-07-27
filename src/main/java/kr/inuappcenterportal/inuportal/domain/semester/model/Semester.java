package kr.inuappcenterportal.inuportal.domain.semester.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "semester",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semester_year_term",
                columnNames = {"academic_year", "term"}
        )
)
public class Semester extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SemesterTerm term;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester_status", nullable = false)
    private SemesterStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // private 생성자
    private Semester(
            Integer year,
            SemesterTerm term,
            SemesterStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.year = year;
        this.term = term;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 정적 팩토리 메서드: private 생성자로 외부 우회로 차단
    public static Semester create(
            Integer year,
            SemesterTerm term,
            SemesterStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new Semester(year, term, status, startDate, endDate);
    }

    public void updateStatus(SemesterStatus status) {
        this.status = status;
    }

    public void updatePeriodAndStatus(
            LocalDate startDate,
            LocalDate endDate,
            SemesterStatus status
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }
}
