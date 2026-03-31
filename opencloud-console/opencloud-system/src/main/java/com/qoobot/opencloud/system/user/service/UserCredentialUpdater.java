package com.qoobot.opencloud.system.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 跨 Schema 操作 auth.auth_user_credential 的辅助组件
 * 通过 JdbcTemplate 直接执行 SQL，避免在 system 模块定义 auth schema 实体
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCredentialUpdater {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建认证凭证（新增用户时调用）
     */
    public void createCredential(String userId, String tenantId, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO auth.auth_user_credential " +
                "(user_id, tenant_id, password_hash, status, login_fail_count, password_update_time, last_login_time) " +
                "VALUES (?, ?, ?, 'ACTIVE', 0, ?, NULL)",
                userId, tenantId, passwordHash, LocalDateTime.now()
        );
        log.debug("创建认证凭证: userId={}", userId);
    }

    /**
     * 更新密码 Hash
     */
    public void updatePasswordHash(String userId, String passwordHash) {
        jdbcTemplate.update(
                "UPDATE auth.auth_user_credential SET password_hash = ?, password_update_time = ? WHERE user_id = ?",
                passwordHash, LocalDateTime.now(), userId
        );
    }

    /**
     * 同步更新账号状态
     */
    public void updateStatus(String userId, String status) {
        jdbcTemplate.update(
                "UPDATE auth.auth_user_credential SET status = ? WHERE user_id = ?",
                status, userId
        );
    }
}
