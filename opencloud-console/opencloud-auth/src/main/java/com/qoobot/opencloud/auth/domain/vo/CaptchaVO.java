package com.qoobot.opencloud.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码响应")
public class CaptchaVO {

    @Schema(description = "验证码 Key，提交登录时携带")
    private String captchaKey;

    @Schema(description = "验证码 Base64 图片（data:image/png;base64,...）")
    private String captchaImage;
}
