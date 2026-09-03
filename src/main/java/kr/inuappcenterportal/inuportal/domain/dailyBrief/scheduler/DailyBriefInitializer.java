package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefInitializer {

    private final DailyBriefService dailyBriefService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        try {
            log.info("[Daily Brief] 서버 기동 시 기존 회원 기본 알림 설정 백필 확인 시작");
            dailyBriefService.backfillDefaultSettings();
        } catch (Exception e) {
            log.warn("[Daily Brief] 기본 알림 설정 백필 중 오류 발생 (무시하고 기동 계속): {}", e.getMessage(), e);
        }
    }
}
