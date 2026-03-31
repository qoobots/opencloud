package com.qoobot.opencloud.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qoobot.opencloud.auth.domain.entity.UserCredential;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.mapper.UserCredentialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理员操作服务：强制下线、解锁账号
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserCredentialMapper userCredentialMapper;
    private final TokenService         tokenService;
    private final LoginLogService      loginLogService;
    private final OperationLogService  operationLogService;

    /**
     * 强制下线指定用户
     * - tokenVersion +1（所有旧 AccessToken 立即失效）
     * - 清除所有 RefreshToken
     * - 写日志
     */
    public void forceLogout(String operatorId, String targetUserId) {
        // 验证目标用户存在
        ensureUserExists(targetUserId);

        // 1. tokenVersion +1，旧 AccessToken 全部失效
        tokenService.incrementTokenVersion(targetUserId);

        // 2. 清除所有 RefreshToken
        tokenService.deleteAllRefreshTokens(targetUserId);

        // 3. 写登录日志（FORCE_LOGOUT）
        loginLogService.recordForceLogout(targetUserId);

        // 4. 写操作审计日志
        operationLogService.record(operatorId, targetUserId, "FORCE_LOGOUT");

        log.info("管理员强制下线: operatorId={}, targetUserId={}", operatorId, targetUserId);
    }

    /**
     * 解锁账号
     * - status 改为 ACTIVE，failCount 清零，lockExpireTime 清空
     */
    public void unlockAccount(String operatorId, String targetUserId) {
        UserCredential credential = userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, targetUserId));
        if (credential == null) {
            throw AuthException.userNotFound();
        }
        if (!"LOCKED".equals(credential.getStatus())) {
            throw AuthException.accountNotLocked();
        }

        userCredentialMapper.update(null,
                new LambdaUpdateWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, targetUserId)
                        .set(UserCredential::getStatus, "ACTIVE")
                        .set(UserCredential::getLoginFailCount, 0)
                        .set(UserCredential::getLockExpireTime, null));

        // 写操作审计日志
        operationLogService.record(operatorId, targetUserId, "UNLOCK_ACCOUNT");

        log.info("管理员解锁账号: operatorId={}, targetUserId={}", operatorId, targetUserId);
    }

    private void ensureUserExists(String userId) {
        Long count = userCredentialMapper.selectCount(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId));
        if (count == null || count == 0) {
            throw AuthException.userNotFound();
        }
    }
}
