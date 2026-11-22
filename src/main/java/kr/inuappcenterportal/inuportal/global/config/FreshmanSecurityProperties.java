package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 신입생 인증(Firebase App Check 및 검증 해시) 보안 설정.
 */
@ConfigurationProperties(prefix = "app.freshman")
public record FreshmanSecurityProperties(
        boolean appCheckEnabled,
        String firebaseProjectNumber,
        String verifySecret,
        String jwksUri
) {
    public static final String DEFAULT_JWKS_URI = "https://firebaseappcheck.googleapis.com/v1/jwks";
    private static final String DEV_FALLBACK_SECRET = "intip-dev-freshman-verify-secret-change-me";

    public String resolvedFirebaseProjectNumber() {
        return firebaseProjectNumber == null ? "" : firebaseProjectNumber.trim();
    }

    public String resolvedVerifySecret() {
        return (verifySecret != null && !verifySecret.isBlank())
                ? verifySecret.trim()
                : DEV_FALLBACK_SECRET;
    }

    public String resolvedJwksUri() {
        return (jwksUri != null && !jwksUri.isBlank())
                ? jwksUri.trim()
                : DEFAULT_JWKS_URI;
    }
}
