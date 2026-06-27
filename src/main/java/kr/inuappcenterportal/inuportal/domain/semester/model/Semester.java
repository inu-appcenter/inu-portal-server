package kr.inuappcenterportal.inuportal.domain.semester.model;

import jakarta.persistence.*;
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
        name = "semesters",
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

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_opening")
    private boolean isOpening;

    public static Semester create(Integer year, SemesterTerm term, LocalDate startDate, LocalDate endDate) {
        Semester semester = new Semester();
        semester.year = year;
        semester.term = term;
        semester.startDate = startDate;
        semester.endDate = endDate;
        semester.isOpening = false;
        return semester;
    }
}
