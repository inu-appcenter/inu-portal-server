package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageFailedTargetRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 실패한 관리자 알림의 수동 재시도.
 *
 * <p><b>대상은 실패자로 한정한다.</b> 원래 대상 전원에게 재발송하면 이미 받은 사람이 같은 알림을
 * 두 번 받는다. 2026-09-07 장애(3,296건 중 1,149건 성공)가 정확히 그런 부분 실패였기 때문에,
 * 발송 시점에 남겨 둔 {@code fcm_message_failed_target}을 대상으로 삼는다.
 *
 * <p><b>알림함 행은 다시 만들지 않는다.</b> 최초 발송 때 {@code member_fcm_message}가 이미
 * 생성돼 있고 (fcm_message_id, member_id) 유일 제약이 걸려 있다. 재시도는 푸시만 다시 쏘는 것이지
 * 새 알림이 아니므로, 같은 {@code fcmMessageId}를 그대로 쓴다. 사용자 알림함에 중복 항목이
 * 생기지 않는 이유다.
 *
 * <p><b>토큰은 저장해 두지 않고 지금 다시 조회한다.</b> 실패 사유가 만료·해지된 토큰인 경우가
 * 많아서, 그때 실패한 토큰으로 재발송하면 같은 이유로 또 실패한다. 회원이 그 사이 앱을 다시 깔았다면
 * 새 토큰으로 전달된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmRetryService {

    private final FcmMessageRepository fcmMessageRepository;
    private final FcmMessageFailedTargetRepository fcmMessageFailedTargetRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmAsyncService fcmAsyncService;

    /**
     * 재시도를 접수한다. 실제 발송은 비동기로 진행되며, 이 메서드는 선점에 성공한 시점의
     * 알림 상태를 돌려준다.
     *
     * <p>선점({@code leaseForRetry})은 조건부 UPDATE라, 관리자가 버튼을 연타하거나 두 명이
     * 동시에 눌러도 정확히 한 번만 통과한다. 통과하지 못하면 이미 발송 중이거나 재시도할 수 없는
     * 상태이므로 409로 거절한다.
     */
    @Transactional
    public AdminNotificationResponse retry(Long fcmMessageId) {
        FcmMessage fcmMessage = fcmMessageRepository.findByIdAndAdminMessageTrue(fcmMessageId)
                .orElseThrow(() -> new MyException(MyErrorCode.FCM_MESSAGE_NOT_FOUND));

        List<Long> failedMemberIds = fcmMessageFailedTargetRepository.findMemberIdsByFcmMessageId(fcmMessageId);
        if (failedMemberIds.isEmpty()) {
            // 기능 도입 이전에 발송된 건도 여기로 온다. 기록이 없으면 누가 실패했는지 알 수 없고,
            // 전원 재발송은 중복 푸시가 되므로 재시도하지 않는다.
            throw new MyException(MyErrorCode.FCM_RETRY_NO_TARGET);
        }

        Map<String, Long> tokenAndMemberId = resolveCurrentTokens(failedMemberIds);
        if (tokenAndMemberId.isEmpty()) {
            // 실패자는 있는데 살아 있는 토큰이 하나도 없다 (전원 로그아웃/앱 삭제).
            throw new MyException(MyErrorCode.FCM_RETRY_NO_TARGET);
        }

        if (fcmMessageRepository.leaseForRetry(fcmMessageId, LocalDateTime.now()) != 1) {
            throw new MyException(MyErrorCode.FCM_RETRY_NOT_ALLOWED);
        }

        FcmMessageType type = resolveType(fcmMessageId);

        log.warn("Admin notification retry accepted: fcmMessageId={}, failedMembers={}, tokens={}, previousSendCount={}",
                fcmMessageId, failedMemberIds.size(), tokenAndMemberId.size(), fcmMessage.getSendCount());

        fcmAsyncService.retryAsync(
                fcmMessageId,
                tokenAndMemberId,
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                type,
                fcmMessage.getTargetId(),
                fcmMessage.getPath(),
                fcmMessage.getSendCount()
        );

        // leaseForRetry가 벌크 UPDATE라 영속성 컨텍스트를 비운 뒤 다시 읽어 최신 상태를 돌려준다.
        FcmMessage leased = fcmMessageRepository.findById(fcmMessageId).orElse(fcmMessage);
        return AdminNotificationResponse.of(leased, tokenAndMemberId.size());
    }

    /**
     * 실패 회원들의 <b>현재</b> 토큰을 조회한다. 같은 토큰 문자열이 여러 회원에 물려 있을 수
     * 없지만(unique), 방어적으로 먼저 온 매핑을 유지한다.
     */
    private Map<String, Long> resolveCurrentTokens(List<Long> memberIds) {
        List<FcmToken> tokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        for (FcmToken token : tokens) {
            if (token.getMemberId() == null) {
                continue;
            }
            tokenAndMemberId.putIfAbsent(token.getToken(), token.getMemberId());
        }
        return tokenAndMemberId;
    }

    /**
     * 알림함 행에 기록된 타입을 그대로 쓴다. 타입이 하나로 특정되지 않으면 임의로 골라 잘못된
     * 화면으로 라우팅하는 대신 GENERAL로 보낸다(관리자 알림은 전부 GENERAL로 저장된다).
     */
    private FcmMessageType resolveType(Long fcmMessageId) {
        List<FcmMessageType> types = memberFcmMessageRepository.findDistinctTypesByFcmMessageId(fcmMessageId);
        if (types.size() == 1 && types.get(0) != null) {
            return types.get(0);
        }
        log.warn("Retry target has ambiguous inbox type, falling back to GENERAL: fcmMessageId={}, types={}",
                fcmMessageId, types);
        return FcmMessageType.GENERAL;
    }
}
