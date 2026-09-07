package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageFailedTargetRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmRetryServiceTest {

    @Mock
    private FcmMessageRepository fcmMessageRepository;

    @Mock
    private FcmMessageFailedTargetRepository fcmMessageFailedTargetRepository;

    @Mock
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FcmAsyncService fcmAsyncService;

    @InjectMocks
    private FcmRetryService fcmRetryService;

    private static FcmMessage adminMessage(int sendCount, int failureCount) {
        return FcmMessage.builder()
                .title("공지")
                .body("본문")
                .isAdminMessage(true)
                .sendCount(sendCount)
                .failureCount(failureCount)
                .targetCount(sendCount + failureCount)
                .sendStatus(FcmSendStatus.PARTIAL_FAILURE)
                .build();
    }

    private static FcmToken token(String value, Long memberId) {
        return FcmToken.builder().token(value).memberId(memberId).deviceType("android").build();
    }

    @Test
    @DisplayName("실패한 회원의 현재 토큰만 재발송 대상이 되고, 이전 성공분은 그대로 넘겨진다")
    void retry_dispatchesOnlyFailedMembersWithTheirCurrentTokens() {
        FcmMessage message = adminMessage(1149, 2147);
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(13030L)).thenReturn(Optional.of(message));
        when(fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(13030L)).thenReturn(List.of(11L, 22L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(11L, 22L)))
                .thenReturn(List.of(token("newToken11", 11L), token("newToken22", 22L)));
        when(fcmMessageRepository.leaseForRetry(eq(13030L), any())).thenReturn(1);
        when(memberFcmMessageRepository.findDistinctTypesByFcmMessageId(13030L))
                .thenReturn(List.of(FcmMessageType.GENERAL));
        when(fcmMessageRepository.findById(13030L)).thenReturn(Optional.of(message));

        AdminNotificationResponse response = fcmRetryService.retry(13030L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Long>> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmAsyncService).retryAsync(
                eq(13030L), tokensCaptor.capture(), eq("공지"), eq("본문"),
                eq(FcmMessageType.GENERAL), eq(null), eq(null), eq(1149));

        assertThat(tokensCaptor.getValue())
                .as("실패했던 회원의 현재 토큰만 대상이어야 이미 받은 사람에게 중복이 가지 않는다")
                .containsExactlyInAnyOrderEntriesOf(Map.of("newToken11", 11L, "newToken22", 22L));
        assertThat(response.retryableCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패 기록이 없으면 전원 재발송하지 않고 거부한다")
    void retry_rejectsWhenNoFailedTargetRecorded() {
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(1L)).thenReturn(Optional.of(adminMessage(10, 0)));
        when(fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> fcmRetryService.retry(1L))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.FCM_RETRY_NO_TARGET);

        verify(fcmAsyncService, never()).retryAsync(
                anyLong(), any(), any(), any(), any(), any(), any(), anyInt());
        verify(fcmMessageRepository, never()).leaseForRetry(anyLong(), any());
    }

    @Test
    @DisplayName("실패자는 있지만 살아 있는 토큰이 없으면 선점하지 않고 거부한다")
    void retry_rejectsWhenFailedMembersHaveNoTokens() {
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(1L)).thenReturn(Optional.of(adminMessage(0, 5)));
        when(fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(1L)).thenReturn(List.of(7L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(7L))).thenReturn(List.of());

        assertThatThrownBy(() -> fcmRetryService.retry(1L))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.FCM_RETRY_NO_TARGET);

        verify(fcmMessageRepository, never()).leaseForRetry(anyLong(), any());
    }

    /**
     * 선점 실패는 곧 "다른 요청이 이미 이 알림을 재발송 중"이라는 뜻이다. 그대로 진행하면
     * 같은 사람에게 푸시가 두 번 나간다. 버튼 연타를 막는 유일한 방어선이라 반드시 발송을 건너뛰어야 한다.
     */
    @Test
    @DisplayName("선점에 실패하면(연타·동시 요청) 재발송하지 않고 409로 거부한다")
    void retry_rejectsWhenLeaseFails() {
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(1L)).thenReturn(Optional.of(adminMessage(3, 2)));
        when(fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(1L)).thenReturn(List.of(7L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(7L))).thenReturn(List.of(token("t7", 7L)));
        when(fcmMessageRepository.leaseForRetry(eq(1L), any())).thenReturn(0);

        assertThatThrownBy(() -> fcmRetryService.retry(1L))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.FCM_RETRY_NOT_ALLOWED);

        verify(fcmAsyncService, never()).retryAsync(
                anyLong(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("존재하지 않거나 관리자 알림이 아니면 404로 거부한다")
    void retry_rejectsUnknownMessage() {
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fcmRetryService.retry(999L))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.FCM_MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("한 회원의 기기가 여러 대면 모든 토큰이 재발송 대상이 된다")
    void retry_includesEveryDeviceOfAFailedMember() {
        FcmMessage message = adminMessage(0, 3);
        when(fcmMessageRepository.findByIdAndAdminMessageTrue(5L)).thenReturn(Optional.of(message));
        when(fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(5L)).thenReturn(List.of(9L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(anyList()))
                .thenReturn(List.of(token("phone", 9L), token("tablet", 9L)));
        when(fcmMessageRepository.leaseForRetry(eq(5L), any())).thenReturn(1);
        when(memberFcmMessageRepository.findDistinctTypesByFcmMessageId(5L))
                .thenReturn(List.of(FcmMessageType.GENERAL));
        when(fcmMessageRepository.findById(5L)).thenReturn(Optional.of(message));

        fcmRetryService.retry(5L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Long>> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmAsyncService).retryAsync(
                eq(5L), tokensCaptor.capture(), any(), any(), any(), any(), any(), eq(0));

        assertThat(tokensCaptor.getValue()).containsOnlyKeys("phone", "tablet");
    }
}
