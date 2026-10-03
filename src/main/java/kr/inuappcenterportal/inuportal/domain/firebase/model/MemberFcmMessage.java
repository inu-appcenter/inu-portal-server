package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_fcm_message",
        // 한 알림에 대해 회원당 알림함 행은 하나만 존재한다.
        // 푸시 payload의 fcmMessageId로 개인 행을 특정하는 읽음 처리가 이 유일성에 기댄다.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_fcm_message_message_member",
                columnNames = {"fcm_message_id", "member_id"}
        )
)
public class MemberFcmMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fcm_message_id", nullable = false)
    private Long fcmMessageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "fcm_message_type")
    @Enumerated(EnumType.STRING)
    private FcmMessageType fcmMessageType;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 어떤 경로로 읽음 처리됐는지. 읽지 않았으면 null이다.
     * 발송별 클릭율 집계에서 실제 클릭(PUSH/INBOX)과 일괄 읽음(BULK)을 가른다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "read_source", length = 16)
    private NotificationReadSource readSource;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    private MemberFcmMessage(Long fcmMessageId, Long memberId, FcmMessageType fcmMessageType) {
        this.fcmMessageId = fcmMessageId;
        this.memberId = memberId;
        this.fcmMessageType = fcmMessageType;
        this.isRead = false;
        this.viewCount = 0;
    }

    public static MemberFcmMessage of(Long fcmMessageId, Long memberId, FcmMessageType fcmMessageType) {
        return new MemberFcmMessage(fcmMessageId, memberId, fcmMessageType);
    }

    /**
     * 읽음 처리한다. 이미 읽은 행이면 읽은 시각은 최초 값을 유지한다.
     *
     * <p>단, 일괄 읽음(BULK)으로 먼저 닫힌 행에 뒤늦게 실제 클릭이 들어오면 경로만 승격한다.
     * 알림함을 두 번 열면 조회수 기반 자동 읽음이 먼저 걸리므로(#466의 markAsReadByViewCount),
     * 승격하지 않으면 그 뒤에 사용자가 진짜로 눌러도 클릭으로 집계되지 않아 전환율이 과소 집계된다.
     * 이미 클릭으로 기록된 행은 최초 클릭 경로를 유지한다.
     */
    public void markAsRead(NotificationReadSource readSource) {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
            this.readSource = readSource;
            return;
        }
        boolean alreadyClicked = this.readSource != null && this.readSource.isClick();
        if (readSource != null && readSource.isClick() && !alreadyClicked) {
            this.readSource = readSource;
        }
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }
}
