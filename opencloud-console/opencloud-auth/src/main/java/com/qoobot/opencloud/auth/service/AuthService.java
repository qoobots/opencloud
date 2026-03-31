package com.qoobot.opencloud.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qoobot.opencloud.auth.domain.dto.LoginRequest;
import com.qoobot.opencloud.auth.domain.entity.SysUserQuery;
import com.qoobot.opencloud.auth.domain.entity.UserCredential;
import com.qoobot.opencloud.auth.domain.vo.LoginVO;
import com.qoobot.opencloud.auth.domain.vo.TokenRefreshVO;
import com.qoobot.opencloud.auth.domain.vo.UserInfoVO;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.mapper.SysUserQueryMapper;
import com.qoobot.opencloud.auth.mapper.UserCredentialMapper;
import com.qoobot.opencloud.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 认证核心服务：登录、登出、Token 刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialMapper userCredentialMapper;
    private final SysUserQueryMapper   sysUserQueryMapper;
    private final JwtTokenProvider     jwtTokenProvider;
    private final TokenService         tokenService;
    private final PasswordEncoder      passwordEncoder;
    private final PermissionService    permissionService;
    private final CaptchaService       captchaService;
    private final LoginLogService      loginLogService;
    private final StringRedisTemplate  redisTemplate;

    @Value("${opencloud.auth.captcha-enabled:false}")
    private boolean captchaEnabled;

    @Value("${opencloud.auth.login-rate-max:10}")
    private int loginRateMax;

    @Value("${opencloud.auth.login-fail-max:5}")
    private int loginFailMax;

    @Value("${opencloud.auth.login-fail-lock-minutes:15}")
    private int loginFailLockMinutes;

    // ── 登录 ─────────────────────────────────────────────────

    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);

        // 1. 登录限频（IP 维度）
        checkLoginRate(ip);

        // 2. 验证码校验
        if (captchaEnabled) {
            captchaService.validate(request.getCaptchaKey(), request.getCaptchaCode());
        }

        // 3. 先查 sys_user 获取 userId（username → Long id）
        SysUserQuery sysUser = sysUserQueryMapper.selectOne(
                new LambdaQueryWrapper<SysUserQuery>()
                        .eq(SysUserQuery::getUsername, request.getUsername())
                        .eq(SysUserQuery::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (sysUser == null) {
            loginLogService.recordFailure(null, "用户不存在", httpRequest);
            throw AuthException.badCredentials();
        }

        // 4. 再查 auth_user_credential（userId 为 sys_user.id 的字符串形式）
        String userId = String.valueOf(sysUser.getId());
        UserCredential credential = userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId)
        );
        if (credential == null) {
            loginLogService.recordFailure(userId, "凭证不存在", httpRequest);
            throw AuthException.badCredentials();
        }

        // 5. 账号状态检查 & 自动解锁
        checkAndAutoUnlock(credential);

        if ("DISABLED".equals(credential.getStatus())) {
            throw AuthException.accountDisabled();
        }
        if ("LOCKED".equals(credential.getStatus())) {
            long remain = 0;
            if (credential.getLockExpireTime() != null) {
                remain = ChronoUnit.MINUTES.between(LocalDateTime.now(), credential.getLockExpireTime());
            }
            loginLogService.recordFailure(credential.getUserId(), "账号锁定", httpRequest);
            throw AuthException.accountLocked(Math.max(remain, 1));
        }

        // 6. 密码比对
        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            handleLoginFailure(credential);
            loginLogService.recordFailure(credential.getUserId(), "密码错误", httpRequest);
            throw AuthException.badCredentials();
        }

        // 7. 登录成功，重置失败计数
        userCredentialMapper.update(null,
                new LambdaUpdateWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, credential.getUserId())
                        .set(UserCredential::getLoginFailCount, 0)
                        .set(UserCredential::getLastLoginTime, LocalDateTime.now()));

        // 8. 获取用户权限
        Set<String> permissions = permissionService.getPermissions(credential.getUserId());
        Set<String> roles       = permissionService.getRoles(credential.getUserId());

        // 9. 签发 Token
        int tokenVersion = tokenService.getTokenVersion(credential.getUserId());
        String accessToken  = jwtTokenProvider.generateAccessToken(
                credential.getUserId(), credential.getTenantId(),
                request.getUsername(), List.copyOf(roles), tokenVersion);
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        String jti          = jwtTokenProvider.validateAndParseClaims(accessToken).getId();

        // 10. 存储 RefreshToken
        tokenService.storeRefreshToken(credential.getUserId(), jti, refreshToken,
                jwtTokenProvider.getRefreshTokenValidity());

        // 11. 写登录日志
        loginLogService.recordSuccess(credential.getUserId(), credential.getTenantId(),
                jti, httpRequest);

        log.info("用户登录成功: userId={}", credential.getUserId());

        return LoginVO.builder()
                .accessToken(accessToken)
                .accessTokenExpireAt(jwtTokenProvider.getAccessTokenExpireAt())
                .refreshToken(refreshToken)
                .refreshTokenExpireAt(jwtTokenProvider.getRefreshTokenExpireAt())
                .tokenType("Bearer")
                .userInfo(UserInfoVO.builder()
                        .userId(credential.getUserId())
                        .username(request.getUsername())
                        .nickname(sysUser.getNickname())
                        .avatar(sysUser.getAvatar())
                        .tenantId(credential.getTenantId())
                        .roles(roles)
                        .permissions(permissions)
                        .build())
                .build();
    }

    // ── 登出 ─────────────────────────────────────────────────

    public void logout(Claims claims) {
        String jti    = jwtTokenProvider.parseJti(claims);
        String userId = jwtTokenProvider.parseUserId(claims);
        long remain   = jwtTokenProvider.getRemainSeconds(claims);

        // 将 AccessToken 加入黑名单
        tokenService.revokeToken(jti, remain);
        // 删除对应的 RefreshToken
        tokenService.deleteRefreshToken(userId, jti);
        // 写登出日志
        loginLogService.recordLogout(userId, jti);

        log.info("用户登出: userId={}", userId);
    }

    // ── Token 刷新 ───────────────────────────────────────────

    public TokenRefreshVO refreshToken(String refreshToken) {
        // RefreshToken 是 UUID，需要从 Redis 中检索匹配的 Key
        // 设计：刷新时前端同时传 userId 或在 Cookie 中携带（此处简化，扫描匹配）
        // 实际生产建议前端传 userId 以避免 SCAN
        throw new UnsupportedOperationException(
                "Token 刷新需要 userId 参数，请使用 /auth/token/refresh 接口");
    }

    /**
     * Token 刷新（携带 userId）
     */
    public TokenRefreshVO refreshToken(String userId, String oldJti, String refreshToken) {
        // 1. 校验 RefreshToken
        tokenService.validateRefreshToken(userId, oldJti, refreshToken);

        // 2. 删除旧 RefreshToken（单次有效）
        tokenService.deleteRefreshToken(userId, oldJti);

        // 3. 获取权限（用新版本签发）
        Set<String> roles = permissionService.getRoles(userId);
        int tokenVersion  = tokenService.getTokenVersion(userId);

        // 4. 签发新 Token
        String newAccessToken  = jwtTokenProvider.generateAccessToken(
                userId, null, null, List.copyOf(roles), tokenVersion);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        String newJti          = jwtTokenProvider.validateAndParseClaims(newAccessToken).getId();

        // 5. 存储新 RefreshToken
        tokenService.storeRefreshToken(userId, newJti, newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidity());

        return TokenRefreshVO.builder()
                .accessToken(newAccessToken)
                .accessTokenExpireAt(jwtTokenProvider.getAccessTokenExpireAt())
                .refreshToken(newRefreshToken)
                .refreshTokenExpireAt(jwtTokenProvider.getRefreshTokenExpireAt())
                .build();
    }

    // ── 私有辅助 ─────────────────────────────────────────────

    /** IP 登录限频：每分钟最多 loginRateMax 次 */
    private void checkLoginRate(String ip) {
        String key = "auth:login:rate:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        if (count != null && count > loginRateMax) {
            throw AuthException.loginRateLimit();
        }
    }

    /** 登录前检查是否已自动解锁（锁定过期则恢复 ACTIVE） */
    private void checkAndAutoUnlock(UserCredential credential) {
        if ("LOCKED".equals(credential.getStatus())
                && credential.getLockExpireTime() != null
                && credential.getLockExpireTime().isBefore(LocalDateTime.now())) {
            userCredentialMapper.update(null,
                    new LambdaUpdateWrapper<UserCredential>()
                            .eq(UserCredential::getUserId, credential.getUserId())
                            .set(UserCredential::getStatus, "ACTIVE")
                            .set(UserCredential::getLoginFailCount, 0)
                            .set(UserCredential::getLockExpireTime, null));
            credential.setStatus("ACTIVE");
            credential.setLoginFailCount(0);
        }
    }

    /** 密码比对失败：递增失败计数，达到阈值则锁定 */
    private void handleLoginFailure(UserCredential credential) {
        int newFailCount = credential.getLoginFailCount() + 1;
        LambdaUpdateWrapper<UserCredential> wrapper = new LambdaUpdateWrapper<UserCredential>()
                .eq(UserCredential::getUserId, credential.getUserId())
                .setSql("login_fail_count = login_fail_count + 1");

        if (newFailCount >= loginFailMax) {
            wrapper.set(UserCredential::getStatus, "LOCKED")
                   .set(UserCredential::getLockExpireTime,
                           LocalDateTime.now().plusMinutes(loginFailLockMinutes));
        }
        userCredentialMapper.update(null, wrapper);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
