package com.qoobot.opencloud.system.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限缓存清除助手
 * 封装对 auth 模块 Redis 缓存的操作（清除 auth:perm:{userId}）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPermCacheHelper {

    private static final String PERM_KEY_PREFIX     = "auth:perm:";
    private static final String TOKEN_VERSION_KEY   = "auth:token:version:";
    private static final String REFRESH_KEY_PATTERN = "auth:refresh:%s:*";

    private final StringRedisTemplate redisTemplate;

    /**
     * 清除单用户权限缓存
     */
    public void evictPerm(String userId) {
        redisTemplate.delete(PERM_KEY_PREFIX + userId);
        log.debug("权限缓存已清除: userId={}", userId);
    }

    /**
     * 批量清除多用户权限缓存
     */
    public void evictPerms(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        List<String> keys = userIds.stream()
                .map(id -> PERM_KEY_PREFIX + id)
                .collect(Collectors.toList());
        redisTemplate.delete(keys);
        log.debug("批量权限缓存已清除: count={}", keys.size());
    }

    /**
     * 强制下线用户：
     * 1. INCR token:version（所有旧 AccessToken 失效）
     * 2. SCAN+DEL 所有 RefreshToken
     */
    public void forceLogout(String userId) {
        redisTemplate.opsForValue().increment(TOKEN_VERSION_KEY + userId);
        String pattern = String.format(REFRESH_KEY_PATTERN, userId);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("forceLogout scan delete failed for userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 批量强制下线多个用户
     */
    public void forceLogoutBatch(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        userIds.forEach(this::forceLogout);
    }
}
