package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class})
public class SendToMembersTest {

    @MockBean
    private FcmTokenRepository fcmTokenRepository;

    @MockBean
    private FcmMessageRepository fcmMessageRepository;

    @MockBean
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private FcmService fcmService;

    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @Test
    void testSendToMembers_withSpecifiedMembers() throws FirebaseMessagingException {
        AdminNotificationRequest request = new AdminNotificationRequest(List.of(69L, 96L), "Test Title", "Test Content");

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96");

        when(fcmTokenRepository.findFcmTokensByMemberIds(Mockito.anyList())).thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BatchResponse mockResponse = mock(BatchResponse.class);
        when(mockResponse.getSuccessCount()).thenReturn(2);
        when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(mockResponse);

        fcmService.sendToMembers(request);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(fcmTokenRepository).findFcmTokensByMemberIds(captor.capture());

        List<Long> actualMemberIds = captor.getValue();
        Assertions.assertThat(List.of(69L, 96L)).isEqualTo(actualMemberIds);

        Mockito.verify(fcmMessageRepository).save(any(FcmMessage.class));
        Mockito.verify(firebaseMessaging).sendEachForMulticast(any());
    }

    @Test
    void testSendToMembers_withAllMembers() throws FirebaseMessagingException {
        AdminNotificationRequest request = new AdminNotificationRequest(null, "Test Title", "Test Content");

        when(fcmTokenRepository.findAllTokens()).thenReturn(List.of("token1", "token2"));
        when(fcmMessageRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BatchResponse mockResponse = mock(BatchResponse.class);
        when(mockResponse.getSuccessCount()).thenReturn(2);
        when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(mockResponse);

        fcmService.sendToMembers(request);

        Mockito.verify(fcmTokenRepository).findAllTokens();
        Mockito.verify(fcmMessageRepository).save(any(FcmMessage.class));
        Mockito.verify(firebaseMessaging).sendEachForMulticast(any());
    }

}
