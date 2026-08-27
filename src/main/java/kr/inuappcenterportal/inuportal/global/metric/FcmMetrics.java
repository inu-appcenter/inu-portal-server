package kr.inuappcenterportal.inuportal.global.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FcmMetrics {

    private final MeterRegistry registry;

    public FcmMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordBatch(String type, int batchSize, int successCount, int failureCount, long durationNanos) {
        if (successCount > 0) {
            Counter.builder("intip_fcm_send_total")
                    .tag("type", type)
                    .tag("result", "success")
                    .register(registry)
                    .increment(successCount);
        }
        if (failureCount > 0) {
            Counter.builder("intip_fcm_send_total")
                    .tag("type", type)
                    .tag("result", "failure")
                    .register(registry)
                    .increment(failureCount);
        }

        DistributionSummary.builder("intip_fcm_batch_size")
                .tag("type", type)
                .publishPercentileHistogram()
                .register(registry)
                .record(batchSize);

        Timer.builder("intip_fcm_send_duration_seconds")
                .tag("type", type)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
