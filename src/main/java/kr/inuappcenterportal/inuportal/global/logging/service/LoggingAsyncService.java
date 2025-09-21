package kr.inuappcenterportal.inuportal.global.logging.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoggingAsyncService {

    private final LoggingService loggingService;

    @Async("loggingExecutor")
    public void asyncSaveLog(String memberId, String httpMethod, String uri, long duration) {
        loggingService.saveLog(memberId, httpMethod, uri, duration);
    }
}
