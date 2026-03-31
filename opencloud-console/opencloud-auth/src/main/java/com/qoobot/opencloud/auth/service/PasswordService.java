package com.qoobot.opencloud.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qoobot.opencloud.auth.domain.dto.ChangePasswordRequest;
import com.qoobot.opencloud.auth.domain.entity.UserCredential;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.mapper.UserCredentialMapper;
import com.qoobot.opencloud.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 密码服务：修改密码、强度校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserCredentialMapper userCredentialMapper;
    private final PasswordEncoder      passwordEncoder;
    private final TokenService         tokenService;
    private final OperationLogService  operationLogService;
    private final JwtTokenProvider     jwtTokenProvider;

    /**
     * 修改密码（全局 Token 失效）
     *
     * @param userId 当前登录用户 ID
     * @param claims 当前 AccessToken Claims（用于加黑名单）
     * @param req    请求体
     */
    public void changePassword(String userId, Claims claims, ChangePasswordRequest req) {
        // 1. 参数校验
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw AuthException.passwordNotMatch();
        }
        checkStrength(req.getNewPassword());

        // 2. 查凭证
        UserCredential credential = userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId));
        if (credential == null) {
            throw AuthException.userNotFound();
        }

        // 3. 旧密码比对
        if (!passwordEncoder.matches(req.getOldPassword(), credential.getPasswordHash())) {
            throw AuthException.oldPasswordError();
        }

        // 4. 不能与旧密码相同
        if (passwordEncoder.matches(req.getNewPassword(), credential.getPasswordHash())) {
            throw AuthException.sameAsOldPassword();
        }

        // 5. 更新 DB（精确字段更新）
        userCredentialMapper.update(null,
                new LambdaUpdateWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId)
                        .set(UserCredential::getPasswordHash,
                                passwordEncoder.encode(req.getNewPassword()))
                        .set(UserCredential::getPasswordUpdateTime, LocalDateTime.now()));

        // 6. 使所有 Token 失效
        String jti    = jwtTokenProvider.parseJti(claims);
        long   remain = jwtTokenProvider.getRemainSeconds(claims);
        tokenService.revokeToken(jti, remain);          // 当前 AccessToken 加黑名单
        tokenService.deleteAllRefreshTokens(userId);    // 清除所有 RefreshToken

        // 7. 写操作审计日志
        operationLogService.record(userId, userId, "CHANGE_PASSWORD");

        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 密码强度校验：≥8 位，同时含大写字母、小写字母、数字
     */
    public void checkStrength(String password) {
        if (password == null || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw AuthException.weakPassword();
        }
    }
}
