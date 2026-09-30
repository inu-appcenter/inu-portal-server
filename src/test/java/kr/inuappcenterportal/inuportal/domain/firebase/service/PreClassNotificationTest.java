package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private FcmMessageRepository fcmMessageRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private FcmDispatchGate fcmDispatchGate;

    @InjectMocks
    private FcmService fcmService;

    private FcmMessage savedMessage;

    @BeforeEach
    void setUp() {
        savedMessage = FcmMessage.builder().title("t").body("b").isAdminMessage(false).build();
        ReflectionTestUtils.setField(savedMessage, "id", 1L);
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenReturn(savedMessage);
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
        assertThat((List<String>) ReflectionTestUtils.getField(captor.getValue(), "tokens")).containsExactly("android");
        assertThat(savedMessage.getSendCount()).isEqualTo(2);
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
        assertThat(savedMessage.getSendCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("모든 기기가 Live Activity로 받으면 일반 알림은 보내지 않는다")
    void skipsMulticastWhenAllDevicesGotLiveActivity() throws Exception {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(MEMBER_ID)))
                .thenReturn(List.of(token("ios-la", "la-token")));
        when(fcmDispatchGate.sendOne(any(Message.class))).thenReturn("msg-id");

        fcmService.sendPreClassNotification(MEMBER_ID, "title", "body", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);

        verify(fcmDispatchGate, never()).send(any(MulticastMessage.class));
        assertThat(savedMessage.getSendCount()).isEqualTo(1);
    }
}
