package kr.inuappcenterportal.inuportal.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class FcmTestAsyncConfig {

    @Bean(name = "messageExecutor")
    public TaskExecutor messageExecutor() {
        return new SyncTaskExecutor();
    }
}
