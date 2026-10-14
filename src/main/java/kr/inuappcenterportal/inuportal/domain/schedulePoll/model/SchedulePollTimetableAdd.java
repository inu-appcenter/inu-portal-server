package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결과를 내 시간표에 추가한 이력. 한 결과(조율·버전)에서는 한 사람당 후보 하나만 추가된다(명세서 R12).
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll_timetable_add",
        uniqueConstraints = {
                // 시간표 추가 연타 방지
                @UniqueConstraint(name = "uk_schedule_poll_timetable_add_member_poll_version",
                        columnNames = {"member_id", "poll_id", "version"})
        }
)
public class SchedulePollTimetableAdd extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(nullable = false)
    private int version;

    @Column(name = "candidate_rank", nullable = false)
    private int candidateRank;

    // 만든 시간표 요소와 커스텀 일정. 사용자가 시간표에서 지우면 요소가 없어진다.
    @Column(name = "timetable_item_id", nullable = false)
    private Long timetableItemId;

    @Column(name = "custom_schedule_id", nullable = false)
    private Long customScheduleId;

    private SchedulePollTimetableAdd(Long memberId, Long pollId, int version, int candidateRank,
                                     Long timetableItemId, Long customScheduleId) {
        this.memberId = memberId;
        this.pollId = pollId;
        this.version = version;
        this.candidateRank = candidateRank;
        this.timetableItemId = timetableItemId;
        this.customScheduleId = customScheduleId;
    }

    public static SchedulePollTimetableAdd create(Long memberId, Long pollId, int version, int candidateRank,
                                                  Long timetableItemId, Long customScheduleId) {
        return new SchedulePollTimetableAdd(memberId, pollId, version, candidateRank, timetableItemId, customScheduleId);
    }

    // 지워진 일정을 다시 추가했을 때
    public void replace(int candidateRank, Long timetableItemId, Long customScheduleId) {
        this.candidateRank = candidateRank;
        this.timetableItemId = timetableItemId;
        this.customScheduleId = customScheduleId;
    }
}
