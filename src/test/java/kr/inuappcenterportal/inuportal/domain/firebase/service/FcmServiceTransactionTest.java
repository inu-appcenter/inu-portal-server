package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FcmServiceTransactionTest {

    @Test
    void dailyBriefNotification_usesIndependentWriteTransaction() throws NoSuchMethodException {
        Method method = FcmService.class.getMethod(
                "sendDailyBriefNotification",
                Long.class,
                String.class,
                String.class,
                FcmMessageType.class,
                String.class
        );

        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(transactional.readOnly()).isFalse();
    }
}
