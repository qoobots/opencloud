package com.qoobot.opencloud.auth.controller;

import com.qoobot.opencloud.auth.domain.vo.UserInfoVO;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.security.JwtTokenProvider;
import com.qoobot.opencloud.auth.service.PermissionService;
import com.qoobot.opencloud.common.result.R;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 当前用户信息接口
 *
 * <p>GET /auth/me — 获取当前登录用户信息及权限码列表
 */
@Tag(name = "用户信息", description = "获取当前登录用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final PermissionService permissionService;
    private final JwtTokenProvider  jwtTokenProvider;

    @Operation(
            summary = "获取当前用户信息",
            description = "返回用户基本信息 + 角色列表 + 权限码列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public R<UserInfoVO> me(@AuthenticationPrincipal String userId,
                            HttpServletRequest request) {
        if (userId == null) {
            throw AuthException.tokenMissing();
        }
        Claims claims = (Claims) request.getAttribute("jwtClaims");

        Set<String> permissions = permissionService.getPermissions(userId);
        Set<String> roles       = permissionService.getRoles(userId);

        UserInfoVO vo = UserInfoVO.builder()
                .userId(userId)
                .username(claims != null ? jwtTokenProvider.parseUsername(claims) : null)
                .tenantId(claims != null ? jwtTokenProvider.parseTenantId(claims) : null)
                .roles(roles)
                .permissions(permissions)
                .build();

        return R.ok(vo);
    }
}

