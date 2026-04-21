package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
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
        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, target.size());

        if (target.isEmpty()) {
            return;
        }

        MulticastMessage message = createMulticastMessage(target, title, body);
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            fcmMessage.updateDeliveryResult(response.getSuccessCount(), response.getFailureCount());
            log.info("Admin notification sent: target={}, success={}, failure={}",
                    target.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            fcmMessage.markFailed(target.size());
            log.warn("Admin notification send failed: {}", e.getMessage());
        } catch (Exception e) {
            fcmMessage.markFailed(target.size());
            log.error("Admin notification send failed unexpectedly: target={}, message={}",
                    target.size(), e.getMessage(), e);
        }
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToAll(String title, String body) {
        List<String> target = fcmTokenRepository.findAllStringTokens();
        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, target.size());

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
    public void sendKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, FcmMessageType fcmMessageType) {
        if (tokenAndMemberId.isEmpty()) {
            return;
        }

        FcmMessage fcmMessage = saveTrackedMessage(title, body, false, tokenAndMemberId.size());

        List<Long> targetMemberIds = tokenAndMemberId.values().stream()
                .filter(id -> id != null && !id.equals(UNLINKED_MEMBER_ID))
                .distinct()
                .toList();

        saveMemberFcmMessages(createMemberFcmMessages(fcmMessage.getId(), targetMemberIds, fcmMessageType));

        DeliveryResult deliveryResult = dispatchToMembers(tokenAndMemberId, title, body);
        fcmMessage.updateDeliveryResult(deliveryResult.successCount(), deliveryResult.failureCount());
    }

    @Transactional
    public AdminNotificationDispatch prepareAdminNotification(AdminNotificationRequest request) {
        NotificationTargets notificationTargets = getAdminNotificationTargets(request);
        FcmMessage fcmMessage = saveTrackedMessage(
                request.title(),
                request.content(),
                true,
                notificationTargets.tokenAndMemberId().size()
        );

        return new AdminNotificationDispatch(
                fcmMessage.getId(),
                request.title(),
                request.content(),
                Map.copyOf(notificationTargets.tokenAndMemberId()),
                List.copyOf(notificationTargets.targetMemberIds())
        );
    }

    @Transactional
    public void sendToMembers(AdminNotificationDispatch dispatch) {
        FcmMessage fcmMessage = fcmMessageRepository.findById(dispatch.fcmMessageId())
                .orElseThrow(() -> new MyException(MyErrorCode.MESSAGE_NOT_FOUND));

        saveMemberFcmMessages(createMemberFcmMessages(
                fcmMessage.getId(),
                dispatch.targetMemberIds(),
                FcmMessageType.GENERAL
        ));

        if (!dispatch.hasTarget()) {
            log.info("Admin member notification stored without push targets: memberTargets={}",
                    dispatch.memberTargetCount());
            return;
        }

        try {
            DeliveryResult deliveryResult = dispatchToMembers(
                    dispatch.tokenAndMemberId(),
                    dispatch.title(),
                    dispatch.content()
            );

            fcmMessage.updateDeliveryResult(deliveryResult.successCount(), deliveryResult.failureCount());

            log.info("Admin member notification finished: target={}, success={}, failure={}",
                    dispatch.targetCount(), deliveryResult.successCount(), deliveryResult.failureCount());
        } catch (Exception e) {
            fcmMessage.markFailed(dispatch.targetCount());
            log.error("Admin member notification failed: fcmMessageId={}, target={}, message={}",
                    dispatch.fcmMessageId(), dispatch.targetCount(), e.getMessage(), e);
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

    private MulticastMessage createMulticastMessage(List<String> tokens, String title, String body) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
    }

    private FcmMessage saveTrackedMessage(String title, String body, boolean adminMessage, int targetCount) {
        FcmMessage fcmMessage = FcmMessage.builder()
                .title(title)
                .body(body)
                .isAdminMessage(adminMessage)
                .build();
        fcmMessage.markPending(targetCount);
        return fcmMessageRepository.save(fcmMessage);
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

    private DeliveryResult dispatchToMembers(Map<String, Long> tokenAndMemberId, String title, String body) {
        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());
        int batchSize = 500;
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batchTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            MulticastMessage message = createMulticastMessage(batchTokens, title, body);

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse sendResponse = responses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String token = batchTokens.get(j);
                        FirebaseMessagingException exception = sendResponse.getException();
                        String errorMsg = exception != null ? exception.getMessage() : "unknown error";
                        log.warn("FCM send failed: token={}, error={}", token, errorMsg);
                    }
                }
            } catch (FirebaseMessagingException e) {
                failureCount += batchTokens.size();
                log.error("FCM batch send failed: batchSize={}, errorCode={}, message={}",
                        batchTokens.size(),
                        e.getMessagingErrorCode(),
                        e.getMessage(),
                        e);
            } catch (Exception e) {
                failureCount += batchTokens.size();
                log.error("FCM batch send failed unexpectedly: batchSize={}, message={}",
                        batchTokens.size(),
                        e.getMessage(),
                        e);
            }
        }

        return new DeliveryResult(successCount, failureCount);
    }

    private void saveMemberFcmMessages(Collection<MemberFcmMessage> memberFcmMessages) {
        if (memberFcmMessages.isEmpty()) {
            return;
        }

        List<MemberFcmMessage> messages = new ArrayList<>(memberFcmMessages);
        int batchSize = 500;
        for (int i = 0; i < messages.size(); i += batchSize) {
            List<MemberFcmMessage> batch = messages.subList(i, Math.min(i + batchSize, messages.size()));
            memberFcmMessageRepository.saveAll(batch);
            memberFcmMessageRepository.flush();
        }
    }

    private Collection<MemberFcmMessage> createMemberFcmMessages(Long fcmMessageId, List<Long> memberIds,
                                                                 FcmMessageType fcmMessageType) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        return distinctMemberIds(memberIds).stream()
                .map(memberId -> MemberFcmMessage.of(fcmMessageId, memberId, fcmMessageType))
                .toList();
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
}
