package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_poll_link_token", columnNames = {"link_token"})
        },
        indexes = {
                // 마감 스케줄러 조회(status <> CLOSED AND deadline_at <= now)용
                @Index(name = "idx_schedule_poll_due", columnList = "status, deadline_at")
        }
)
public class SchedulePoll extends BaseTimeEntity {

    public static final int MAX_PARTICIPANTS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Member creator;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    // 정기일 때만 값이 있다. "MON,WED,FRI"(월→일 순). 일회성이면 NULL
    @Column(length = 27)
    private String days;

    // 일회성일 때만 값이 있다.
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // 하루 시작부터의 분. "24:00"을 담으려고 시각 타입 대신 숫자로 저장한다. (09:00 → 540, 24:00 → 1440)
    @Column(name = "range_start_min", nullable = false)
    private int rangeStartMinutes;

    @Column(name = "range_end_min", nullable = false)
    private int rangeEndMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_mode", nullable = false, length = 10)
    private DurationMode durationMode;

    @Column(name = "duration_min", nullable = false)
    private int durationMinutes;

    // 서울 시각. null이면 마감 없음
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "origin_chat_room_id")
    private Long originChatRoomId;

    @Column(name = "link_token", nullable = false, length = 32)
    private String linkToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SchedulePollStatus status;

    // 폴링 API가 조회 한 번으로 끝나도록 참여자 수를 여기에 같이 둔다.
    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(nullable = false)
    private long revision;

    @Column(name = "result_version", nullable = false)
    private int resultVersion;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 마감 1시간 전 알림을 보낸 시각. 마감을 바꾸면 초기화한다.
    @Column(name = "deadline_reminded_at")
    private LocalDateTime deadlineRemindedAt;

    // 마감 처리를 한 시각. 값이 있으면 다시 처리하지 않는다.
    @Column(name = "deadline_processed_at")
    private LocalDateTime deadlineProcessedAt;

    private SchedulePoll(Member creator, String title, boolean recurring, List<PollDay> days,
                         LocalDate startDate, LocalDate endDate, int rangeStartMinutes, int rangeEndMinutes,
                         DurationMode durationMode, int durationMinutes, LocalDateTime deadlineAt,
                         Long originChatRoomId, String linkToken) {
        this.creator = creator;
        this.title = title;
        this.recurring = recurring;
        this.days = recurring ? joinDays(days) : null;
        this.startDate = recurring ? null : startDate;
        this.endDate = recurring ? null : endDate;
        this.rangeStartMinutes = rangeStartMinutes;
        this.rangeEndMinutes = rangeEndMinutes;
        this.durationMode = durationMode;
        this.durationMinutes = durationMinutes;
        this.deadlineAt = deadlineAt;
        this.originChatRoomId = originChatRoomId;
        this.linkToken = linkToken;
        this.status = SchedulePollStatus.COLLECTING;
        this.participantCount = 0;
        this.revision = 1;
        this.resultVersion = 0;
    }

    public static SchedulePoll createRecurring(Member creator, String title, List<PollDay> days,
                                               int rangeStartMinutes, int rangeEndMinutes,
                                               DurationMode durationMode, int durationMinutes,
                                               LocalDateTime deadlineAt, Long originChatRoomId, String linkToken) {
        return new SchedulePoll(creator, title, true, days, null, null, rangeStartMinutes, rangeEndMinutes,
                durationMode, durationMinutes, deadlineAt, originChatRoomId, linkToken);
    }

    public static SchedulePoll createOneTime(Member creator, String title, LocalDate startDate, LocalDate endDate,
                                             int rangeStartMinutes, int rangeEndMinutes,
                                             DurationMode durationMode, int durationMinutes,
                                             LocalDateTime deadlineAt, Long originChatRoomId, String linkToken) {
        return new SchedulePoll(creator, title, false, List.of(), startDate, endDate, rangeStartMinutes, rangeEndMinutes,
                durationMode, durationMinutes, deadlineAt, originChatRoomId, linkToken);
    }

    // 정기 조율의 요일 목록. 일회성이면 빈 목록
    public List<PollDay> getDays() {
        if (days == null || days.isBlank()) {
            return List.of();
        }
        return Arrays.stream(days.split(",")).map(PollDay::valueOf).toList();
    }

    public boolean isClosed() {
        return status == SchedulePollStatus.CLOSED;
    }

    // 마감 시각이 되었는데 아직 끝나지 않은 상태
    public boolean isDeadlinePassed(LocalDateTime now) {
        return !isClosed() && deadlineAt != null && !now.isBefore(deadlineAt);
    }

    public boolean isDeadlineProcessed() {
        return deadlineProcessedAt != null;
    }

    // 마감 때문에 끝나면 끝난 시각은 마감 시각이다(처리 전 조회 응답과 같은 값). 개설자 종료면 지금 시각.
    // 실제로 처리한 시각은 deadlineProcessedAt에 남긴다.
    public void close(LocalDateTime now) {
        changeStatus(SchedulePollStatus.CLOSED);
        this.closedAt = deadlineAt != null && !now.isBefore(deadlineAt) ? deadlineAt : now;
        this.deadlineProcessedAt = now;
    }

    public void increaseRevision() {
        this.revision++;
    }

    // 첫 제출로 참여자가 늘었을 때. 나가기가 없어 줄지 않는다.
    public void addParticipant() {
        this.participantCount++;
    }

    // 결과를 새로 계산했을 때. 결과 번호 +1, 참여자 2명 이상이므로 RESULT_READY
    public int nextResultVersion() {
        changeStatus(SchedulePollStatus.RESULT_READY);
        return ++this.resultVersion;
    }

    private static String joinDays(List<PollDay> days) {
        return String.join(",", days.stream().sorted().distinct().map(PollDay::name).toList());
    }

    // 상태는 앞으로만 바뀐다.
    private void changeStatus(SchedulePollStatus next) {
        if (next.ordinal() < this.status.ordinal()) {
            throw new IllegalStateException("조율 상태를 되돌릴 수 없습니다: " + this.status + " → " + next);
        }
        this.status = next;
    }
}
