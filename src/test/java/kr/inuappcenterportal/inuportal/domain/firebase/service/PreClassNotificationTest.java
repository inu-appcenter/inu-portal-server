package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreClassNotificationTest {

    private static final Long MEMBER_ID = 7L;
    private static final LiveActivityStartPush PUSH =
            new LiveActivityStartPush("TimetableLiveActivity", "{}", 1_800_000_000L, 1_800_000_000L);

    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private FcmTransactionService fcmTransactionService;
    @Mock
    private FcmDispatchGate fcmDispatchGate;

    @InjectMocks
    private FcmService fcmService;

    @BeforeEach
    void setUp() {
        when(fcmTransactionService.createMemberNotification(anyString(), anyString(), anyLong(), any()))
                .thenReturn(1L);
    }

    private static FcmToken token(String value, String liveActivityToken) {
        FcmToken token = FcmToken.builder().memberId(MEMBER_ID).token(value).deviceType("IOS").build();
        token.updateLiveActivityStartToken(liveActivityToken);
        return token;
    }

    private static BatchResponse batch(int success, int failure) {
        BatchResponse response = mock(BatchResponse.class);
        when(response.getSuccessCount()).thenReturn(success);
        when(response.getFailureCount()).thenReturn(failure);
        return response;
    }

    @Test
    @DisplayName("push-to-start 토큰이 있는 기기는 Live Activity로, 나머지는 일반 알림으로 보낸다")
    void splitsLiveActivityAndNotificationTargets() throws Exception {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(MEMBER_ID)))
                .thenReturn(List.of(token("ios-la", "la-token"), token("android", null)));
        when(fcmDispatchGate.sendOne(any(Message.class))).thenReturn("msg-id");
        BatchResponse response = batch(1, 0);
        when(fcmDispatchGate.send(any(MulticastMessage.class))).thenReturn(response);

        fcmService.sendPreClassNotification(MEMBER_ID, "title", "body", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);

        verify(fcmDispatchGate, times(1)).sendOne(any(Message.class));
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(fcmDispatchGate).send(captor.capture());
        MulticastMessage sentMessage = captor.getValue();
        assertThat((List<String>) ReflectionTestUtils.getField(sentMessage, "tokens")).containsExactly("android");
        // Android 중복 시스템 알림 방지: 최상위 notification은 null이어야 하고 data 블록에 title/body가 실려야 한다
        assertThat(ReflectionTestUtils.getField(sentMessage, "notification")).isNull();
        java.util.Map<String, String> data = (java.util.Map<String, String>) ReflectionTestUtils.getField(sentMessage, "data");
        assertThat(data).containsEntry("type", "DAILY_BRIEF_TIMETABLE")
                .containsEntry("title", "title")
                .containsEntry("body", "body")
                .containsEntry("path", "/timetable");
        verify(fcmTransactionService).updateFinalStatus(1L, 2, 0);
    }

    @Test
    @DisplayName("Live Activity 발송이 실패한 기기는 일반 알림으로 대체한다")
    void fallsBackToNotificationWhenLiveActivityFails() throws Exception {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(MEMBER_ID)))
                .thenReturn(List.of(token("ios-la", "la-token")));
        when(fcmDispatchGate.sendOne(any(Message.class))).thenThrow(new RuntimeException("invalid la token"));
        BatchResponse response = batch(1, 0);
        when(fcmDispatchGate.send(any(MulticastMessage.class))).thenReturn(response);

        fcmService.sendPreClassNotification(MEMBER_ID, "title", "body", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(fcmDispatchGate).send(captor.capture());
        assertThat((List<String>) ReflectionTestUtils.getField(captor.getValue(), "tokens")).containsExactly("ios-la");
        verify(fcmTransactionService).updateFinalStatus(1L, 1, 0);
    }

    @Test
    @DisplayName("이력 생성 → 발송 → 결과 반영 순서이고, 이력은 회원 알림함에도 남긴다")
    void recordsHistoryBeforeSendingAndResultAfter() throws Exception {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(MEMBER_ID)))
                .thenReturn(List.of(token("ios-la", "la-token")));
        when(fcmDispatchGate.sendOne(any(Message.class))).thenReturn("msg-id");

        fcmService.sendPreClassNotification(MEMBER_ID, "title", "body", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);

        InOrder order = inOrder(fcmTransactionService, fcmDispatchGate);
        order.verify(fcmTransactionService).createMemberNotification("title", "body", MEMBER_ID, FcmMessageType.DAILY_BRIEF_TIMETABLE);
        order.verify(fcmDispatchGate).sendOne(any(Message.class));
        order.verify(fcmTransactionService).updateFinalStatus(eq(1L), eq(1), eq(0));
    }

    @Test
    @DisplayName("모든 기기가 Live Activity로 받으면 일반 알림은 보내지 않는다")
    void skipsMulticastWhenAllDevicesGotLiveActivity() throws Exception {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(MEMBER_ID)))
                .thenReturn(List.of(token("ios-la", "la-token")));
        when(fcmDispatchGate.sendOne(any(Message.class))).thenReturn("msg-id");

        fcmService.sendPreClassNotification(MEMBER_ID, "title", "body", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);

        verify(fcmDispatchGate, never()).send(any(MulticastMessage.class));
        verify(fcmTransactionService).updateFinalStatus(1L, 1, 0);
    }
}
