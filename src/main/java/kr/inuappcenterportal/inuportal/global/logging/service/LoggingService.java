package kr.inuappcenterportal.inuportal.global.logging.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.logging.domain.*;
import kr.inuappcenterportal.inuportal.global.logging.dto.req.ApiLoggingRequest;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingMemberResponse;
import kr.inuappcenterportal.inuportal.global.logging.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingService {

    private final LoggingRepository loggingRepository;
    private final SummaryMemberLogRepository summaryMemberLogRepository;
    private final SummaryMemberLogItemRepository summaryMemberLogItemRepository;
    private final SummaryApiLogRepository summaryApiLogRepository;
    private final SummaryApiLogItemRepository summaryApiLogItemRepository;

    @Transactional
    public void saveLog(String memberId, String httpMethod, String uri, long duration) {
        loggingRepository.save(Logging.createLog(memberId, httpMethod, uri, duration));
    }

    @Transactional(readOnly = true)
    public LoggingMemberResponse getMemberLogsByDate(LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            return getMemberLogResponseByDate(date);
        } else {
            SummaryMemberLog summaryMemberLog = summaryMemberLogRepository.findByDate(date)
                    .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_LOG));
            List<String> memberIds =
                    summaryMemberLogItemRepository.findAllBySummaryMemberLogId(summaryMemberLog.getId());

            return LoggingMemberResponse.of(summaryMemberLog.getMemberCount(), memberIds);
        }
    }

    @Transactional(readOnly = true)
    public List<LoggingApiResponse> getAPILogsByDate(LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            return getAPILogResponseByDate(date);
        } else {
            Long summaryApiLogId = summaryApiLogRepository.findIdByDate(date)
                    .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_LOG));
            List<SummaryApiLogItem> summaryApiLogItems =
                    summaryApiLogItemRepository.findAllBySummaryApiLogId(summaryApiLogId);

            return summaryApiLogItems.stream().map(summaryApiLogItem ->
                    LoggingApiResponse.of(summaryApiLogItem.getMethod(), summaryApiLogItem.getUri(), summaryApiLogItem.getApiCount())
            ).collect(Collectors.toList());
        }
    }

    // 매일 새벽 4시에 실행
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void summarizeDailyLogs() {
        LocalDate oneDayAgo = LocalDate.now().minusDays(1);

        saveSummaryMemberLog(oneDayAgo);
        saveSummaryApiLog(oneDayAgo);
        deleteLogsByDate(oneDayAgo);
    }

    @Transactional
    public void saveSummaryMemberLog(LocalDate oneDayAgo) {
        LoggingMemberResponse loggingMemberResponse = getMemberLogResponseByDate(oneDayAgo);

        SummaryMemberLog summaryMemberLog = summaryMemberLogRepository.save(
                SummaryMemberLog.of(loggingMemberResponse.memberCount(), oneDayAgo));

        List<SummaryMemberLogItem> summaryMemberLogItems = loggingMemberResponse.memberIds().stream()
                .map(memberId -> SummaryMemberLogItem.of(summaryMemberLog.getId(), memberId))
                .collect(Collectors.toList());
        summaryMemberLogItemRepository.saveAll(summaryMemberLogItems);

        log.info("회원 로그 경량화 완료. 기준 날짜: {}", oneDayAgo);
    }

    @Transactional
    public void saveSummaryApiLog(LocalDate oneDayAgo) {
        List<LoggingApiResponse> loggingApiResponses = getAPILogResponseByDate(oneDayAgo);
        if (loggingApiResponses == null) return;

        SummaryApiLog summaryApiLog = summaryApiLogRepository.save(SummaryApiLog.from(oneDayAgo));

        List<SummaryApiLogItem> summaryApiLogItems = loggingApiResponses.stream()
                .map(apiLog -> SummaryApiLogItem.of(summaryApiLog.getId(), apiLog.apiCount(), apiLog.method(), apiLog.uri()))
                .collect(Collectors.toList());
        summaryApiLogItemRepository.saveAll(summaryApiLogItems);

        log.info("API 로그 경량화 완료. 기준 날짜: {}", oneDayAgo);
    }

    @Transactional
    public void deleteLogsByDate(LocalDate oneDayAgo) {
        int deletedCount = 0;
        int batchDeleted;

        do {
            batchDeleted = deleteNextBatchOfOldLogs(oneDayAgo);
            deletedCount += batchDeleted;
        } while (batchDeleted > 0);

        log.info("총 {}건의 로그 삭제 완료.", deletedCount);
    }

    // 트랜잭션 분리
    @Transactional
    public int deleteNextBatchOfOldLogs(LocalDate cutoffDate) {
        int BATCH_SIZE = 1000;

        List<Logging> logsToDelete =
                loggingRepository.findAllByCreateDate(cutoffDate, PageRequest.of(0, BATCH_SIZE));

        if (logsToDelete.isEmpty()) {
            return 0;
        }

        loggingRepository.deleteAllInBatch(logsToDelete);
        loggingRepository.flush();

        return logsToDelete.size();
    }

    @Transactional
    public String saveApiLogs(ApiLoggingRequest apiLoggingRequest, Member member, HttpServletRequest request) {
        String memberId = getClientId(member, request);
        String httpMethod = "CUSTOM";

        return loggingRepository.save(
                Logging.createLog(memberId, httpMethod, apiLoggingRequest.uri(), -1)
        ).getUri();
    }

    private LoggingMemberResponse getMemberLogResponseByDate(LocalDate date) {
        List<String> memberIds = loggingRepository.findDistinctMemberIdsByCreateDate(date);
        Integer memberCount = memberIds.size();

        return LoggingMemberResponse.of(memberCount, memberIds);
    }

    private List<LoggingApiResponse> getAPILogResponseByDate(LocalDate date) {
        return loggingRepository.findApILogsByCreateDate(date, EXCLUDED_URIS, PageRequest.of(0, 20));
    }

    private String getClientId(Member member, HttpServletRequest request) {
        if (member != null) {
            return member.getId().toString();
        } else {
            return request.getHeader("X-Forwarded-For");
        }
    }

    private static final List<String> EXCLUDED_URIS = List.of(
            "/v3/api-docs/swagger-config",
            "/v3/api-docs",
            "/api/logs/members",
            "/api/logs/apis"
    );
}
