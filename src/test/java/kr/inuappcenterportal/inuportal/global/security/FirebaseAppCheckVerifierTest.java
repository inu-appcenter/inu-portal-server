package kr.inuappcenterportal.inuportal.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kr.inuappcenterportal.inuportal.global.config.FreshmanSecurityProperties;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseAppCheckVerifierTest {

    private static final String PROJECT_NUMBER = "123456789012";
    private static final String KID = "kid-1";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private KeyPair keyPair;
    private String jwksJson;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray());
        jwksJson = """
                {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"%s","n":"%s","e":"%s"}]}
                """.formatted(KID, n, e);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private String createToken(String kid, String issuer, Object aud, Date exp, String jti) {
        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setIssuer(issuer)
                .claim("aud", aud)
                .setId(jti)
                .setExpiration(exp)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    @Test
    @DisplayName("유효한 Firebase App Check 토큰은 JWKS 서명과 클레임을 통과하고 1회 소비된다")
    void validTokenPassesAndConsumedOnce() {
        AtomicInteger fetchCount = new AtomicInteger();
        FreshmanSecurityProperties props = new FreshmanSecurityProperties(true, PROJECT_NUMBER, "secret", null);
        FirebaseAppCheckVerifier verifier = new FirebaseAppCheckVerifier(props, redisTemplate, objectMapper, url -> {
            fetchCount.incrementAndGet();
            return jwksJson;
        });

        when(valueOperations.setIfAbsent(eq("appcheck:used:jti-1"), eq("1"), any(Duration.class))).thenReturn(true);
        String token = createToken(
                KID,
                "https://firebaseappcheck.googleapis.com/" + PROJECT_NUMBER,
                List.of("projects/" + PROJECT_NUMBER),
                new Date(System.currentTimeMillis() + 300_000),
                "jti-1"
        );

        assertDoesNotThrow(() -> verifier.verify(token));
        assertEquals(1, fetchCount.get());
        verify(valueOperations).setIfAbsent(eq("appcheck:used:jti-1"), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("같은 1회용 토큰을 재사용하면 Replay 차단으로 401 예외가 발생한다")
    void replayedTokenRejected() {
        FreshmanSecurityProperties props = new FreshmanSecurityProperties(true, PROJECT_NUMBER, "secret", null);
        FirebaseAppCheckVerifier verifier = new FirebaseAppCheckVerifier(props, redisTemplate, objectMapper, url -> jwksJson);

        when(valueOperations.setIfAbsent(eq("appcheck:used:jti-replay"), eq("1"), any(Duration.class)))
                .thenReturn(true)
                .thenReturn(false);
        String token = createToken(
                KID,
                "https://firebaseappcheck.googleapis.com/" + PROJECT_NUMBER,
                List.of("projects/" + PROJECT_NUMBER),
                new Date(System.currentTimeMillis() + 300_000),
                "jti-replay"
        );

        assertDoesNotThrow(() -> verifier.verify(token));
        MyException ex = assertThrows(MyException.class, () -> verifier.verify(token));
        assertEquals(MyErrorCode.INVALID_APP_CHECK_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("aud나 iss가 다르거나 토큰이 없으면 401 예외가 발생한다")
    void invalidClaimsOrMissingTokenRejected() {
        FreshmanSecurityProperties props = new FreshmanSecurityProperties(true, PROJECT_NUMBER, "secret", null);
        FirebaseAppCheckVerifier verifier = new FirebaseAppCheckVerifier(props, redisTemplate, objectMapper, url -> jwksJson);

        assertThrows(MyException.class, () -> verifier.verify(null));

        String wrongAud = createToken(
                KID,
                "https://firebaseappcheck.googleapis.com/" + PROJECT_NUMBER,
                List.of("projects/999999999"),
                new Date(System.currentTimeMillis() + 300_000),
                "jti-bad-aud"
        );
        assertEquals(MyErrorCode.INVALID_APP_CHECK_TOKEN,
                assertThrows(MyException.class, () -> verifier.verify(wrongAud)).getErrorCode());

        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any());
    }
}
