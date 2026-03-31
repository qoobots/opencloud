package com.qoobot.opencloud.auth.controller;

import com.qoobot.opencloud.auth.domain.dto.ChangePasswordRequest;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.security.JwtTokenProvider;
import com.qoobot.opencloud.auth.service.PasswordService;
import com.qoobot.opencloud.common.result.R;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 密码管理接口
 *
 * <p>POST /auth/password/change — 修改密码（全局 Token 失效，需重新登录）
 */
@Tag(name = "密码管理", description = "修改密码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService  passwordService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "修改密码",
            description = "校验旧密码后更新，成功后当前用户所有 Token 立即失效，需重新登录",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/password/change")
    public R<Void> changePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        if (userId == null) {
            throw AuthException.tokenMissing();
        }
        Claims claims = (Claims) httpRequest.getAttribute("jwtClaims");
        passwordService.changePassword(userId, claims, request);
        return R.ok("密码修改成功，请重新登录", null);
    }
}
