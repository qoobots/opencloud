package com.qoobot.opencloud.auth.controller;

import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.service.AdminAuthService;
import com.qoobot.opencloud.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员操作接口：强制下线、解锁账号
 */
@Tag(name = "管理员操作", description = "强制下线、解锁账号（需要管理员权限）")
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "强制下线指定用户",
               description = "tokenVersion +1，使该用户所有 Token 立即失效",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('auth:admin:force-logout')")
    @PostMapping("/force-logout/{targetUserId}")
    public R<Void> forceLogout(
            @Parameter(description = "目标用户 ID") @PathVariable String targetUserId,
            @AuthenticationPrincipal String operatorId) {
        if (operatorId == null) {
            throw AuthException.tokenMissing();
        }
        adminAuthService.forceLogout(operatorId, targetUserId);
        return R.ok("已强制下线", null);
    }

    @Operation(summary = "解锁被锁定账号",
               description = "将账号状态恢复为 ACTIVE，失败计数清零",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('auth:admin:unlock')")
    @PostMapping("/unlock/{targetUserId}")
    public R<Void> unlock(
            @Parameter(description = "目标用户 ID") @PathVariable String targetUserId,
            @AuthenticationPrincipal String operatorId) {
        if (operatorId == null) {
            throw AuthException.tokenMissing();
        }
        adminAuthService.unlockAccount(operatorId, targetUserId);
        return R.ok("账号已解锁", null);
    }
}
