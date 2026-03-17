package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.SendResponse;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class})
class SendToMembersTest {

    @MockBean
    private FcmTokenRepository fcmTokenRepository;

    @MockBean
    private FcmMessageRepository fcmMessageRepository;

    @MockBean
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @MockBean
    private MemberRepository memberRepository;

    @Autowired
    private FcmService fcmService;

    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @Test
    void prepareAdminNotification_usesAllTokensAndAllMembersForDefaultSend() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(null, List.of(), List.of(), List.of(), "Test Title", "Test Content");

        FcmToken linkedToken = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken unlinkedToken = new FcmToken(null, "sample_token_guest", "android");

        when(fcmTokenRepository.findAllTokens()).thenReturn(List.of(linkedToken, unlinkedToken));
        when(memberRepository.findAllIds()).thenReturn(List.of(69L, 96L));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.fcmMessageId()).isEqualTo(1L);
        assertThat(dispatch.targetCount()).isEqualTo(2);
        assertThat(dispatch.tokenAndMemberId())
                .containsEntry("sample_token_69", 69L)
                .containsEntry("sample_token_guest", -1L);
        assertThat(dispatch.targetMemberIds()).containsExactly(69L, 96L);

        verify(fcmTokenRepository).findAllTokens();
        verify(memberRepository).findAllIds();
        verifySavedPendingMessage(2);
    }

    @Test
    void prepareAdminNotification_filtersLoggedInTokensAndMembers() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(AdminNotificationTargetType.LOGGED_IN, List.of(), List.of(), List.of(), "Test Title", "Test Content");

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(fcmTokenRepository.findAllByMemberIdIsNotNull()).thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findIdsWithLinkedFcmToken()).thenReturn(List.of(69L, 96L));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.targetMemberIds()).containsExactly(69L, 96L);
        verify(fcmTokenRepository).findAllByMemberIdIsNotNull();
        verify(memberRepository).findIdsWithLinkedFcmToken();
        verifySavedPendingMessage(2);
    }

    @Test
    void prepareAdminNotification_filtersLoggedOutTokensAndMembers() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(AdminNotificationTargetType.LOGGED_OUT, List.of(), List.of(), List.of(), "Test Title", "Test Content");

        FcmToken fcmToken1 = new FcmToken(null, "sample_token_guest_1", "iphone");
        FcmToken fcmToken2 = new FcmToken(null, "sample_token_guest_2", "android");

        when(fcmTokenRepository.findAllByMemberIdIsNull()).thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findIdsWithoutLinkedFcmToken()).thenReturn(List.of(10L, 20L));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.tokenAndMemberId())
                .containsEntry("sample_token_guest_1", -1L)
                .containsEntry("sample_token_guest_2", -1L);
        assertThat(dispatch.targetMemberIds()).containsExactly(10L, 20L);

        verify(fcmTokenRepository).findAllByMemberIdIsNull();
        verify(memberRepository).findIdsWithoutLinkedFcmToken();
        verifySavedPendingMessage(2);
    }

    @Test
    void prepareAdminNotification_filtersSpecificMembers() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(
                        AdminNotificationTargetType.MEMBERS,
                        List.of(69L, 96L, 999L),
                        List.of(),
                        List.of(),
                        "Test Title",
                        "Test Content"
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");
        Member member1 = createMember(69L);
        Member member2 = createMember(96L);

        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L, 999L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findAllById(eq(List.of(69L, 96L, 999L))))
                .thenReturn(List.of(member1, member2));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.targetMemberIds()).containsExactly(69L, 96L);
        verify(fcmTokenRepository).findFcmTokensByMemberIds(eq(List.of(69L, 96L, 999L)));
        verify(memberRepository).findAllById(eq(List.of(69L, 96L, 999L)));
        verifySavedPendingMessage(2);
    }

    @Test
    void prepareAdminNotification_filtersStudentIds() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(
                        AdminNotificationTargetType.STUDENT_IDS,
                        List.of(),
                        List.of("201900069", "201900096", "209999999"),
                        List.of(),
                        "Test Title",
                        "Test Content"
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(memberRepository.findIdsByStudentIdIn(eq(List.of("201900069", "201900096", "209999999"))))
                .thenReturn(List.of(69L, 96L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.targetMemberIds()).containsExactly(69L, 96L);
        verify(memberRepository, org.mockito.Mockito.times(2))
                .findIdsByStudentIdIn(eq(List.of("201900069", "201900096", "209999999")));
        verify(fcmTokenRepository).findFcmTokensByMemberIds(eq(List.of(69L, 96L)));
        verifySavedPendingMessage(2);
    }

    @Test
    void prepareAdminNotification_filtersDepartments() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(
                        AdminNotificationTargetType.DEPARTMENTS,
                        List.of(),
                        List.of(),
                        List.of(Department.COMPUTER_ENGINEERING),
                        "Test Title",
                        "Test Content"
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(memberRepository.findIdsByDepartmentIn(eq(List.of(Department.COMPUTER_ENGINEERING))))
                .thenReturn(List.of(69L, 96L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.targetMemberIds()).containsExactly(69L, 96L);
        verify(memberRepository, org.mockito.Mockito.times(2))
                .findIdsByDepartmentIn(eq(List.of(Department.COMPUTER_ENGINEERING)));
        verify(fcmTokenRepository).findFcmTokensByMemberIds(eq(List.of(69L, 96L)));
        verifySavedPendingMessage(2);
    }

    @Test
    void sendToMembers_savesInboxForTargetMembersAndUpdatesCounts() throws FirebaseMessagingException {
        FcmMessage fcmMessage = FcmMessage.builder()
                .title("Test Title")
                .body("Test Content")
                .isAdminMessage(true)
                .build();
        fcmMessage.markPending(2);
        ReflectionTestUtils.setField(fcmMessage, "id", 1L);

        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("sample_token_69", 69L);
        tokenAndMemberId.put("sample_token_guest", -1L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L,
                "Test Title",
                "Test Content",
                tokenAndMemberId,
                List.of(69L, 77L)
        );

        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse successResponse = mock(SendResponse.class);
        SendResponse failedResponse = mock(SendResponse.class);
        FirebaseMessagingException firebaseMessagingException = mock(FirebaseMessagingException.class);

        when(fcmMessageRepository.findById(1L)).thenReturn(Optional.of(fcmMessage));
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, failedResponse));
        when(successResponse.isSuccessful()).thenReturn(true);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(firebaseMessagingException);
        when(firebaseMessagingException.getMessage()).thenReturn("registration-token-not-registered");
        when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(batchResponse);

        fcmService.sendToMembers(dispatch);

        assertThat(fcmMessage.getTargetCount()).isEqualTo(2);
        assertThat(fcmMessage.getSendCount()).isEqualTo(1);
        assertThat(fcmMessage.getFailureCount()).isEqualTo(1);
        assertThat(fcmMessage.getSendStatus()).isEqualTo(FcmSendStatus.PARTIAL_FAILURE);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Iterable<MemberFcmMessage>> captor =
                org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(memberFcmMessageRepository).saveAll(captor.capture());
        verify(memberFcmMessageRepository).flush();

        Iterator<MemberFcmMessage> iterator = captor.getValue().iterator();
        assertThat(iterator.hasNext()).isTrue();
        MemberFcmMessage firstSavedMessage = iterator.next();
        assertThat(firstSavedMessage.getMemberId()).isEqualTo(69L);
        assertThat(iterator.hasNext()).isTrue();
        MemberFcmMessage secondSavedMessage = iterator.next();
        assertThat(secondSavedMessage.getMemberId()).isEqualTo(77L);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void sendToMembers_marksFailureButStillSavesInboxWhenBatchThrows() throws FirebaseMessagingException {
        FcmMessage fcmMessage = FcmMessage.builder()
                .title("Test Title")
                .body("Test Content")
                .isAdminMessage(true)
                .build();
        fcmMessage.markPending(2);
        ReflectionTestUtils.setField(fcmMessage, "id", 1L);

        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("sample_token_69", 69L);
        tokenAndMemberId.put("sample_token_96", 96L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L,
                "Test Title",
                "Test Content",
                tokenAndMemberId,
                List.of(69L, 96L)
        );

        FirebaseMessagingException firebaseMessagingException = mock(FirebaseMessagingException.class);

        when(fcmMessageRepository.findById(1L)).thenReturn(Optional.of(fcmMessage));
        when(firebaseMessaging.sendEachForMulticast(any())).thenThrow(firebaseMessagingException);

        fcmService.sendToMembers(dispatch);

        assertThat(fcmMessage.getTargetCount()).isEqualTo(2);
        assertThat(fcmMessage.getSendCount()).isZero();
        assertThat(fcmMessage.getFailureCount()).isEqualTo(2);
        assertThat(fcmMessage.getSendStatus()).isEqualTo(FcmSendStatus.FAILED);

        verify(memberFcmMessageRepository).saveAll(any());
        verify(memberFcmMessageRepository).flush();
    }

    @Test
    void sendToMembers_savesInboxEvenWithoutPushTargets() {
        FcmMessage fcmMessage = FcmMessage.builder()
                .title("Test Title")
                .body("Test Content")
                .isAdminMessage(true)
                .build();
        fcmMessage.markPending(0);
        ReflectionTestUtils.setField(fcmMessage, "id", 1L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L,
                "Test Title",
                "Test Content",
                Map.of(),
                List.of(69L, 96L)
        );

        when(fcmMessageRepository.findById(1L)).thenReturn(Optional.of(fcmMessage));

        fcmService.sendToMembers(dispatch);

        assertThat(fcmMessage.getTargetCount()).isZero();
        assertThat(fcmMessage.getSendCount()).isZero();
        assertThat(fcmMessage.getFailureCount()).isZero();
        assertThat(fcmMessage.getSendStatus()).isEqualTo(FcmSendStatus.NO_TARGET);

        verify(memberFcmMessageRepository).saveAll(any());
        verify(memberFcmMessageRepository).flush();
    }

    private void verifySavedPendingMessage(int expectedTargetCount) {
        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        FcmMessage savedMessage = captor.getValue();
        assertThat(savedMessage.isAdminMessage()).isTrue();
        assertThat(savedMessage.getTargetCount()).isEqualTo(expectedTargetCount);
        assertThat(savedMessage.getSendCount()).isZero();
        assertThat(savedMessage.getFailureCount()).isZero();
        assertThat(savedMessage.getSendStatus()).isEqualTo(FcmSendStatus.PENDING);
    }

    private Member createMember(Long id) {
        Member member = Member.builder()
                .studentId("201900000")
                .roles(List.of("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
