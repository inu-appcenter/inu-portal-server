package kr.inuappcenterportal.inuportal.domain.timeTable.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
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
        updateTimeTableName(timeTableName);
        this.isPrimary = isPrimary;
        this.visibility = Visibility.PUBLIC;
        this.member = member;
        this.semester = semester;
    }

    // 정적 팩토리 메서드
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
            throw new MyException(MyErrorCode.NECESSARY_VISIBILITY);
        }

        this.visibility = visibility;
    }

    public void updateTimeTableName(String name) {
        if (name == null || name.isBlank()) {
            throw new MyException(MyErrorCode.NECESSARY_TIMETABLE_NAME);
        }

        this.timeTableName = name;
    }
}
