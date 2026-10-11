package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.JoinSource;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SubmissionVia;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 한 사람이 한 조율에 낸 가능 시간. 이 줄이 있으면 참여자다(제출 = 참여).
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll_submission",
        uniqueConstraints = {
                // 같은 사람이 동시에 첫 제출해도 한 줄만 생기게 한다.
                @UniqueConstraint(name = "uk_schedule_poll_submission_poll_member", columnNames = {"poll_id", "member_id"})
        }
)
public class SchedulePollSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private SchedulePoll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 가능 시간 구간 목록 JSON. 예: [{"key":"MON","start":"09:00","end":"11:00"}]
    // 검증·정리(SchedulePollSlotUtils.normalize)한 결과를 그대로 저장한다.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String available;

    // 시간표에서 불러온 수업 ID 목록 JSON. 예: [4412,4413]. 저장만 하고 검증하지 않는다.
    @Column(name = "imported_course_offering_ids", columnDefinition = "TEXT")
    private String importedCourseOfferingIds;

    // 내 제출 번호. 다른 기기에서 먼저 고쳤는지 감지할 때 쓴다.
    @Column(name = "submission_revision", nullable = false)
    private int submissionRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubmissionVia via;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_source", length = 10)
    private JoinSource joinSource;

    // 처음 제출한 시각 = 참여한 시각
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 마지막으로 제출한 시각
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    private SchedulePollSubmission(SchedulePoll poll, Member member, String available, String importedCourseOfferingIds,
                                   SubmissionVia via, JoinSource joinSource, LocalDateTime now) {
        this.poll = poll;
        this.member = member;
        this.available = available;
        this.importedCourseOfferingIds = importedCourseOfferingIds;
        this.submissionRevision = 1;
        this.via = via;
        this.joinSource = joinSource;
        this.joinedAt = now;
        this.submittedAt = now;
    }

    // 첫 제출
    public static SchedulePollSubmission create(SchedulePoll poll, Member member, String available,
                                                String importedCourseOfferingIds, SubmissionVia via,
                                                JoinSource joinSource, LocalDateTime now) {
        return new SchedulePollSubmission(poll, member, available, importedCourseOfferingIds, via, joinSource, now);
    }

    // 재제출. 덮어쓰고 제출 번호 +1. 참여 경로(joinSource)와 참여 시각은 처음 값을 유지한다.
    public void resubmit(String available, String importedCourseOfferingIds, SubmissionVia via, LocalDateTime now) {
        this.available = available;
        this.importedCourseOfferingIds = importedCourseOfferingIds;
        this.via = via;
        this.submissionRevision++;
        this.submittedAt = now;
    }
}
