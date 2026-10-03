package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 기기에 떠 있는 iOS 시간표 Live Activity 하나와 그 ActivityKit 업데이트 push 토큰.
 *
 * <p>앱이 꺼져 있으면 Activity를 갱신/종료할 수 없어, 끝난 수업의 Activity가 남아 다음 수업 것과 겹쳤다.
 * 앱이 Activity마다 업데이트 토큰을 보고하면 서버가 수업 시작 시각에 "수업 중"으로 갱신하고 종료 시각에
 * Activity를 끝낸다 ({@code LiveActivityLifecycleScheduler}).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "live_activity_instance")
public class LiveActivityInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** ActivityKit Activity.id (기기 내 UUID) */
    @Column(name = "activity_id", length = 64, nullable = false, unique = true)
    private String activityId;

    /** 이 Activity가 떠 있는 기기의 FCM 토큰. FCM으로 Live Activity를 보낼 때 함께 실어야 한다. */
    @Column(name = "fcm_token", length = 512, nullable = false)
    private String fcmToken;

    /** 이 Activity를 갱신/종료하는 ActivityKit push 토큰 (hex) */
    @Column(name = "push_token", length = 512, nullable = false)
    private String pushToken;

    /** 레이아웃 props(JSON). 갱신/종료 푸시의 content-state에 그대로(또는 phase만 바꿔) 싣는다. */
    @Column(name = "props_json", length = 2048, nullable = false)
    private String propsJson;

    /** 수업 시작/종료 시각 (epoch ms, props에서 읽음) */
    @Column(name = "start_at", nullable = false)
    private long startAt;

    @Column(name = "end_at", nullable = false)
    private long endAt;

    /** 수업 시작 갱신(ONGOING)을 보냈는지 */
    @Column(name = "ongoing_sent", nullable = false)
    private boolean ongoingSent;

    /** 종료 푸시를 보냈는지. 보낸 행은 더 처리하지 않는다. */
    @Column(name = "ended", nullable = false)
    private boolean ended;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Builder
    public LiveActivityInstance(Long memberId, String activityId, String fcmToken, String pushToken,
                                String propsJson, long startAt, long endAt) {
        this.memberId = memberId;
        this.activityId = activityId;
        this.fcmToken = fcmToken;
        this.pushToken = pushToken;
        this.propsJson = propsJson;
        this.startAt = startAt;
        this.endAt = endAt;
        this.modifiedDate = LocalDateTime.now();
    }

    /**
     * 같은 Activity의 새 보고를 반영한다. 앱은 떠 있는 Activity를 다음 수업 내용으로 바꿔 쓰기도 하므로,
     * 수업 시각이 바뀌었으면 갱신/종료 진행 상태를 처음부터 다시 잡는다.
     */
    public void refresh(Long memberId, String fcmToken, String pushToken, String propsJson, long startAt, long endAt) {
        boolean classChanged = this.startAt != startAt || this.endAt != endAt;
        this.memberId = memberId;
        this.fcmToken = fcmToken;
        this.pushToken = pushToken;
        this.propsJson = propsJson;
        this.startAt = startAt;
        this.endAt = endAt;
        if (classChanged) {
            this.ongoingSent = false;
            this.ended = false;
        }
        this.modifiedDate = LocalDateTime.now();
    }

    public void markOngoingSent() {
        this.ongoingSent = true;
        this.modifiedDate = LocalDateTime.now();
    }

    public void markEnded() {
        this.ended = true;
        this.modifiedDate = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return Objects.equals(this.memberId, memberId);
    }
}
