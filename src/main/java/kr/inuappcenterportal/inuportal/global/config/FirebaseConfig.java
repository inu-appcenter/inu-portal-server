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

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setConnectTimeout(5000)
                    .setReadTimeout(15000)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("파이어베이스 연결 성공");
        }
        return FirebaseMessaging.getInstance();
    }
}
