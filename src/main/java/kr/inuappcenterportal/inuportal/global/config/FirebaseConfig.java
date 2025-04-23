package kr.inuappcenterportal.inuportal.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
@Slf4j
public class FirebaseConfig {
    @Value("${firebaseKeyPath}")
    private String path;
    @PostConstruct
    public void init(){
        try{
            FileInputStream serviceAccount =
                    new FileInputStream("C:\\Users\\hen71\\Desktop\\inu-portal-server\\src\\main\\resources\\firebase_key.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("파이어베이스 연결 성공");
        }catch (Exception e){
            log.warn("파이어베이스 연결 실패 : {}",e.getMessage());
        }
    }
}
