package com.graphify.auth;

import com.graphify.config.GraphifyAuthProperties;
import com.graphify.user.User;
import com.graphify.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final GraphifyAuthProperties properties;
    private final SecretKey secretKey;

    public JwtTokenService(GraphifyAuthProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        return buildToken(user, "access", properties.getAccessExpirationMinutes() * 60);
    }

    public String createRefreshToken(User user) {
        return buildToken(user, "refresh", properties.getRefreshExpirationDays() * 24 * 60 * 60);
    }

    private String buildToken(User user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserRole extractRole(Claims claims) {
        return UserRole.valueOf(claims.get("role", String.class));
    }
}
