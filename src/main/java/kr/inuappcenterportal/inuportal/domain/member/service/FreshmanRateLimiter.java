package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 신입생 로그인/가입 요청의 브루트포스 및 대량 허위 계정 생성을 Redis 카운터로 제한한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreshmanRateLimiter {

    private static final int MAX_ATTEMPTS_PER_IP = 10;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);

    private static final int MAX_SIGNUPS_PER_IP = 3;
    private static final Duration SIGNUP_WINDOW = Duration.ofHours(1);

    private static final int MAX_MISMATCH_PER_EXAM = 5;
    private static final Duration EXAM_FAIL_WINDOW = Duration.ofMinutes(10);
    private static final Duration EXAM_LOCK_DURATION = Duration.ofMinutes(30);

    private static final String KEY_IP_ATTEMPT = "freshman:rate:ip:";
    private static final String KEY_IP_SIGNUP = "freshman:rate:signup-ip:";
    private static final String KEY_EXAM_FAIL = "freshman:fail:exam:";
    private static final String KEY_EXAM_LOCK = "freshman:lock:exam:";

    private final StringRedisTemplate redisTemplate;

    public void checkAttempt(String clientIp, String examNo) {
        String ipKey = KEY_IP_ATTEMPT + sanitizeKeyPart(clientIp);
        Long attempts = incrementWithWindow(ipKey, ATTEMPT_WINDOW);
        if (attempts != null && attempts > MAX_ATTEMPTS_PER_IP) {
            log.warn("[FreshmanRateLimit] IP 시도 횟수 초과: {}", clientIp);
            throw new MyException(MyErrorCode.FRESHMAN_RATE_LIMITED);
        }

        if (examNo != null && !examNo.isBlank()) {
            String lockKey = KEY_EXAM_LOCK + sanitizeKeyPart(examNo);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                log.warn("[FreshmanRateLimit] 수험번호 잠금 상태 차단");
                throw new MyException(MyErrorCode.FRESHMAN_RATE_LIMITED);
            }
        }
    }

    public void checkSignupLimit(String clientIp) {
        String signupKey = KEY_IP_SIGNUP + sanitizeKeyPart(clientIp);
        Long signups = incrementWithWindow(signupKey, SIGNUP_WINDOW);
        if (signups != null && signups > MAX_SIGNUPS_PER_IP) {
            log.warn("[FreshmanRateLimit] IP 신규 신입생 가입 한도 초과: {}", clientIp);
            throw new MyException(MyErrorCode.FRESHMAN_RATE_LIMITED);
        }
    }

    public void recordExamFailure(String examNo) {
        if (examNo == null || examNo.isBlank()) {
            return;
        }
        String sanitized = sanitizeKeyPart(examNo);
        Long fails = incrementWithWindow(KEY_EXAM_FAIL + sanitized, EXAM_FAIL_WINDOW);
        if (fails != null && fails >= MAX_MISMATCH_PER_EXAM) {
            redisTemplate.opsForValue().set(KEY_EXAM_LOCK + sanitized, "1", EXAM_LOCK_DURATION);
        }
    }

    public void clearExamFailure(String examNo) {
        if (examNo == null || examNo.isBlank()) {
            return;
        }
        redisTemplate.delete(KEY_EXAM_FAIL + sanitizeKeyPart(examNo));
    }

    private Long incrementWithWindow(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count;
    }

    private static String sanitizeKeyPart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
