package kr.inuappcenterportal.inuportal.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenProvider {

    private static final long ACCESS_TOKEN_VALID_DAYS = 1L;
    private static final long REFRESH_TOKEN_VALID_MONTHS = 6L;

    private final UserDetailsService userDetailsService;

    @Value("${jwtSecret}")
    private String secret;

    @Value("${refreshSecret}")
    private String refreshSecret;

    private Key secretKey;
    private Key refreshKey;

    @PostConstruct
    protected void init() {
        log.info("키 생성 암호화 키: {}", secret);
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        log.info("키 생성 완료 - secretKey 객체 생성됨");
    }

    public String createToken(String id, List<String> roles, LocalDateTime localDateTime) {
        Claims claims = Jwts.claims().setSubject(id);
        claims.put("roles", roles);

        Date now = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date accessExpiredTime = Date.from(getAccessTokenExpiry(localDateTime)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(accessExpiredTime)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String id, LocalDateTime localDateTime) {
        Claims claims = Jwts.claims().setSubject(id);

        Date now = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date refreshExpiredTime = Date.from(getRefreshTokenExpiry(localDateTime)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(refreshExpiredTime)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public LocalDateTime getAccessTokenExpiry(LocalDateTime issuedAt) {
        return issuedAt.plusDays(ACCESS_TOKEN_VALID_DAYS);
    }

    public LocalDateTime getRefreshTokenExpiry(LocalDateTime issuedAt) {
        return issuedAt.plusMonths(REFRESH_TOKEN_VALID_MONTHS);
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUsername(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getUsernameByRefresh(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        return request.getHeader("Auth");
    }

    public boolean validateToken(String token) {
        return valid(secretKey, token);
    }

    public boolean validateRefreshToken(String token) {
        return valid(refreshKey, token);
    }

    private boolean valid(Key key, String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (SignatureException ex) {
            throw new MyException(MyErrorCode.WRONG_TYPE_TOKEN);
        } catch (MalformedJwtException ex) {
            throw new MyException(MyErrorCode.UNSUPPORTED_TOKEN);
        } catch (ExpiredJwtException ex) {
            throw new MyException(MyErrorCode.EXPIRED_TOKEN);
        } catch (IllegalArgumentException ex) {
            throw new MyException(MyErrorCode.UNKNOWN_TOKEN_ERROR);
        }
    }
}
