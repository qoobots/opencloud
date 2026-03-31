package com.qoobot.opencloud.auth.controller;

import com.qoobot.opencloud.auth.domain.dto.LoginRequest;
import com.qoobot.opencloud.auth.domain.dto.TokenRefreshRequest;
import com.qoobot.opencloud.auth.domain.vo.LoginVO;
import com.qoobot.opencloud.auth.domain.vo.TokenRefreshVO;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.security.JwtTokenProvider;
import com.qoobot.opencloud.auth.service.AuthService;
import com.qoobot.opencloud.common.result.R;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证核心接口：登录 / 登出 / Token 刷新
 */
@Tag(name = "认证管理", description = "登录、登出、Token 刷新")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService      authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "用户登录", description = "返回 accessToken + refreshToken")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request,
                            HttpServletRequest httpRequest) {
        return R.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "Token 刷新", description = "用 refreshToken 换新 accessToken，旧 token 立即废弃")
    @PostMapping("/token/refresh")
    public R<TokenRefreshVO> refresh(@Valid @RequestBody TokenRefreshRequest request,
                                     HttpServletRequest httpRequest) {
        // RefreshToken 请求中需要携带 userId 和 oldJti
        // 通过 Header X-User-Id 和 X-Old-Jti 传递（或存储在 HttpOnly Cookie）
        String userId = httpRequest.getHeader("X-User-Id");
        String oldJti = httpRequest.getHeader("X-Old-Jti");
        if (userId == null || oldJti == null) {
            throw AuthException.refreshTokenInvalid();
        }
        return R.ok(authService.refreshToken(userId, oldJti, request.getRefreshToken()));
    }

    @Operation(summary = "用户登出",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Claims claims = (Claims) request.getAttribute("jwtClaims");
        if (claims == null) {
            throw AuthException.tokenMissing();
        }
        authService.logout(claims);
        return R.ok("登出成功", null);
    }
}
