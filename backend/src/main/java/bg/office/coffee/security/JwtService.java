package bg.office.coffee.security;

import bg.office.coffee.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration sessionTtl;
    private final Duration rememberTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-hours}") long ttlHours,
                      @Value("${app.jwt.remember-days}") long rememberDays) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET трябва да е поне 32 байта (символа).");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.sessionTtl = Duration.ofHours(ttlHours);
        this.rememberTtl = Duration.ofDays(rememberDays);
    }

    /** remember=true издава дълготраен токен („Запомни ме“), иначе – за една работна сесия. */
    public String issue(AppUser user, boolean remember) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("tv", user.getTokenVersion())
                .claim("rem", remember)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(remember ? rememberTtl : sessionTtl)))
                .signWith(key)
                .compact();
    }

    /** Проверява подписа и срока. Хвърля JwtException при невалиден или изтекъл токен. */
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer version = claims.get("tv", Integer.class);
        Boolean remember = claims.get("rem", Boolean.class);
        return new TokenClaims(
                Long.valueOf(claims.getSubject()),
                version == null ? 0 : version,
                Boolean.TRUE.equals(remember));
    }
}