package com.qoobot.opencloud.system.profile.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.profile.domain.dto.ProfileUpdateDTO;
import com.qoobot.opencloud.system.profile.domain.vo.ProfileVO;
import com.qoobot.opencloud.system.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心接口
 */
@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/system/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "获取个人信息（含角色）")
    @GetMapping
    public R<ProfileVO> getProfile() {
        return R.ok(profileService.getProfile());
    }

    @Operation(summary = "修改个人信息（昵称/邮箱/手机/头像）")
    @PutMapping
    @SysLog(module = "个人中心", action = "修改个人信息")
    public R<Void> updateProfile(@RequestBody ProfileUpdateDTO dto) {
        profileService.updateProfile(dto);
        return R.ok();
    }
}
