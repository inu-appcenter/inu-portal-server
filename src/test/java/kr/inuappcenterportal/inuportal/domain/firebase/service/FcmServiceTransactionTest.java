package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler.DailyBriefScheduler;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 알림 발송의 트랜잭션 경계.
 *
 * <p>FCM 발송(외부 HTTP)은 트랜잭션 밖에서 해야 DB 커넥션을 붙잡지 않는다. 대신 발송 이력 쓰기는 호출자가
 * read-only 트랜잭션 안이어도 독립된 쓰기 트랜잭션(REQUIRES_NEW)이어야 한다 — 그래야 쓰기가 호출자
 * 트랜잭션을 rollback-only로 오염시키지 않고, 한 회원의 저장 실패가 다른 회원에게 번지지 않는다.
 */
class FcmServiceTransactionTest {

    @Test
    void memberNotifications_sendOutsideTransaction() throws NoSuchMethodException {
        Method dailyBrief = FcmService.class.getMethod("sendDailyBriefNotification",
                Long.class, String.class, String.class, FcmMessageType.class, String.class);
        Method preClass = FcmService.class.getMethod("sendPreClassNotification",
                Long.class, String.class, String.class, FcmMessageType.class, String.class, LiveActivityStartPush.class);

        assertThat(AnnotatedElementUtils.findMergedAnnotation(dailyBrief, Transactional.class)).isNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(preClass, Transactional.class)).isNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(FcmService.class, Transactional.class)).isNull();
    }

    @Test
    void memberNotificationHistory_usesIndependentWriteTransactions() throws NoSuchMethodException {
        Method create = FcmTransactionService.class.getMethod("createMemberNotification",
                String.class, String.class, Long.class, FcmMessageType.class);
        Method finish = FcmTransactionService.class.getMethod("updateFinalStatus",
                Long.class, int.class, int.class);

        for (Method method : new Method[]{create, finish}) {
            Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
            assertThat(transactional).as(method.getName()).isNotNull();
            assertThat(transactional.propagation()).as(method.getName()).isEqualTo(Propagation.REQUIRES_NEW);
            assertThat(transactional.readOnly()).as(method.getName()).isFalse();
        }
    }

    @Test
    void dailyBriefScheduler_sendsOutsideTransaction() {
        assertThat(AnnotatedElementUtils.findMergedAnnotation(DailyBriefScheduler.class, Transactional.class)).isNull();
        assertThat(Arrays.stream(DailyBriefScheduler.class.getDeclaredMethods())
                .filter(m -> AnnotatedElementUtils.findMergedAnnotation(m, Transactional.class) != null))
                .isEmpty();
    }
}
