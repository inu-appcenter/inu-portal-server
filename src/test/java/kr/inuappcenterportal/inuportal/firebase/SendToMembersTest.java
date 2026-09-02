package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
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
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmTransactionService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                null
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
        when(firebaseMessagingException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.sendEachForMulticastAsync(any())).thenReturn(com.google.api.core.ApiFutures.immediateFuture(batchResponse));

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
                null
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
                null
        );

        when(fcmMessageRepository.findById(1L)).thenReturn(Optional.of(fcmMessage));

        fcmService.sendToMembers(dispatch);

        verify(fcmTransactionService).updateStatusToProcessing(1L);
        verify(fcmTransactionService).updateFinalStatus(1L, 0, 0);
        verify(jdbcTemplate).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }

    @Test
    void noticeAll_recordsSuccessStatusInsteadOfLeavingItPending() throws FirebaseMessagingException {
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 10L);
            return message;
        });

        fcmService.noticeAll("공지 제목");

        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        FcmMessage saved = captor.getValue();
        assertThat(saved.getSendStatus()).isEqualTo(FcmSendStatus.SUCCESS);
        assertThat(saved.getTargetCount()).isEqualTo(1);
        assertThat(saved.getSendCount()).isEqualTo(1);
        assertThat(saved.getFailureCount()).isZero();
    }

    @Test
    void noticeAll_recordsFailedStatusWhenTopicSendThrows() throws FirebaseMessagingException {
        FirebaseMessagingException firebaseMessagingException = mock(FirebaseMessagingException.class);
        when(firebaseMessaging.send(any(com.google.firebase.messaging.Message.class)))
                .thenThrow(firebaseMessagingException);
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 11L);
            return message;
        });

        fcmService.noticeAll("공지 제목", 42L);

        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        FcmMessage saved = captor.getValue();
        assertThat(saved.getSendStatus()).isEqualTo(FcmSendStatus.FAILED);
        assertThat(saved.getTargetCount()).isEqualTo(1);
        assertThat(saved.getSendCount()).isZero();
        assertThat(saved.getFailureCount()).isEqualTo(1);
        assertThat(saved.getTargetId()).isEqualTo(42L);
    }

    @Test
    void noticeAll_persistsFailedRecordWithoutPropagatingUnexpectedRuntimeException()
            throws FirebaseMessagingException {
        // send()는 FirebaseMessagingException만 선언하지만, SDK 내부 사정으로
        // 런타임 예외가 나올 수 있다. 이게 전파되면 @Transactional 경계가 롤백돼
        // 발송 기록 자체가 사라진다. 기록은 남기고 예외는 삼켜야 한다.
        when(firebaseMessaging.send(any(com.google.firebase.messaging.Message.class)))
                .thenThrow(new IllegalStateException("FirebaseApp is not initialized"));
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 12L);
            return message;
        });

        org.assertj.core.api.Assertions.assertThatCode(() -> fcmService.noticeAll("공지 제목"))
                .doesNotThrowAnyException();

        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getSendStatus()).isEqualTo(FcmSendStatus.FAILED);
    }

    @Test
    void noticeAll_sendsOnceAndRecordsOnceWithoutTargetIdForSingleArgOverload()
            throws FirebaseMessagingException {
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 13L);
            return message;
        });

        fcmService.noticeAll("공지 제목");

        // 단건 오버로드가 내부적으로 2-arg 버전을 호출하므로, 발송과 저장이
        // 각각 정확히 1회여야 한다 (중복 푸시/중복 행 방지).
        verify(firebaseMessaging).send(any(com.google.firebase.messaging.Message.class));
        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isNull();
    }

    @Test
    void noticeAll_recordIsNotAnAdminMessageSoItStaysOutOfAdminStats()
            throws FirebaseMessagingException {
        // 토픽 발송 행이 이제 targetCount=1, SUCCESS로 확정되므로,
        // adminMessage로 잘못 분류되면 관리자 발송 통계에 섞여 들어간다.
        when(fcmMessageRepository.save(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 14L);
            return message;
        });

        fcmService.noticeAll("공지 제목", 7L);

        org.mockito.ArgumentCaptor<FcmMessage> captor = org.mockito.ArgumentCaptor.forClass(FcmMessage.class);
        verify(fcmMessageRepository).save(captor.capture());
        assertThat(captor.getValue().isAdminMessage()).isFalse();
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

    private Member createMember(Long id) {
        Member member = Member.builder()
                .studentId("201900000")
                .roles(List.of("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    /**
     * API 타임아웃 회귀 테스트 (#143). sendEachForMulticast는 개별 토큰이 타임아웃으로 실패해도
     * BatchResponse 자체는 정상 반환한다. 예전 구현은 이 경우를 로그만 남기고 즉시 실패로 확정해,
     * 멀쩡한 토큰이 재시도 한 번 없이 유실됐다.
     */
    @Test
    void dispatchRetriesOnlyTokensThatFailedWithTransientError() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("token_ok", 69L);
        tokenAndMemberId.put("token_timeout", 96L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L, "Test Title", "Test Content", tokenAndMemberId, List.of(69L, 96L), null);

        FirebaseMessagingException timeout = mock(FirebaseMessagingException.class);
        when(timeout.getMessage()).thenReturn("Timed out while making an API call: Connect timed out");
        when(timeout.getMessagingErrorCode()).thenReturn(null);

        // 중첩 스터빙(when(...) 인자 안에서 다시 스터빙)은 Mockito가 거부하므로 미리 만들어 둔다.
        BatchResponse firstAttempt = mockBatchResponse(mockSendResponse(true, null), mockSendResponse(false, timeout));
        BatchResponse retryAttempt = mockBatchResponse(mockSendResponse(true, null));

        when(firebaseMessaging.sendEachForMulticastAsync(any()))
                .thenReturn(com.google.api.core.ApiFutures.immediateFuture(firstAttempt))
                .thenReturn(com.google.api.core.ApiFutures.immediateFuture(retryAttempt));

        fcmService.sendToMembers(dispatch);

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging, times(2)).sendEachForMulticastAsync(captor.capture());

        // 재시도는 실패한 토큰만 대상으로 한다. 이미 성공한 토큰이 빠지므로 중복 푸시도 없다.
        assertThat(tokensOf(captor.getAllValues().get(0))).containsExactly("token_ok", "token_timeout");
        assertThat(tokensOf(captor.getAllValues().get(1))).containsExactly("token_timeout");

        verify(fcmTransactionService).updateFinalStatus(1L, 2, 0);
    }

    @Test
    void dispatchDoesNotRetryPermanentFailures() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("token_dead", 69L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L, "Test Title", "Test Content", tokenAndMemberId, List.of(69L), null);

        FirebaseMessagingException unregistered = mock(FirebaseMessagingException.class);
        when(unregistered.getMessage()).thenReturn("registration-token-not-registered");
        when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        BatchResponse response = mockBatchResponse(mockSendResponse(false, unregistered));

        when(firebaseMessaging.sendEachForMulticastAsync(any()))
                .thenReturn(com.google.api.core.ApiFutures.immediateFuture(response));

        fcmService.sendToMembers(dispatch);

        verify(firebaseMessaging, times(1)).sendEachForMulticastAsync(any());
        verify(fcmTransactionService).updateFinalStatus(1L, 0, 1);
    }

    @Test
    void dispatchRetriesWholeBatchWhenTheCallItselfFails() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("token_69", 69L);
        tokenAndMemberId.put("token_96", 96L);

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L, "Test Title", "Test Content", tokenAndMemberId, List.of(69L, 96L), null);

        FirebaseMessagingException callFailure = mock(FirebaseMessagingException.class);

        when(firebaseMessaging.sendEachForMulticastAsync(any()))
                .thenReturn(com.google.api.core.ApiFutures.immediateFailedFuture(callFailure));

        fcmService.sendToMembers(dispatch);

        // 배치 호출 자체가 실패하면 개별 결과를 알 수 없으므로 전체 토큰을 최대 3회까지 재시도한다.
        verify(firebaseMessaging, times(3)).sendEachForMulticastAsync(any());
        verify(fcmTransactionService).updateFinalStatus(1L, 0, 2);
    }

    private BatchResponse mockBatchResponse(SendResponse... responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        int successCount = (int) Arrays.stream(responses).filter(SendResponse::isSuccessful).count();
        when(batchResponse.getResponses()).thenReturn(List.of(responses));
        when(batchResponse.getSuccessCount()).thenReturn(successCount);
        when(batchResponse.getFailureCount()).thenReturn(responses.length - successCount);
        return batchResponse;
    }

    private SendResponse mockSendResponse(boolean successful, FirebaseMessagingException exception) {
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(successful);
        when(sendResponse.getException()).thenReturn(exception);
        return sendResponse;
    }

    @SuppressWarnings("unchecked")
    private List<String> tokensOf(MulticastMessage message) {
        return (List<String>) ReflectionTestUtils.getField(message, "tokens");
    }
}
