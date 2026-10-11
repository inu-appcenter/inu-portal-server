package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ResultState;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 결과 1버전. 만들어진 뒤 내용은 바뀌지 않고 상태(LATEST → STALE)와 최종 여부만 바뀐다.
// 열·시간대·회의 길이는 조율에서 가져온다(조율 설정은 생성 뒤 바뀌지 않음).
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll_result",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_poll_result_poll_version", columnNames = {"poll_id", "version"})
        }
)
public class SchedulePollResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private SchedulePoll poll;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultState state;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    // 이 결과에 반영된 마지막 제출 시각
    @Column(name = "based_on_submitted_at", nullable = false)
    private LocalDateTime basedOnSubmittedAt;

    // 참여자 회원 ID 목록 JSON. 순서가 히트맵 자리(index)다. 예: [205,318]
    // 이름은 볼 때마다 지금 이름으로 보여 주려고 ID만 저장한다.
    @Column(name = "participant_member_ids", nullable = false, columnDefinition = "TEXT")
    private String participantMemberIds;

    // 히트맵 JSON. heatmap[열][칸] = 가능한 참여자 비트마스크
    @Column(nullable = false, columnDefinition = "TEXT")
    private String heatmap;

    // 추천 후보 JSON (최대 3개)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String candidates;

    private SchedulePollResult(SchedulePoll poll, int version, LocalDateTime basedOnSubmittedAt,
                               String participantMemberIds, String heatmap, String candidates) {
        this.poll = poll;
        this.version = version;
        this.state = ResultState.LATEST;
        this.isFinal = false;
        this.basedOnSubmittedAt = basedOnSubmittedAt;
        this.participantMemberIds = participantMemberIds;
        this.heatmap = heatmap;
        this.candidates = candidates;
    }

    public static SchedulePollResult create(SchedulePoll poll, int version, LocalDateTime basedOnSubmittedAt,
                                            String participantMemberIds, String heatmap, String candidates) {
        return new SchedulePollResult(poll, version, basedOnSubmittedAt, participantMemberIds, heatmap, candidates);
    }

    public void markStale() {
        this.state = ResultState.STALE;
    }
}
