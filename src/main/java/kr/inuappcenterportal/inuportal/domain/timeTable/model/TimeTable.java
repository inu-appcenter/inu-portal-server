package kr.inuappcenterportal.inuportal.domain.timeTable.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "timetables",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_member_semester_name",
                        columnNames = {"member_id", "semester_id", "timetable_name"}
                )
        }
)
public class TimeTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_id", nullable = false)
    private Long id;

    @Column(name = "timetable_name", nullable = false)
    private String timeTableName;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    private TimeTable(
            String timeTableName,
            boolean isPrimary,
            Member member,
            Semester semester
    ) {
        this.timeTableName = timeTableName;
        this.isPrimary = isPrimary;
        this.visibility = Visibility.PUBLIC;
        this.member = member;
        this.semester = semester;
    }

    public static TimeTable create(
            String timeTableName,
            boolean isPrimary,
            Member member,
            Semester semester
    ) {
        return new TimeTable(timeTableName, isPrimary, member, semester);
    }

    public void unmarkPrimary() {
        this.isPrimary = false;
    }

    public void markPrimary() {
        this.isPrimary = true;
    }

    public void updateVisibility(Visibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("공개 범위는 필수입니다.");
        }

        this.visibility = visibility;
    }

    public void updateTimeTableName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("시간표 이름은 필수입니다.");
        }

        this.timeTableName = name;
    }
}
