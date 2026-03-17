package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_message")
public class FcmMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "is_admin_message", nullable = false)
    private boolean adminMessage = false;

    @Column(name = "send_count", nullable = false)
    private int sendCount = 0;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "target_count", nullable = false)
    private int targetCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 32)
    private FcmSendStatus sendStatus = FcmSendStatus.PENDING;

    @Builder
    public FcmMessage(String title, String body, boolean isAdminMessage, int sendCount,
                      int failureCount, int targetCount, FcmSendStatus sendStatus) {
        this.title = title;
        this.body = body;
        this.adminMessage = isAdminMessage;
        this.sendCount = Math.max(sendCount, 0);
        this.failureCount = Math.max(failureCount, 0);
        this.targetCount = Math.max(targetCount, 0);
        this.sendStatus = sendStatus == null ? FcmSendStatus.PENDING : sendStatus;
    }

    public void markPending(int targetCount) {
        this.targetCount = Math.max(targetCount, 0);
        this.sendCount = 0;
        this.failureCount = 0;
        this.sendStatus = this.targetCount == 0 ? FcmSendStatus.NO_TARGET : FcmSendStatus.PENDING;
    }

    public void updateDeliveryResult(int successCount, int failureCount) {
        this.sendCount = Math.max(successCount, 0);
        this.failureCount = Math.max(failureCount, 0);
        this.targetCount = Math.max(this.targetCount, this.sendCount + this.failureCount);

        if (this.targetCount == 0) {
            this.sendStatus = FcmSendStatus.NO_TARGET;
        } else if (this.failureCount == 0) {
            this.sendStatus = FcmSendStatus.SUCCESS;
        } else if (this.sendCount == 0) {
            this.sendStatus = FcmSendStatus.FAILED;
        } else {
            this.sendStatus = FcmSendStatus.PARTIAL_FAILURE;
        }
    }

    public void markFailed(int targetCount) {
        this.targetCount = Math.max(targetCount, 0);
        this.sendCount = 0;
        this.failureCount = this.targetCount;
        this.sendStatus = this.targetCount == 0 ? FcmSendStatus.NO_TARGET : FcmSendStatus.FAILED;
    }
}
