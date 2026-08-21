package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmTransactionService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class, FcmTransactionService.class})
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

    @MockBean
    private SemesterRepository semesterRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FcmTransactionService fcmTransactionService;

    @Autowired
    private FcmService fcmService;

    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @MockBean
    private FcmMetrics fcmMetrics;

    @Test
    void prepareAdminNotification_usesAllTokensAndAllMembersForDefaultSend() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(null, null, List.of(), List.of(), List.of(), "Test Title", "Test Content", null);

        FcmToken linkedToken = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken unlinkedToken = new FcmToken(null, "sample_token_guest", "android");

        when(fcmTokenRepository.findAllTokens()).thenReturn(List.of(linkedToken, unlinkedToken));
        when(memberRepository.findAllIds()).thenReturn(List.of(69L, 96L));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
    void prepareAdminNotification_carriesPathThrough() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(null, null, List.of(), List.of(), List.of(), "Test Title", "Test Content", "/board/1");

        FcmToken linkedToken = new FcmToken(69L, "sample_token_69", "iphone");

        when(fcmTokenRepository.findAllTokens()).thenReturn(List.of(linkedToken));
        when(memberRepository.findAllIds()).thenReturn(List.of(69L));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        assertThat(dispatch.path()).isEqualTo("/board/1");
    }

    @Test
    void prepareAdminNotification_filtersLoggedInTokensAndMembers() {
        AdminNotificationRequest request =
                new AdminNotificationRequest(AdminNotificationTargetType.LOGGED_IN, null, List.of(), List.of(), List.of(), "Test Title", "Test Content", null);

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(fcmTokenRepository.findAllByMemberIdIsNotNull()).thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findIdsWithLinkedFcmToken()).thenReturn(List.of(69L, 96L));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
                new AdminNotificationRequest(AdminNotificationTargetType.LOGGED_OUT, null, List.of(), List.of(), List.of(), "Test Title", "Test Content", null);

        FcmToken fcmToken1 = new FcmToken(null, "sample_token_guest_1", "iphone");
        FcmToken fcmToken2 = new FcmToken(null, "sample_token_guest_2", "android");

        when(fcmTokenRepository.findAllByMemberIdIsNull()).thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findIdsWithoutLinkedFcmToken()).thenReturn(List.of(10L, 20L));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
                        null,
                        List.of(69L, 96L, 999L),
                        List.of(),
                        List.of(),
                        "Test Title",
                        "Test Content",
                        null
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");
        Member member1 = createMember(69L);
        Member member2 = createMember(96L);

        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L, 999L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(memberRepository.findAllById(eq(List.of(69L, 96L, 999L))))
                .thenReturn(List.of(member1, member2));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
                        null,
                        List.of(),
                        List.of("201900069", "201900096", "209999999"),
                        List.of(),
                        "Test Title",
                        "Test Content",
                        null
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(memberRepository.findIdsByStudentIdIn(eq(List.of("201900069", "201900096", "209999999"))))
                .thenReturn(List.of(69L, 96L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
                        null,
                        List.of(),
                        List.of(),
                        List.of(Department.COMPUTER_ENGINEERING),
                        "Test Title",
                        "Test Content",
                        null
                );

        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(memberRepository.findIdsByDepartmentIn(eq(List.of(Department.COMPUTER_ENGINEERING))))
                .thenReturn(List.of(69L, 96L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
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
                List.of(69L, 77L),
                null,
                Map.of()
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
        when(firebaseMessaging.sendEach(any(List.class))).thenReturn(batchResponse);

        fcmService.sendToMembers(dispatch);

        // 상태 업데이트가 별도 서비스(FcmTransactionService)로 분리되었으므로, 
        // Mock을 사용한 테스트에서는 엔티티 내부 필드가 직접 바뀌지 않습니다.
        // 대신 해당 서비스가 올바른 인자로 호출되었는지를 검증합니다.
        verify(fcmTransactionService).updateStatusToProcessing(1L);
        verify(fcmTransactionService).updateFinalStatus(1L, 1, 1);
        verify(jdbcTemplate).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }

    @Test
    @org.junit.jupiter.api.Disabled
    void sendToMembers_marksFailureButStillSavesInboxWhenBatchThrows() throws FirebaseMessagingException {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("sample_token_69", 69L);
        tokenAndMemberId.put("sample_token_96", 96L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L,
                "Test Title",
                "Test Content",
                tokenAndMemberId,
                List.of(69L, 96L),
                null,
                Map.of()
        );

        FirebaseMessagingException firebaseMessagingException = mock(FirebaseMessagingException.class);

        when(firebaseMessaging.sendEachForMulticastAsync(any())).thenReturn(com.google.api.core.ApiFutures.immediateFailedFuture(firebaseMessagingException));

        fcmService.sendToMembers(dispatch);

        verify(fcmTransactionService).updateStatusToProcessing(1L);
        // dispatchToMembersInternal 내부에서 예외가 발생하므로 catch 블록의 markAsFailed가 호출되어야 함
        verify(fcmTransactionService).markAsFailed(eq(1L), eq(2));
        verify(jdbcTemplate).batchUpdate(anyString(), any(List.class), anyInt(), any());
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
                List.of(69L, 96L),
                null,
                Map.of()
        );

        when(fcmMessageRepository.findById(1L)).thenReturn(Optional.of(fcmMessage));

        fcmService.sendToMembers(dispatch);

        verify(fcmTransactionService).updateStatusToProcessing(1L);
        verify(fcmTransactionService).updateFinalStatus(1L, 0, 0);
        verify(jdbcTemplate).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }

    @Test
    void prepareTrackedNotification_returnsSavedMemberFcmMessageIds() {
        FcmToken fcmToken1 = new FcmToken(69L, "sample_token_69", "iphone");
        FcmToken fcmToken2 = new FcmToken(96L, "sample_token_96", "android");

        when(fcmTokenRepository.findFcmTokensByMemberIds(eq(List.of(69L, 96L))))
                .thenReturn(List.of(fcmToken1, fcmToken2));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });
        when(memberFcmMessageRepository.saveAll(anyList()))
                .thenReturn(List.of(
                        memberFcmMessage(1001L, 1L, 69L, FcmMessageType.POST_REPLY),
                        memberFcmMessage(1002L, 1L, 96L, FcmMessageType.POST_REPLY)
                ));

        TrackedNotificationDispatch dispatch = fcmService.prepareTrackedNotification(
                List.of(69L, 96L),
                "Test Title",
                "Test Content",
                FcmMessageType.POST_REPLY,
                55L,
                "/posts/55"
        );

        assertThat(dispatch.memberFcmMessageIds())
                .containsEntry(69L, 1001L)
                .containsEntry(96L, 1002L);
        assertThat(dispatch.tokenAndMemberId())
                .containsEntry("sample_token_69", 69L)
                .containsEntry("sample_token_96", 96L);
        assertThat(dispatch.path()).isEqualTo("/posts/55");
    }

    @Test
    void dispatchTrackedNotification_includesMemberFcmMessageIdInPayload() throws FirebaseMessagingException {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("sample_token_69_a", 69L);
        tokenAndMemberId.put("sample_token_69_b", 69L);
        tokenAndMemberId.put("sample_token_96", 96L);
        tokenAndMemberId.put("sample_token_guest", -1L);

        TrackedNotificationDispatch dispatch = new TrackedNotificationDispatch(
                1L,
                tokenAndMemberId,
                "Test Title",
                "Test Content",
                FcmMessageType.POST_REPLY,
                55L,
                "/posts/55",
                Map.of(69L, 1001L, 96L, 1002L)
        );

        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse successResponse = mock(SendResponse.class);

        when(batchResponse.getSuccessCount()).thenReturn(4);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, successResponse, successResponse, successResponse));
        when(successResponse.isSuccessful()).thenReturn(true);
        when(firebaseMessaging.sendEach(any(List.class))).thenReturn(batchResponse);

        fcmService.dispatchTrackedNotification(dispatch);

        org.mockito.ArgumentCaptor<List<Message>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(firebaseMessaging).sendEach(captor.capture());
        List<Message> sentMessages = captor.getValue();

        assertThat(sentMessages).hasSize(4);
        assertThat(sentMessages)
                .extracting(this::tokenOf)
                .containsExactly("sample_token_69_a", "sample_token_69_b", "sample_token_96", "sample_token_guest");

        assertThat(dataOf(sentMessages.get(0)))
                .containsEntry("type", "POST_REPLY")
                .containsEntry("targetId", "55")
                .containsEntry("path", "/posts/55")
                .containsEntry("memberFcmMessageId", "1001");
        assertThat(dataOf(sentMessages.get(1))).containsEntry("memberFcmMessageId", "1001");
        assertThat(dataOf(sentMessages.get(2))).containsEntry("memberFcmMessageId", "1002");
        assertThat(dataOf(sentMessages.get(3))).doesNotContainKey("memberFcmMessageId");

        verify(fcmTransactionService).updateFinalStatus(1L, 4, 0);
    }

    private void verifySavedPendingMessage(int expectedTargetCount) {
        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).saveAndFlush(captor.capture());
        FcmMessage savedMessage = captor.getValue();
        assertThat(savedMessage.isAdminMessage()).isTrue();
        assertThat(savedMessage.getTargetCount()).isEqualTo(expectedTargetCount);
        assertThat(savedMessage.getSendCount()).isZero();
        assertThat(savedMessage.getFailureCount()).isZero();
        assertThat(savedMessage.getSendStatus()).isEqualTo(FcmSendStatus.PENDING);
    }

    private MemberFcmMessage memberFcmMessage(Long id, Long fcmMessageId, Long memberId, FcmMessageType type) {
        MemberFcmMessage message = MemberFcmMessage.of(fcmMessageId, memberId, type);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dataOf(Message message) {
        return (Map<String, String>) ReflectionTestUtils.getField(message, "data");
    }

    private String tokenOf(Message message) {
        return (String) ReflectionTestUtils.getField(message, "token");
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
