package com.qoobot.opencloud.auth.controller;

import com.qoobot.opencloud.auth.domain.vo.CaptchaVO;
import com.qoobot.opencloud.auth.service.CaptchaService;
import com.qoobot.opencloud.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码接口
 */
@Tag(name = "验证码", description = "获取图形验证码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码", description = "返回 captchaKey 和 Base64 图片，TTL 5 分钟，一次有效")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(captchaService.generate());
    }
}
