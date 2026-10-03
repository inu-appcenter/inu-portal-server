package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationReadStats;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 발송 로그별 읽음/클릭 집계를 읽는다 (#466).
 *
 * <p>관리자 목록/상세/재발송 응답이 모두 같은 집계를 실어야 해서 한 곳으로 모았다.
 * 어느 한 경로가 빠뜨리면 그 화면만 클릭율이 0으로 보인다.
 */
@Component
@RequiredArgsConstructor
public class NotificationReadStatsReader {

    private final MemberFcmMessageRepository memberFcmMessageRepository;

    public NotificationReadStats findOne(Long fcmMessageId) {
        return findAll(List.of(fcmMessageId)).getOrDefault(fcmMessageId, NotificationReadStats.empty());
    }

    /**
     * 알림함 행이 하나도 없는 발송(전송 실패 등)은 결과에 빠지므로 호출부가 빈 값으로 채운다.
     * 목록 API가 페이지당 8건이라 IN 목록은 사실상 한 자릿수로 묶인다.
     */
    public Map<Long, NotificationReadStats> findAll(List<Long> fcmMessageIds) {
        if (fcmMessageIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, NotificationReadStats> stats = new LinkedHashMap<>();
        for (Object[] row : memberFcmMessageRepository.aggregateReadStatsByFcmMessageIds(
                fcmMessageIds, NotificationReadSource.PUSH, NotificationReadSource.INBOX)) {
            stats.put((Long) row[0], new NotificationReadStats(
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).intValue()));
        }
        return stats;
    }
}
