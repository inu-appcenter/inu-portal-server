    package kr.inuappcenterportal.inuportal.firebase;

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

    @SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class})
    public class SendToMembersTest {

        @MockBean
        private FcmTokenRepository fcmTokenRepository;

        @MockBean
        private FcmMessageRepository fcmMessageRepository;

        @MockBean
        private MemberFcmMessageRepository memberFcmMessageRepository;

        @Autowired
        private FcmService fcmService;

        @MockBean
        private FcmAsyncExecutor fcmAsyncExecutor;

        @Test
        void testSendToMembers_withSpecifiedMembers() {
            AdminNotificationRequest request = new AdminNotificationRequest(List.of(69L, 96L), "Test Title", "Test Body");

            FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69");
            FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96");

            Mockito.when(fcmTokenRepository.findFcmTokensByMemberIds(Mockito.anyList())).thenReturn(List.of(fcmToken1, fcmToken2));
            Mockito.when(fcmMessageRepository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);

            fcmService.sendToMembers(request);

            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(fcmTokenRepository).findFcmTokensByMemberIds(captor.capture());

            List<Long> actualMemberIds = captor.getValue();
            Assertions.assertThat(List.of(69L, 96L)).isEqualTo(actualMemberIds);

            Mockito.verify(fcmMessageRepository).save(Mockito.any(FcmMessage.class));
        }

        @Test
        void testSendToMembers_withAllMembers() {
            AdminNotificationRequest request = new AdminNotificationRequest(null, "Test Title", "Test Body");

            Mockito.when(fcmTokenRepository.findAllTokens())
                    .thenReturn(List.of("sample_token_69", "sample_token_null", "sample_token_96"));

            Mockito.when(fcmMessageRepository.save(Mockito.any()))
                    .thenAnswer(i -> i.getArguments()[0]);

            fcmService.sendToMembers(request);

            Mockito.verify(fcmTokenRepository).findAllTokens();
            Mockito.verify(fcmMessageRepository).save(Mockito.any(FcmMessage.class));
        }

    }
