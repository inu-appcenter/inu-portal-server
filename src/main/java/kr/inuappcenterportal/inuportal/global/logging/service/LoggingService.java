package kr.inuappcenterportal.inuportal.global.logging.service;

import kr.inuappcenterportal.inuportal.global.logging.domain.Logging;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingMemberResponse;
import kr.inuappcenterportal.inuportal.global.logging.repository.LoggingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingService {

    private final LoggingRepository loggingRepository;
    private final int BATCH_SIZE = 1000;

    @Transactional
    public void saveLog(String memberId, String httpMethod, String uri, long duration) {
        loggingRepository.save(Logging.createLog(memberId, httpMethod, uri, duration));
    }

    @Transactional(readOnly = true)
    public LoggingMemberResponse getMemberLogsByDate(LocalDate date) {
        List<String> memberIds = loggingRepository.findDistinctMemberIdsByCreateDate(date);
        Integer memberCount = memberIds.size();

        return LoggingMemberResponse.of(memberCount, memberIds);
    }

    @Transactional(readOnly = true)
    public List<LoggingApiResponse> getAPILogsByDate(LocalDate date) {
        return loggingRepository.findApILogsByCreateDate(date, PageRequest.of(0, 10));
    }

    // 매일 새벽 4시에 실행
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void deleteOldLogs() {
        LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
        log.info("일주일 전 로그 삭제 시작. 기준 날짜: {}", oneWeekAgo);

        int deletedCount = 0;
        int batchDeleted;

        do {
            batchDeleted = deleteNextBatchOfOldLogs(oneWeekAgo);
            deletedCount += batchDeleted;
        } while (batchDeleted > 0);

        log.info("총 {}건의 오래된 로그 삭제 완료.", deletedCount);
    }

    // 트랜잭션 분리
    @Transactional
    public int deleteNextBatchOfOldLogs(LocalDate cutoffDate) {
        List<Logging> logsToDelete =
                loggingRepository.findAllByCreateDateBefore(cutoffDate, PageRequest.of(0, BATCH_SIZE));

        if (logsToDelete.isEmpty()) {
            return 0;
        }

        loggingRepository.deleteAllInBatch(logsToDelete);
        loggingRepository.flush();

        return logsToDelete.size();
    }
}
