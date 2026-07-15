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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    private TimeTable(
            Long id,
            String timeTableName,
            boolean isPrimary,
            Visibility visibility,
            Member member,
            Semester semester
    ) {
        this.id = id;
        this.timeTableName = timeTableName;
        this.isPrimary = isPrimary;
        this.visibility = visibility;
        this.member = member;
        this.semester = semester;
    }

    private static TimeTable create(
            Long id,
            String timeTableName,
            boolean isPrimary,
            Visibility visibility,
            Member member,
            Semester semester
    ) {
        return new TimeTable(id, timeTableName, isPrimary, visibility, member, semester);
    }
}
