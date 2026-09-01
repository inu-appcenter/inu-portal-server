package kr.inuappcenterportal.inuportal.domain.firebase.model;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmMessageTest {

    @Test
    @DisplayName("ABANDONED는 markProcessing()으로 되살아나지 않는다")
    void abandonedIsNotResurrectedByMarkProcessing() {
        FcmMessage message = abandonedMessage();

        message.markProcessing();

        // markProcessing()이 PENDING에서만 전이하는 덕분에 격리가 유지된다.
        // 이 가드가 사라지면 레거시 행이 다시 발송 파이프라인에 올라탄다 (#431).
        assertThat(message.getSendStatus()).isEqualTo(FcmSendStatus.ABANDONED);
    }

    @Test
    @DisplayName("마이그레이션이 실제 발송 중인 행을 ABANDONED로 바꿔도, 도착한 발송 결과가 최종 상태를 확정한다")
    void realDeliveryResultOverridesAbandoned() {
        // 앱 가동 중 마이그레이션을 실행하면, CUTOFF 직전에 생성돼 아직 발송 중이던
        // 진짜 PENDING 행이 ABANDONED로 전환될 수 있다. 이때 발송이 끝나고 도착한
        // 실제 결과가 ABANDONED를 덮어써야 통계가 어긋나지 않는다.
        FcmMessage message = abandonedMessage();

        message.updateDeliveryResult(8, 2);

        assertThat(message.getSendStatus()).isEqualTo(FcmSendStatus.PARTIAL_FAILURE);
        assertThat(message.getSendCount()).isEqualTo(8);
        assertThat(message.getFailureCount()).isEqualTo(2);
        assertThat(message.getTargetCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("ABANDONED 행도 전량 성공하면 SUCCESS로 확정된다")
    void abandonedBecomesSuccessWhenDeliveryFullySucceeds() {
        FcmMessage message = abandonedMessage();

        message.updateDeliveryResult(5, 0);

        assertThat(message.getSendStatus()).isEqualTo(FcmSendStatus.SUCCESS);
        assertThat(message.getTargetCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("대상이 없는 알림은 PENDING이 아니라 NO_TARGET으로 확정된다")
    void markPendingWithoutTargetSettlesAsNoTarget() {
        // PENDING이 쌓이지 않게 하는 기존 보장. 이게 깨지면 대상 0건 알림이
        // 영구 PENDING으로 남아 이번 사고와 같은 형태가 재현된다.
        FcmMessage message = FcmMessage.builder()
                .title("인천대학교 총학생회")
                .body("본문")
                .build();

        message.markPending(0);

        assertThat(message.getSendStatus()).isEqualTo(FcmSendStatus.NO_TARGET);
    }

    @Test
    @DisplayName("대상이 있는 알림은 markPending()으로 PENDING이 된다")
    void markPendingWithTargetBecomesPending() {
        FcmMessage message = FcmMessage.builder()
                .title("인천대학교 총학생회")
                .body("본문")
                .build();

        message.markPending(3);

        assertThat(message.getSendStatus()).isEqualTo(FcmSendStatus.PENDING);
        assertThat(message.getTargetCount()).isEqualTo(3);
    }

    private FcmMessage abandonedMessage() {
        return FcmMessage.builder()
                .title("인천대학교 총학생회")
                .body("본문")
                .sendStatus(FcmSendStatus.ABANDONED)
                .build();
    }
}
