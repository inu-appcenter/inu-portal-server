package kr.inuappcenterportal.inuportal.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
@Profile("!test")
@ConditionalOnProperty(name = "app.local-auth.enabled", havingValue = "false", matchIfMissing = true)
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount =
                    new ClassPathResource("firebase_key.json").getInputStream();

            // 2026-09-07 장애에서는 커넥션 수립이 5초 안에 끝나지 않아 6,500건이 타임아웃 났다.
            // 근본 원인은 동시 커넥션 폭주였고 그건 FcmDispatchGate가 막는다. 여기 상향은
            // 정상 부하에서 일시적으로 응답이 느려질 때 곧바로 포기하지 않게 하는 여유분일 뿐이다.
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setConnectTimeout(10000)
                    .setReadTimeout(30000)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("파이어베이스 연결 성공");
        }
        return FirebaseMessaging.getInstance();
    }
}
