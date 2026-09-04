package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 어드민이 예약한 회원 알림 발송 요청.
 * <p>
 * {@link FcmMessage}와 의도적으로 분리한다. {@code fcm_message}는 "실제 발송이 시도된
 * 메시지"라는 의미를 유지해야 유실 보정 스케줄러의 PENDING 기반 쿼리들과 간섭하지 않는다.
 * 발송 대상은 예약 시점이 아니라 {@link #scheduledAt} 도달 시점에 {@link #requestPayload}를
 * 다시 해석해 조회한다 — 그 사이의 가입/탈퇴/토큰 변경을 반영하기 위함이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scheduled_notification")
public class ScheduledNotification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 512)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private AdminNotificationTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_filter", nullable = false, length = 32)
    private AdminNotificationSubFilter subFilter;

    /** 발송 시점에 재해석되는 AdminNotificationRequest 원본 JSON. */
    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ScheduledNotificationStatus status = ScheduledNotificationStatus.SCHEDULED;

    /** 발송 이벤트가 만든 fcm_message로의 느슨한 링크. 실제 성공/실패는 이 id로 조회한다. */
    @Column(name = "fcm_message_id")
    private Long fcmMessageId;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Builder
    public ScheduledNotification(String title, String content, String path,
                                  AdminNotificationTargetType targetType, AdminNotificationSubFilter subFilter,
                                  String requestPayload, LocalDateTime scheduledAt) {
        this.title = title;
        this.content = content;
        this.path = path;
        this.targetType = targetType;
        this.subFilter = subFilter;
        this.requestPayload = requestPayload;
        this.scheduledAt = scheduledAt;
        this.status = ScheduledNotificationStatus.SCHEDULED;
    }

    public void markSent(Long fcmMessageId) {
        this.status = ScheduledNotificationStatus.SENT;
        this.fcmMessageId = fcmMessageId;
    }

    public void markFailed(String reason) {
        this.status = ScheduledNotificationStatus.FAILED;
        this.failureReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 512));
    }

    public void markExpired() {
        this.status = ScheduledNotificationStatus.EXPIRED;
    }
}
