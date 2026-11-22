package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "messageExecutor")
    public Executor messageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("FCM-MSG-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 종료 시 진행 중/대기 중인 발송 작업을 마무리한 뒤 종료 (배포 중 알림 유실 방지)
        // 대기 30s는 컨테이너 stop_grace_period(45s)보다 짧게 둬서 SIGKILL 전에 드레인을 끝낸다
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "sendExecutor")
    public Executor sendExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("FCM-SEND-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 종료 시 진행 중/대기 중인 배치 발송을 마무리한 뒤 종료 (배포 중 알림 유실 방지)
        // 대기 30s는 컨테이너 stop_grace_period(45s)보다 짧게 둬서 SIGKILL 전에 드레인을 끝낸다
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "loggingExecutor")
    public Executor loggingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("LOGGING-");
        // 종료 시 버퍼링된 로그 적재 작업을 마무리한 뒤 종료
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(name = "crawlerExecutor")
    public Executor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("CRAWLER-");
        // 크롤링은 오래 걸리고 스케줄러가 재실행하므로, 종료 시 배포를 붙잡지 않고 즉시 중단
        executor.setWaitForTasksToCompleteOnShutdown(false);
        // Spring 6.1부터 executor가 lifecycle 종료 단계에서 실행 중인 작업을 기다리므로(phase timeout까지),
        // 종료 단계 대기에서 제외해 실행 중인 크롤링을 즉시 interrupt 한다
        executor.setAcceptTasksAfterContextClose(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationViewExecutor")
    public Executor notificationViewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("NOTI-VIEW-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
