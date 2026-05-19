package kr.inuappcenterportal.inuportal.global.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Optional;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public RedisLockProvider redisLockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "inu-portal");
    }

    @Bean
    @Primary
    public LockProvider meteredLockProvider(RedisLockProvider delegate, MeterRegistry registry) {
        return new LockProvider() {
            @Override
            public Optional<SimpleLock> lock(LockConfiguration config) {
                Optional<SimpleLock> result = delegate.lock(config);
                if (result.isEmpty()) {
                    Counter.builder("intip_scheduler_executions_total")
                            .tag("name", config.getName())
                            .tag("result", "skipped")
                            .register(registry)
                            .increment();
                }
                return result;
            }
        };
    }
}
