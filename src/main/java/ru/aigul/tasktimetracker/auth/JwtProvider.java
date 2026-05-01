package ru.aigul.tasktimetracker.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtProvider {
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ID = "id";

    private final JwtProperties properties;
    private final Clock clock;

    public JwtProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generateAccessToken(Employee employee) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        return Jwts.builder()
                .setSubject(employee.getUsername())
                .setIssuer(properties.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim(CLAIM_ID, employee.getId())
                .claim(CLAIM_ROLE, employee.getRole().name())
                .signWith(Keys.hmacShaKeyFor(getSecretBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtPrincipal parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(getSecretBytes()))
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();
            validateClaims(claims);

            Long id = parseId(claims);
            String username = claims.getSubject();
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new JwtPrincipal(id, username, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    private void validateClaims(Claims claims) {
        if (!properties.getIssuer().equals(claims.getIssuer())) {
            throw new UnauthorizedException("Invalid token issuer");
        }
        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.toInstant().isBefore(Instant.now(clock))) {
            throw new UnauthorizedException("Token expired");
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new UnauthorizedException("Invalid token subject");
        }
        if (claims.get(CLAIM_ROLE) == null || claims.get(CLAIM_ID) == null) {
            throw new UnauthorizedException("Invalid token claims");
        }
    }

    private Long parseId(Claims claims) {
        Object id = claims.get(CLAIM_ID);
        if (id instanceof Number number) {
            return number.longValue();
        }
        return Optional.ofNullable(id)
                .map(Object::toString)
                .map(Long::parseLong)
                .orElseThrow(() -> new UnauthorizedException("Invalid token claims"));
    }

    private byte[] getSecretBytes() {
        String secret = properties.getSecret();
        try {
            return Decoders.BASE64.decode(secret);
        } catch (DecodingException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
