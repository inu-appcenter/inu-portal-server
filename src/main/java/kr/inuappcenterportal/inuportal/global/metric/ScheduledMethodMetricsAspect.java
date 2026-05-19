package kr.inuappcenterportal.inuportal.global.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class ScheduledMethodMetricsAspect {

    private final MeterRegistry registry;

    public ScheduledMethodMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object recordScheduledExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String name = resolveName(joinPoint);
        long startNanos = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            Counter.builder("intip_scheduler_executions_total")
                    .tag("name", name)
                    .tag("result", "success")
                    .register(registry)
                    .increment();
            return result;
        } catch (Throwable e) {
            Counter.builder("intip_scheduler_executions_total")
                    .tag("name", name)
                    .tag("result", "failure")
                    .register(registry)
                    .increment();
            throw e;
        } finally {
            Timer.builder("intip_scheduler_duration_seconds")
                    .tag("name", name)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .register(registry)
                    .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    private String resolveName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        SchedulerLock lock = signature.getMethod().getAnnotation(SchedulerLock.class);
        if (lock != null && !lock.name().isBlank()) {
            return lock.name();
        }
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
