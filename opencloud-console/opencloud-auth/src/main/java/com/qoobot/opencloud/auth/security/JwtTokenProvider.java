package com.qoobot.opencloud.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT Token 提供者
 * - 签发 AccessToken（HS256，含 jti / tokenVersion）
 * - 生成 UUID 格式 RefreshToken
 * - 校验、解析 Claims
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity:7200}")
    private long accessTokenValidity;   // 秒，默认 2 小时

    @Value("${jwt.refresh-token-validity:604800}")
    private long refreshTokenValidity;  // 秒，默认 7 天

    // ── Token 生成 ──────────────────────────────────────────

    /**
     * 签发 AccessToken
     */
    public String generateAccessToken(String userId, String tenantId,
                                      String username, List<String> roles,
                                      int tokenVersion) {
        String jti = UUID.randomUUID().toString();
        Date now   = new Date();
        Date exp   = new Date(now.getTime() + accessTokenValidity * 1000);

        return Jwts.builder()
                .id(jti)
                .subject(userId)
                .claim("tenantId",     tenantId)
                .claim("username",     username)
                .claim("roles",        roles)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 UUID 格式 RefreshToken
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // ── Token 校验与解析 ────────────────────────────────────

    /**
     * 校验签名 & 过期时间，返回 Claims（过期/无效抛 JwtException）
     */
    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String parseJti(Claims claims)         { return claims.getId(); }
    public String parseUserId(Claims claims)      { return claims.getSubject(); }
    public String parseTenantId(Claims claims)    { return claims.get("tenantId", String.class); }
    public String parseUsername(Claims claims)    { return claims.get("username", String.class); }

    @SuppressWarnings("unchecked")
    public List<String> parseRoles(Claims claims) { return claims.get("roles", List.class); }

    public int parseTokenVersion(Claims claims) {
        Integer v = claims.get("tokenVersion", Integer.class);
        return v == null ? 0 : v;
    }

    /**
     * Token 剩余有效期（秒），用于黑名单 TTL
     */
    public long getRemainSeconds(Claims claims) {
        long expMs  = claims.getExpiration().getTime();
        long remain = (expMs - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 0);
    }

    /**
     * Access Token 过期时间（LocalDateTime）
     */
    public LocalDateTime getAccessTokenExpireAt() {
        return LocalDateTime.now()
                .plusSeconds(accessTokenValidity);
    }

    /**
     * Refresh Token 过期时间（LocalDateTime）
     */
    public LocalDateTime getRefreshTokenExpireAt() {
        return LocalDateTime.now()
                .plusSeconds(refreshTokenValidity);
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    // ── 私有方法 ────────────────────────────────────────────

    private SecretKey getSigningKey() {
        // 支持 Base64 编码密钥；若密钥不是合法 Base64 则回退用 UTF-8 字节
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }
}
