package kr.inuappcenterportal.inuportal.global.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 로컬 환경(app.local-auth.enabled=true)에서 firebase_key.json 없이 부팅하기 위한 설정.
 * 더미 credential로 FirebaseApp을 초기화하여 FirebaseMessaging 빈 생성만 보장하며,
 * 실제 푸시 발송은 인증 실패로 동작하지 않는다(발송 호출부는 예외를 흡수함).
 */
@Configuration
@Slf4j
@Profile("!test")
@ConditionalOnProperty(name = "app.local-auth.enabled", havingValue = "true")
public class LocalFirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.create(new AccessToken("local-dummy-token", null)))
                    .setProjectId("local-dummy")
                    .build();

            FirebaseApp.initializeApp(options);
            log.warn("로컬 환경: firebase_key.json 없이 더미 FirebaseApp 초기화 — 실제 푸시 발송은 비활성화됨");
        }
        return FirebaseMessaging.getInstance();
    }
}
