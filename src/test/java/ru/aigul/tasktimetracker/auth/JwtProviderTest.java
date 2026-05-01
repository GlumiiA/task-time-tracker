package ru.aigul.tasktimetracker.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final Instant NOW = Instant.parse("2030-05-01T10:00:00Z");
    private static final String SECRET = "change-this-secret-to-at-least-32-characters";

    @Test
    void generateAndParseAccessToken() {
        JwtProvider provider = provider("task-time-tracker", Duration.ofHours(1), SECRET);
        Employee employee = new Employee(7L, "Test User", "test.user", "password", Role.EMPLOYEE);

        String token = provider.generateAccessToken(employee);

        JwtPrincipal principal = provider.parseToken(token);

        assertThat(principal.getId()).isEqualTo(7L);
        assertThat(principal.getUsername()).isEqualTo("test.user");
        assertThat(principal.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void parseTokenSupportsStringIdClaim() {
        JwtProvider provider = provider("task-time-tracker", Duration.ofHours(1), SECRET);
        String token = token("task-time-tracker", "admin", "42", Role.ADMIN, NOW.plus(Duration.ofMinutes(30)), SECRET);

        JwtPrincipal principal = provider.parseToken(token);

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void parseTokenRejectsInvalidToken() {
        JwtProvider provider = provider("task-time-tracker", Duration.ofHours(1), SECRET);

        assertThatThrownBy(() -> provider.parseToken("not-a-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void parseTokenRejectsWrongIssuer() {
        JwtProvider provider = provider("task-time-tracker", Duration.ofHours(1), SECRET);
        String token = token("another-issuer", "user", 1L, Role.EMPLOYEE, NOW.plus(Duration.ofMinutes(30)), SECRET);

        assertThatThrownBy(() -> provider.parseToken(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid token issuer");
    }

    @Test
    void parseTokenRejectsExpiredToken() {
        JwtProvider provider = provider("task-time-tracker", Duration.ofHours(1), SECRET);
        String token = token("task-time-tracker", "user", 1L, Role.EMPLOYEE, NOW.minusSeconds(1), SECRET);

        assertThatThrownBy(() -> provider.parseToken(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Token expired");
    }

    private JwtProvider provider(String issuer, Duration ttl, String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer(issuer);
        properties.setAccessTokenTtl(ttl);
        properties.setSecret(secret);
        return new JwtProvider(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String token(String issuer, String subject, Object id, Role role, Instant expiresAt, String secret) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(NOW))
                .setExpiration(Date.from(expiresAt))
                .claim("id", id)
                .claim("role", role.name())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
