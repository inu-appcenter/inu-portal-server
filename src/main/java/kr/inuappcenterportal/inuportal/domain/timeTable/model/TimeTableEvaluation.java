package kr.inuappcenterportal.inuportal.domain.timeTable.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "timetable_evaluation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_evaluation_timetable",
                        columnNames = {"timetable_id"}
                )
        }
)
public class TimeTableEvaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_evaluation_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private TimeTable timeTable;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "timetable_hash", nullable = false, length = 64)
    private String timetableHash;

    @Column(name = "regenerate_count", nullable = false)
    private int regenerateCount = 0;

    private TimeTableEvaluation(TimeTable timeTable, String content, String timetableHash) {
        this.timeTable = timeTable;
        this.content = content;
        this.timetableHash = timetableHash;
        this.regenerateCount = 0;
    }

    public static TimeTableEvaluation create(TimeTable timeTable, String content, String timetableHash) {
        return new TimeTableEvaluation(timeTable, content, timetableHash);
    }

    public void updateContent(String content, String timetableHash, boolean isHashChanged) {
        this.content = content;
        if (isHashChanged) {
            this.timetableHash = timetableHash;
            this.regenerateCount = 0; // 시간표 구성이 변경되면 재생성 횟수 리셋
        } else {
            this.regenerateCount++; // 동일한 시간표에서 다시 생성 시 카운트 증가
        }
    }
}
