package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
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

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }
}
