package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private static final long UNLINKED_MEMBER_ID = -1L;

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessageRepository fcmMessageRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;
    private final FcmAsyncExecutor fcmAsyncExecutor;
    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FcmTransactionService fcmTransactionService;
    private final FcmMetrics fcmMetrics;

    @Transactional
    public void saveToken(TokenRequestDto tokenRequestDto, Long memberId) {
        FcmToken fcmToken = fcmTokenRepository.findByToken(tokenRequestDto.getToken())
                .orElse(FcmToken.builder().token(tokenRequestDto.getToken()).memberId(memberId).build());

        if (memberId != null || fcmToken.getId() == null) {
            fcmToken.updateMemberId(memberId);
        }
        fcmToken.updateTimeNow();
        fcmToken.updateDeviceType(tokenRequestDto.getDeviceType());

        if (fcmToken.getId() == null) {
            fcmTokenRepository.save(fcmToken);
        }
    }

    @Transactional
    public void deleteToken(String token, Long memberId) {
        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElseThrow(() -> new MyException(MyErrorCode.TOKEN_NOT_FOUND));

        if (memberId == null || (fcmToken.getMemberId() != null && !Objects.equals(fcmToken.getMemberId(), memberId))) {
            throw new MyException(MyErrorCode.TOKEN_NOT_FOUND);
        }

        fcmToken.clearMemberId();
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToAdmin(String title, String body) {
        List<String> target = fcmTokenRepository.findAllAdminTokens();
        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, target.size(), null);

        if (target.isEmpty()) {
            return;
        }

        MulticastMessage message = createMulticastMessage(target, title, body, null, null);
        long startNanos = System.nanoTime();
        int batchSuccess = 0;
        int batchFailure = 0;
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            batchSuccess = response.getSuccessCount();
            batchFailure = response.getFailureCount();
            fcmMessage.updateDeliveryResult(batchSuccess, batchFailure);
            log.info("Admin notification sent: target={}, success={}, failure={}",
                    target.size(), batchSuccess, batchFailure);
        } catch (FirebaseMessagingException e) {
            batchFailure = target.size();
            fcmMessage.markFailed(target.size());
            log.warn("Admin notification send failed: {}", e.getMessage());
        } catch (Exception e) {
            batchFailure = target.size();
            fcmMessage.markFailed(target.size());
            log.error("Admin notification send failed unexpectedly: target={}, message={}",
                    target.size(), e.getMessage(), e);
        } finally {
            fcmMetrics.recordBatch("ADMIN", target.size(), batchSuccess, batchFailure, System.nanoTime() - startNanos);
        }
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToAll(String title, String body) {
        List<String> target = fcmTokenRepository.findAllStringTokens();
        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, target.size(), null);

        if (target.isEmpty()) {
            return;
        }

        fcmAsyncExecutor.clearFailedTokens();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < target.size(); i += 500) {
            List<String> tokens = target.subList(i, Math.min(i + 500, target.size()));
            futures.add(fcmAsyncExecutor.sendMessage(tokens, body, title));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            List<String> failedTokens = fcmAsyncExecutor.getFailedTokensList();
            int failureCount = failedTokens.size();
            int successCount = Math.max(target.size() - failureCount, 0);

            fcmMessage.updateDeliveryResult(successCount, failureCount);
            fcmAsyncExecutor.clearFailedTokens();

            log.info("Broadcast notification finished: target={}, success={}, failure={}",
                    target.size(), successCount, failureCount);
        }
    }

    @Transactional
    @Async("messageExecutor")
    public void noticeAll(String title) {
        com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                .setTopic("notice")
                .setNotification(
                        Notification.builder()
                                .setTitle("인천대학교 총학생회")
                                .setBody(title)
                                .build()
                )
                .putData("type", "GENERAL")
                .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("Notice topic send failed: {}", e.getMessage());
        }
        fcmMessageRepository.save(FcmMessage.builder()
                .title("인천대학교 총학생회")
                .body(title)
                .build());
    }

    @Transactional
    public Long prepareKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType fcmMessageType, Long targetId) {
        if (tokenAndMemberId.isEmpty()) {
            return null;
        }

        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, tokenAndMemberId.size(), targetId);

        List<Long> targetMemberIds = tokenAndMemberId.values().stream()
                .filter(id -> id != null && !id.equals(UNLINKED_MEMBER_ID))
                .distinct()
                .toList();

        batchInsertMemberFcmMessages(fcmMessage.getId(), targetMemberIds, fcmMessageType);

        return fcmMessage.getId();
    }

    public void dispatchKeywordNotice(Long fcmMessageId, Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType type, Long targetId) {
        dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, type, targetId, null);
    }

    public void dispatchKeywordNotice(Long fcmMessageId, Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType type, Long targetId, String path) {
        if (fcmMessageId == null || tokenAndMemberId.isEmpty()) {
            return;
        }
        DeliveryResult deliveryResult = dispatchToMembersInternal(fcmMessageId, tokenAndMemberId, title, body, type, targetId, path);
        fcmTransactionService.updateFinalStatus(fcmMessageId, deliveryResult.successCount(), deliveryResult.failureCount());
    }

    @Transactional
    public AdminNotificationDispatch prepareAdminNotification(AdminNotificationRequest request) {
        NotificationTargets notificationTargets = getAdminNotificationTargets(request);
        FcmMessage fcmMessage = saveTrackedMessage(
                request.title(),
                request.content(),
                true,
                notificationTargets.tokenAndMemberId().size(),
                null
        );

        return new AdminNotificationDispatch(
                fcmMessage.getId(),
                request.title(),
                request.content(),
                Map.copyOf(notificationTargets.tokenAndMemberId()),
                List.copyOf(notificationTargets.targetMemberIds())
        );
    }

    /**
     * 비동기로 실행되며, 전체 트랜잭션 없이 각 단계별로 트랜잭션을 분리하여 처리합니다.
     */
    public void sendToMembers(AdminNotificationDispatch dispatch) {
        // 1. 상태를 PROCESSING으로 변경
        fcmTransactionService.updateStatusToProcessing(dispatch.fcmMessageId());

        // 2. 수신 이력 대량 저장 (JdbcTemplate 사용)
        if (!dispatch.targetMemberIds().isEmpty()) {
            batchInsertMemberFcmMessages(dispatch.fcmMessageId(), dispatch.targetMemberIds(), FcmMessageType.GENERAL);
        }

        if (!dispatch.hasTarget()) {
            log.info("Admin member notification stored without push targets: fcmMessageId={}, memberTargets={}",
                    dispatch.fcmMessageId(), dispatch.memberTargetCount());
            fcmTransactionService.updateFinalStatus(dispatch.fcmMessageId(), 0, 0);
            return;
        }

        try {
            // 3. 실제 FCM 발송 및 실시간 카운트 업데이트
            DeliveryResult deliveryResult = dispatchToMembersInternal(
                    dispatch.fcmMessageId(),
                    dispatch.tokenAndMemberId(),
                    dispatch.title(),
                    dispatch.content(),
                    FcmMessageType.GENERAL,
                    null
            );

            // 4. 최종 상태 업데이트
            fcmTransactionService.updateFinalStatus(dispatch.fcmMessageId(), deliveryResult.successCount(), deliveryResult.failureCount());

            log.info("Admin member notification finished: fcmMessageId={}, target={}, success={}, failure={}",
                    dispatch.fcmMessageId(), dispatch.targetCount(), deliveryResult.successCount(), deliveryResult.failureCount());
        } catch (Exception e) {
            log.error("Admin member notification failed: fcmMessageId={}, target={}, message={}",
                    dispatch.fcmMessageId(), dispatch.targetCount(), e.getMessage(), e);
            fcmTransactionService.markAsFailed(dispatch.fcmMessageId(), dispatch.targetCount());
        }
    }

    private void batchInsertMemberFcmMessages(Long fcmMessageId, List<Long> memberIds, FcmMessageType type) {
        List<Long> distinctIds = distinctMemberIds(memberIds);
        if (distinctIds.isEmpty()) return;

        String sql = "INSERT INTO member_fcm_message (fcm_message_id, member_id, fcm_message_type, create_date, modified_date) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, distinctIds, 500, (PreparedStatement ps, Long memberId) -> {
            ps.setLong(1, fcmMessageId);
            ps.setLong(2, memberId);
            ps.setString(3, type.name());
            ps.setObject(4, now);
            ps.setObject(5, now);
        });
    }

    private DeliveryResult dispatchToMembersInternal(Long fcmMessageId, Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType type, Long targetId) {
        return dispatchToMembersInternal(fcmMessageId, tokenAndMemberId, title, body, type, targetId, null);
    }

    private DeliveryResult dispatchToMembersInternal(Long fcmMessageId, Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType type, Long targetId, String path) {
        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());
        int batchSize = 500;
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            MulticastMessage message = createMulticastMessage(batchTokens, title, body, type, targetId, path);

            int batchSuccess = 0;
            int batchFailure = 0;
            long startNanos = System.nanoTime();

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                batchSuccess = response.getSuccessCount();
                batchFailure = response.getFailureCount();

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse sendResponse = responses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String token = batchTokens.get(j);
                        FirebaseMessagingException exception = sendResponse.getException();
                        log.warn("FCM send failed: token={}, error={}", token, exception != null ? exception.getMessage() : "unknown");
                    }
                }
            } catch (Exception e) {
                batchFailure = batchTokens.size();
                log.error("FCM batch send failed: fcmMessageId={}, batchSize={}, message={}", fcmMessageId, batchTokens.size(), e.getMessage());
            } finally {
                String metricType = type != null ? type.name() : "UNKNOWN";
                fcmMetrics.recordBatch(metricType, batchTokens.size(), batchSuccess, batchFailure, System.nanoTime() - startNanos);
            }

            successCount += batchSuccess;
            failureCount += batchFailure;
            // 각 배치마다 즉시 DB에 반영
            fcmTransactionService.updateIncrementalResult(fcmMessageId, batchSuccess, batchFailure);
        }

        return new DeliveryResult(successCount, failureCount);
    }

    /**
     * DB에 이력을 남기지 않고 여러 사용자에게 알림을 보냅니다.
     * @param memberIds 대상 사용자 ID 목록
     * @param title 알림 제목
     * @param body 알림 내용
     */
    @Transactional(readOnly = true)
    public void sendUntrackedNotification(List<Long> memberIds, String title, String body) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        List<FcmToken> tokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Long> tokenAndMemberId = tokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getToken,
                        FcmToken::getMemberId,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        DeliveryResult deliveryResult = dispatchToMembersInternal(null, tokenAndMemberId, title, body, null, null);
        log.info("Untracked notification sent: targets={}, success={}, failure={}",
                tokenAndMemberId.size(), deliveryResult.successCount(), deliveryResult.failureCount());
    }

    /**
     * DB에 이력을 남기면서(알림함 노출) 푸시 알림을 보냅니다.
     */
    @Transactional
    public TrackedNotificationDispatch prepareTrackedNotification(List<Long> memberIds, String title, String body, FcmMessageType type, Long targetId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getToken,
                        fcmToken -> fcmToken.getMemberId() == null ? UNLINKED_MEMBER_ID : fcmToken.getMemberId(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, tokenAndMemberId.size(), targetId);
        batchInsertMemberFcmMessages(fcmMessage.getId(), memberIds, type);

        return new TrackedNotificationDispatch(fcmMessage.getId(), tokenAndMemberId, title, body, type, targetId);
    }

    public void dispatchTrackedNotification(TrackedNotificationDispatch dispatch) {
        if (dispatch == null) {
            return;
        }
        if (!dispatch.tokenAndMemberId().isEmpty()) {
            DeliveryResult deliveryResult = dispatchToMembersInternal(dispatch.fcmMessageId(), dispatch.tokenAndMemberId(), dispatch.title(), dispatch.body(), dispatch.type(), dispatch.targetId());
            fcmTransactionService.updateFinalStatus(dispatch.fcmMessageId(), deliveryResult.successCount(), deliveryResult.failureCount());
        } else {
            fcmTransactionService.updateFinalStatus(dispatch.fcmMessageId(), 0, 0);
        }
    }


    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> countAdminFcmMessagesSuccess(int page) {
        Pageable pageable = PageRequest.of(page - 1, 8, Sort.by(Sort.Direction.DESC, "id"));
        Page<FcmMessage> fcmMessages = fcmMessageRepository.findAllByAdminMessageTrue(pageable);
        return fcmMessages.stream().map(AdminNotificationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public AdminNotificationResponse findAdminNotificationResult(Long fcmMessageId) {
        FcmMessage fcmMessage = fcmMessageRepository.findByIdAndAdminMessageTrue(fcmMessageId)
                .orElseThrow(() -> new MyException(MyErrorCode.MESSAGE_NOT_FOUND));
        return AdminNotificationResponse.of(fcmMessage);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<NotificationResponse> findNotifications(Member member, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<MemberFcmMessage> messages = memberFcmMessageRepository.findAllByMemberId(member.getId(), pageable);

        Map<Long, FcmMessage> fcmMessageMap = fcmMessageRepository.findAllById(
                        messages.stream().map(MemberFcmMessage::getFcmMessageId).toList()
                ).stream()
                .collect(Collectors.toMap(FcmMessage::getId, message -> message, (existing, replacement) -> existing));

        List<NotificationResponse> notificationResponses = messages.stream().map(message -> {
            FcmMessage fcmMessage = fcmMessageMap.get(message.getFcmMessageId());
            if (fcmMessage == null) {
                throw new MyException(MyErrorCode.MESSAGE_NOT_FOUND);
            }
            return NotificationResponse.from(message, fcmMessage);
        }).toList();

        return ListResponseDto.of(messages.getTotalPages(), messages.getTotalElements(), notificationResponses);
    }

    private MulticastMessage createMulticastMessage(List<String> tokens, String title, String body, FcmMessageType type, Long targetId) {
        return createMulticastMessage(tokens, title, body, type, targetId, null);
    }

    private MulticastMessage createMulticastMessage(List<String> tokens, String title, String body, FcmMessageType type, Long targetId, String path) {
        // notification(title, body)과 data를 항상 함께 발송한다 (data-only 발송 금지)
        // Android 백그라운드 노출 및 포그라운드 배너 표시를 위해 두 블록이 모두 필요하다
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (type != null) {
            builder.putData("type", type.name());
        }
        if (targetId != null) {
            builder.putData("targetId", String.valueOf(targetId));

            // 공지사항인 경우 noticeId로도 탑재해 주어 클라이언트 편의 제공
            if (type == FcmMessageType.SCHOOL_NOTICE || type == FcmMessageType.DEPARTMENT) {
                builder.putData("noticeId", String.valueOf(targetId));
            }
        }
        // 라우팅 정보는 클라이언트가 data 블록에서만 판단하므로 notification에는 넣지 않는다
        if (path != null && !path.isBlank()) {
            builder.putData("path", path);
        }
        return builder.build();
    }

    /**
     * 채팅 알림을 위한 특화된 푸시 발송 메서드 (OS별 그룹화 및 무음 처리 지원)
     */
    @Transactional(readOnly = true)
    public void sendChatNotification(List<Long> memberIds, String title, String body, Long chatRoomId, boolean isMuted) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        List<FcmToken> tokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        if (tokens.isEmpty()) {
            return;
        }

        List<String> tokenStrings = tokens.stream().map(FcmToken::getToken).toList();
        int batchSize = 500;
        for (int i = 0; i < tokenStrings.size(); i += batchSize) {
            List<String> batchTokens = tokenStrings.subList(i, Math.min(i + batchSize, tokenStrings.size()));
            MulticastMessage message = createChatMessage(batchTokens, title, body, chatRoomId, isMuted);
            long startNanos = System.nanoTime();
            int batchSuccess = 0;
            int batchFailure = 0;
            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                batchSuccess = response.getSuccessCount();
                batchFailure = response.getFailureCount();
                log.info("Chat push sent: room={}, isMuted={}, targets={}, success={}, failure={}",
                        chatRoomId, isMuted, batchTokens.size(), batchSuccess, batchFailure);
            } catch (Exception e) {
                batchFailure = batchTokens.size();
                log.error("Chat push failed: room={}, isMuted={}, targets={}, error={}",
                        chatRoomId, isMuted, batchTokens.size(), e.getMessage(), e);
            } finally {
                fcmMetrics.recordBatch("CHAT", batchTokens.size(), batchSuccess, batchFailure, System.nanoTime() - startNanos);
            }
        }
    }

    private MulticastMessage createChatMessage(List<String> tokens, String title, String body, Long chatRoomId, boolean isMuted) {
        String roomIdStr = String.valueOf(chatRoomId);
        
        AndroidNotification.Builder androidNotiBuilder = AndroidNotification.builder()
                .setTag("room_" + roomIdStr);
                
        Aps.Builder apsBuilder = Aps.builder()
                .setThreadId("room_" + roomIdStr);
                
        if (isMuted) {
            // Android: 무음용 채널 (앱에서 조용히 노출하도록 알림 중요도 낮춤)
            androidNotiBuilder.setChannelId("chat_channel_muted");
        } else {
            // Android: 소리/진동용 기본 채널
            androidNotiBuilder.setChannelId("chat_channel_default");
            // iOS: 소리가 나도록 default 설정
            apsBuilder.setSound("default");
        }

        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                // 웹뷰에서 라우팅할 때 참조할 공통 데이터 페이로드
                .putData("type", "CHAT")
                .putData("chatRoomId", roomIdStr)
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(androidNotiBuilder.build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(apsBuilder.build())
                        .build())
                .build();
    }


    private FcmMessage saveTrackedMessage(String title, String body, boolean adminMessage, int targetCount, Long targetId) {
        FcmMessage fcmMessage = FcmMessage.builder()
                .title(title)
                .body(body)
                .isAdminMessage(adminMessage)
                .targetId(targetId)
                .build();
        fcmMessage.markPending(targetCount);
        return fcmMessageRepository.saveAndFlush(fcmMessage);
    }

    private NotificationTargets getAdminNotificationTargets(AdminNotificationRequest request) {
        AdminNotificationTargetType targetType = request.resolveTargetType();
        List<FcmToken> fcmTokens = switch (targetType) {
            case ALL -> fcmTokenRepository.findAllTokens();
            case LOGGED_IN -> fcmTokenRepository.findAllByMemberIdIsNotNull();
            case LOGGED_OUT -> fcmTokenRepository.findAllByMemberIdIsNull();
            case MEMBERS -> getMemberTargetTokens(request.memberIds());
            case STUDENT_IDS -> getStudentIdTargetTokens(request.studentIds());
            case DEPARTMENTS -> getDepartmentTargetTokens(request.departments());
        };

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getToken,
                        fcmToken -> fcmToken.getMemberId() == null ? UNLINKED_MEMBER_ID : fcmToken.getMemberId(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        List<Long> targetMemberIds = switch (targetType) {
            case ALL -> memberRepository.findAllIds();
            case LOGGED_IN -> memberRepository.findIdsWithLinkedFcmToken();
            case LOGGED_OUT -> memberRepository.findIdsWithoutLinkedFcmToken();
            case MEMBERS -> getExistingMemberIds(request.memberIds());
            case STUDENT_IDS -> getStudentIdTargetMemberIds(request.studentIds());
            case DEPARTMENTS -> getDepartmentTargetMemberIds(request.departments());
        };

        return new NotificationTargets(
                tokenAndMemberId,
                distinctMemberIds(targetMemberIds)
        );
    }

    private List<FcmToken> getMemberTargetTokens(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
    }

    private List<Long> getExistingMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        return memberRepository.findAllById(memberIds).stream()
                .map(Member::getId)
                .toList();
    }

    private List<Long> getDepartmentTargetMemberIds(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }
        return memberRepository.findIdsByDepartmentIn(departments);
    }

    private List<Long> getStudentIdTargetMemberIds(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return memberRepository.findIdsByStudentIdIn(studentIds);
    }

    private List<FcmToken> getStudentIdTargetTokens(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = getStudentIdTargetMemberIds(studentIds);
        if (memberIds.isEmpty()) {
            return List.of();
        }

        return fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
    }

    private List<FcmToken> getDepartmentTargetTokens(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = getDepartmentTargetMemberIds(departments);
        if (memberIds.isEmpty()) {
            return List.of();
        }

        return fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
    }

    private List<Long> distinctMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        return memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record DeliveryResult(
            int successCount,
            int failureCount
    ) {
    }

    private record NotificationTargets(
            Map<String, Long> tokenAndMemberId,
            List<Long> targetMemberIds
    ) {
    }

    public record TrackedNotificationDispatch(
            Long fcmMessageId,
            Map<String, Long> tokenAndMemberId,
            String title,
            String body,
            FcmMessageType type,
            Long targetId
    ) {
    }
}
