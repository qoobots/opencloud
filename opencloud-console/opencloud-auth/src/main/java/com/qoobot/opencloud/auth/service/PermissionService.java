package com.qoobot.opencloud.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.auth.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 权限缓存服务
 *
 * <p>先读 Redis，未命中查数据库，回写缓存（TTL 10 分钟）。
 * <p>单体架构直接查 system 模块共享 Schema，无 Feign。
 * <p>使用 JSON 序列化存储 Set，兼容 StringRedisTemplate。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final StringRedisTemplate redisTemplate;
    private final PermissionMapper    permissionMapper;
    private final ObjectMapper        objectMapper;

    private static final String KEY_PERM = "auth:perm:";
    private static final long   PERM_TTL = 10L; // 分钟

    private static final TypeReference<Set<String>> SET_TYPE_REF = new TypeReference<>() {};

    // ── 权限标识 ─────────────────────────────────────────────

    /**
     * 获取用户权限标识集合（含缓存）
     * 缓存键：auth:perm:{userId}，值为 JSON 数组字符串
     */
    public Set<String> getPermissions(String userId) {
        String cacheKey = KEY_PERM + userId;

        // 1. 读缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }

        // 2. 查数据库
        Set<String> permissions = permissionMapper.selectPermissionsByUserId(userId);
        if (permissions == null || permissions.isEmpty()) {
            permissions = Collections.emptySet();
        }

        // 3. 写缓存（TTL 10 分钟）
        redisTemplate.opsForValue().set(cacheKey, serialize(permissions), PERM_TTL, TimeUnit.MINUTES);
        return permissions;
    }

    /**
     * 获取用户角色 code 集合（不缓存，查询较轻量）
     */
    public Set<String> getRoles(String userId) {
        Set<String> roles = permissionMapper.selectRoleCodesByUserId(userId);
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * 清除指定用户的权限缓存
     * （角色/权限变更时调用）
     */
    public void evictPermissionCache(String userId) {
        redisTemplate.delete(KEY_PERM + userId);
        log.debug("权限缓存已清除: userId={}", userId);
    }

    // ── 序列化工具 ───────────────────────────────────────────

    private String serialize(Set<String> set) {
        try {
            return objectMapper.writeValueAsString(set);
        } catch (Exception e) {
            log.warn("权限集合序列化失败，返回空 JSON 数组", e);
            return "[]";
        }
    }

    private Set<String> deserialize(String json) {
        try {
            return objectMapper.readValue(json, SET_TYPE_REF);
        } catch (Exception e) {
            log.warn("权限集合反序列化失败，缓存视为失效: json={}", json, e);
            return null; // 返回 null 触发重新查库
        }
    }
}
