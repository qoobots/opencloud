package com.qoobot.opencloud.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 —— 生成、解析、校验 Token
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${opencloud.jwt.secret}")
    private String secret;

    @Value("${opencloud.jwt.expiration:86400}")
    private long expiration; // 单位：秒，默认 24h

    @Value("${opencloud.jwt.refresh-expiration:604800}")
    private long refreshExpiration; // 刷新 Token 有效期，默认 7d

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     */
    public String generateToken(Long userId, String username, Map<String, Object> extra) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        if (extra != null) {
            extra.forEach(builder::claim);
        }
        return builder.compact();
    }

    /**
     * 生成 Refresh Token（较长有效期，仅含 userId）
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 获取 userId
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 校验 Token 是否有效（不抛异常返回 true）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT 无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 判断是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public long getExpiration() {
        return expiration;
    }
}
