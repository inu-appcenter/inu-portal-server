package kr.inuappcenterportal.inuportal.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import kr.inuappcenterportal.inuportal.global.config.FreshmanSecurityProperties;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Firebase App Check (Play Integrity / Apple App Attest) 토큰 검증기.
 * JWKS RS256 서명 및 iss/aud/exp를 검증하고, Redis SETNX로 1회용 토큰 재사용을 차단한다.
 */
@Slf4j
@Component
public class FirebaseAppCheckVerifier {

    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(6);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MIN_REPLAY_TTL = Duration.ofMinutes(1);
    private static final String ISSUER_PREFIX = "https://firebaseappcheck.googleapis.com/";
    private static final String REDIS_USED_PREFIX = "appcheck:used:";

    private final FreshmanSecurityProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Function<String, String> jwksFetcher;

    private volatile Map<String, PublicKey> cachedKeys = Map.of();
    private volatile Instant cacheExpiresAt = Instant.MIN;

    @Autowired
    public FirebaseAppCheckVerifier(
            FreshmanSecurityProperties properties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this(properties, redisTemplate, objectMapper, defaultHttpFetcher());
    }

    FirebaseAppCheckVerifier(
            FreshmanSecurityProperties properties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Function<String, String> jwksFetcher
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jwksFetcher = jwksFetcher;
    }

    private static Function<String, String> defaultHttpFetcher() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        return url -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(HTTP_TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("JWKS HTTP status: " + response.statusCode());
                }
                return response.body();
            } catch (Exception e) {
                throw new IllegalStateException("Firebase App Check JWKS 조회 실패", e);
            }
        };
    }

    /**
     * X-Firebase-AppCheck 토큰을 검증하고 1회용으로 소비한다.
     */
    public void verify(String appCheckToken) {
        if (!properties.appCheckEnabled()) {
            log.warn("[AppCheck] app.freshman.app-check-enabled=false 설정으로 App Check 검증을 건너뜁니다.");
            return;
        }
        if (appCheckToken == null || appCheckToken.isBlank()) {
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }
        String projectNumber = properties.resolvedFirebaseProjectNumber();
        if (projectNumber.isEmpty()) {
            log.error("[AppCheck] firebase-project-number가 설정되지 않았습니다.");
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }

        String trimmedToken = appCheckToken.trim();
        Jws<Claims> jws;
        try {
            jws = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public Key resolveSigningKey(JwsHeader header, Claims claims) {
                            if (!"RS256".equals(header.getAlgorithm())) {
                                throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
                            }
                            return resolvePublicKey(header.getKeyId());
                        }
                    })
                    .requireIssuer(ISSUER_PREFIX + projectNumber)
                    .build()
                    .parseClaimsJws(trimmedToken);
        } catch (MyException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[AppCheck] 토큰 서명/클레임 검증 실패: {}", e.getClass().getSimpleName());
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }

        Claims claims = jws.getBody();
        if (!matchesExpectedAudience(claims, "projects/" + projectNumber)) {
            log.warn("[AppCheck] audience 불일치");
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }

        consumeTokenOnce(trimmedToken, claims);
    }

    private PublicKey resolvePublicKey(String kid) {
        if (kid == null || kid.isBlank()) {
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }
        Map<String, PublicKey> keys = getOrRefreshKeys(false);
        PublicKey key = keys.get(kid);
        if (key == null) {
            // 키 로테이션 대응: 미지의 kid면 강제 1회 재조회
            keys = getOrRefreshKeys(true);
            key = keys.get(kid);
        }
        if (key == null) {
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }
        return key;
    }

    private synchronized Map<String, PublicKey> getOrRefreshKeys(boolean forceRefresh) {
        Instant now = Instant.now();
        if (!forceRefresh && !cachedKeys.isEmpty() && now.isBefore(cacheExpiresAt)) {
            return cachedKeys;
        }
        try {
            String body = jwksFetcher.apply(properties.resolvedJwksUri());
            JsonNode root = objectMapper.readTree(body);
            JsonNode keysNode = root.path("keys");
            if (!keysNode.isArray()) {
                throw new IllegalStateException("JWKS keys 필드가 배열이 아닙니다.");
            }
            Map<String, PublicKey> nextKeys = new ConcurrentHashMap<>();
            KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
            for (JsonNode keyNode : keysNode) {
                String kty = keyNode.path("kty").asText("");
                String kid = keyNode.path("kid").asText("");
                String n = keyNode.path("n").asText("");
                String e = keyNode.path("e").asText("");
                if (!"RSA".equals(kty) || kid.isEmpty() || n.isEmpty() || e.isEmpty()) {
                    continue;
                }
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                PublicKey publicKey = rsaFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
                nextKeys.put(kid, publicKey);
            }
            if (nextKeys.isEmpty()) {
                throw new IllegalStateException("유효한 RSA 공개키가 없습니다.");
            }
            cachedKeys = Map.copyOf(nextKeys);
            cacheExpiresAt = now.plus(JWKS_CACHE_TTL);
            return cachedKeys;
        } catch (Exception e) {
            log.error("[AppCheck] JWKS 파싱 실패", e);
            if (!cachedKeys.isEmpty()) {
                return cachedKeys;
            }
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }
    }

    private boolean matchesExpectedAudience(Claims claims, String expectedAud) {
        Object aud = claims.get("aud");
        if (aud instanceof String str) {
            return expectedAud.equals(str);
        }
        if (aud instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> expectedAud.equals(String.valueOf(item)));
        }
        return false;
    }

    private void consumeTokenOnce(String rawToken, Claims claims) {
        String jti = claims.getId();
        String uniqueKey = (jti != null && !jti.isBlank()) ? jti : sha256Hex(rawToken);
        Date expiration = claims.getExpiration();
        Duration ttl = expiration == null
                ? MIN_REPLAY_TTL
                : Duration.between(Instant.now(), expiration.toInstant());
        if (ttl.compareTo(MIN_REPLAY_TTL) < 0) {
            ttl = MIN_REPLAY_TTL;
        }

        Boolean firstUse = redisTemplate.opsForValue().setIfAbsent(REDIS_USED_PREFIX + uniqueKey, "1", ttl);
        if (!Boolean.TRUE.equals(firstUse)) {
            log.warn("[AppCheck] 이미 소비된 토큰 재전송 차단");
            throw new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 해시 계산 실패", e);
        }
    }
}
