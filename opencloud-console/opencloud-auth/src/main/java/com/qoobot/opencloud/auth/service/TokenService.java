package com.qoobot.opencloud.auth.service;

import com.qoobot.opencloud.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Token Redis 管理服务
 * - RefreshToken 存储与校验
 * - tokenVersion（强制下线）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_REFRESH    = "auth:refresh:";     // auth:refresh:{userId}:{jti}
    private static final String KEY_BLACKLIST  = "auth:blacklist:";   // auth:blacklist:{jti}
    private static final String KEY_VERSION    = "auth:token:version:"; // auth:token:version:{userId}

    // ── RefreshToken ────────────────────────────────────────

    public void storeRefreshToken(String userId, String jti, String refreshToken, long ttlSeconds) {
        String key = KEY_REFRESH + userId + ":" + jti;
        redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 校验 RefreshToken：Key 存在 + 值完全一致
     * @throws AuthException 不存在 → refreshTokenInvalid；值不一致 → refreshTokenReplay
     */
    public void validateRefreshToken(String userId, String jti, String refreshToken) {
        String key   = KEY_REFRESH + userId + ":" + jti;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw AuthException.refreshTokenInvalid();
        }
        if (!stored.equals(refreshToken)) {
            throw AuthException.refreshTokenReplay();
        }
    }

    public void deleteRefreshToken(String userId, String jti) {
        redisTemplate.delete(KEY_REFRESH + userId + ":" + jti);
    }

    /**
     * 清除该用户所有 RefreshToken（SCAN 代替 KEYS，避免 Redis 阻塞）
     */
    public void deleteAllRefreshTokens(String userId) {
        String pattern = KEY_REFRESH + userId + ":*";
        ScanOptions opts = ScanOptions.scanOptions().match(pattern).count(100).build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(opts)) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.warn("SCAN RefreshToken keys failed: {}", e.getMessage());
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── Token 黑名单 ────────────────────────────────────────

    public void revokeToken(String jti, long remainSeconds) {
        if (remainSeconds > 0) {
            redisTemplate.opsForValue().set(
                    KEY_BLACKLIST + jti, "1", remainSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_BLACKLIST + jti));
    }

    // ── tokenVersion（强制下线） ─────────────────────────────

    public int getTokenVersion(String userId) {
        String v = redisTemplate.opsForValue().get(KEY_VERSION + userId);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public void incrementTokenVersion(String userId) {
        redisTemplate.opsForValue().increment(KEY_VERSION + userId);
    }
}
