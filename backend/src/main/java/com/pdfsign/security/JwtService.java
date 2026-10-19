package com.pdfsign.security;

import com.pdfsign.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_EMAIL = "email";

    private final AppProperties appProperties;
    private final SecretKey key;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET must be set");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(padSecret(secretBytes));
    }

    private static byte[] padSecret(byte[] secretBytes) {
        if (secretBytes.length >= 32) {
            return secretBytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
        return padded;
    }

    public String createAccessToken(UUID userId, String email, UUID tenantId) {
        return buildToken(userId, email, tenantId, TYPE_ACCESS, appProperties.getJwt().getAccessExpirationMs());
    }

    public String createRefreshToken(UUID userId, String email, UUID tenantId) {
        return buildToken(userId, email, tenantId, TYPE_REFRESH, appProperties.getJwt().getRefreshExpirationMs());
    }

    private String buildToken(UUID userId, String email, UUID tenantId, String type, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }
}
