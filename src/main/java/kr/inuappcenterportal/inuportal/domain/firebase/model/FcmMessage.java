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

import java.time.LocalDateTime;

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

    @Column(name = "target_id")
    private Long targetId;

    /**
     * 유실 보정 스케줄러가 재발행 시 라우팅 정보를 추정하지 않고 그대로 복원할 수 있도록
     * 저장 시점의 path를 함께 영속화한다 (#431).
     */
    @Column(name = "path", length = 512)
    private String path;

    /** 관리자가 수동 재시도한 횟수. 0이면 최초 발송 결과 그대로다. */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_retried_at")
    private LocalDateTime lastRetriedAt;

    @Builder
    public FcmMessage(String title, String body, boolean isAdminMessage, int sendCount,
                      int failureCount, int targetCount, FcmSendStatus sendStatus, Long targetId, String path) {
        this.title = title;
        this.body = body;
        this.adminMessage = isAdminMessage;
        this.sendCount = Math.max(sendCount, 0);
        this.failureCount = Math.max(failureCount, 0);
        this.targetCount = Math.max(targetCount, 0);
        this.sendStatus = sendStatus == null ? FcmSendStatus.PENDING : sendStatus;
        this.targetId = targetId;
        this.path = path;
    }

    public void markPending(int targetCount) {
        this.targetCount = Math.max(targetCount, 0);
        this.sendCount = 0;
        this.failureCount = 0;
        this.sendStatus = this.targetCount == 0 ? FcmSendStatus.NO_TARGET : FcmSendStatus.PENDING;
    }

    public void markProcessing() {
        if (this.sendStatus == FcmSendStatus.PENDING) {
            this.sendStatus = FcmSendStatus.PROCESSING;
        }
    }

    public void incrementDeliveryResult(int batchSuccess, int batchFailure) {
        this.sendCount += Math.max(batchSuccess, 0);
        this.failureCount += Math.max(batchFailure, 0);
    }

    public void completeProcessing() {
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

    /**
     * 재시도 결과를 기존 성공분 위에 합산해 확정한다.
     *
     * <p>{@code previousSendCount}는 재시도 이전까지 성공한 건수다. 재시도는 실패자에게만
     * 보내므로 이전 성공분은 그대로 유효하고, 실패 건수는 이번 재시도 결과로 <b>대체</b>된다.
     * 재시도가 전부 성공하면 failureCount가 0이 되어 상태가 SUCCESS로 확정된다.
     *
     * <p>집계 단위가 토큰이라 재시도 사이에 회원의 기기 수가 바뀌면 합이 최초 targetCount와
     * 어긋날 수 있다. {@link #updateDeliveryResult}가 targetCount를 하한으로 끌어올려 보정한다.
     */
    public void applyRetryResult(int previousSendCount, int retrySuccessCount, int retryFailureCount) {
        updateDeliveryResult(Math.max(previousSendCount, 0) + Math.max(retrySuccessCount, 0), retryFailureCount);
    }
}
