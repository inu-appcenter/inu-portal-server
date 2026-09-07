package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageFailedTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 알림별 "전달 실패 회원" 집합을 관리한다.
 *
 * <p>발송 루프는 트랜잭션 밖에서 오래 돌기 때문에, 다른 집계 갱신과 마찬가지로
 * {@code REQUIRES_NEW}로 짧게 끊어 커밋한다. 발송 실패가 이 기록 실패로 번져
 * 알림 발송 자체를 되돌리는 일이 없어야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmFailedTargetService {

    private final FcmMessageFailedTargetRepository fcmMessageFailedTargetRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 이 알림의 실패 회원 집합을 통째로 교체한다.
     *
     * <p>증분이 아니라 교체인 이유: 재시도에 성공한 회원은 더 이상 실패자가 아니므로 반드시
     * 빠져야 한다. 남겨 두면 다음 재시도에서 이미 받은 사람에게 중복 푸시가 나간다.
     * 교체로 두면 최초 발송이든 N번째 재시도든 같은 코드가 항상 "현재 실패자"만 남긴다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceFailedTargets(Long fcmMessageId, Set<Long> failedMemberIds) {
        try {
            fcmMessageFailedTargetRepository.deleteByFcmMessageId(fcmMessageId);
            if (failedMemberIds.isEmpty()) {
                return;
            }
            batchInsert(fcmMessageId, failedMemberIds);
        } catch (Exception e) {
            // 이 기록은 재시도 편의를 위한 부가 정보다. 실패해도 발송 결과 집계를 되돌리지 않는다.
            log.error("Failed to record FCM failed targets: fcmMessageId={}, count={}, error={}",
                    fcmMessageId, failedMemberIds.size(), e.getMessage(), e);
        }
    }

    private void batchInsert(Long fcmMessageId, Collection<Long> memberIds) {
        String sql = "INSERT INTO fcm_message_failed_target " +
                "(fcm_message_id, member_id, create_date, modified_date) VALUES (?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = List.copyOf(memberIds);

        jdbcTemplate.batchUpdate(sql, ids, 500, (PreparedStatement ps, Long memberId) -> {
            ps.setLong(1, fcmMessageId);
            ps.setLong(2, memberId);
            ps.setObject(3, now);
            ps.setObject(4, now);
        });
    }
}
