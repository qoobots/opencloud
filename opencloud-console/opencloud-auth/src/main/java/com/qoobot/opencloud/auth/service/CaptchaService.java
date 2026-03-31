package com.qoobot.opencloud.auth.service;

import com.qoobot.opencloud.auth.domain.vo.CaptchaVO;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.wf.captcha.SpecCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * - 使用 easy-captcha 生成图形验证码
 * - 验证码大小写不敏感，一次有效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "auth:captcha:";
    private static final long   TTL_MINUTES = 5;

    /**
     * 生成验证码，写入 Redis，返回 CaptchaVO
     */
    public CaptchaVO generate() {
        // 130x48，5位字符
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 5);
        String code       = captcha.text().toLowerCase();   // 统一小写
        String captchaKey = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                KEY_PREFIX + captchaKey, code, TTL_MINUTES, TimeUnit.MINUTES);

        log.debug("验证码已生成: key={}", captchaKey);
        return new CaptchaVO(captchaKey, captcha.toBase64());
    }

    /**
     * 校验验证码（大小写不敏感，无论结果都删除 Key）
     *
     * @throws AuthException captchaExpired / captchaError
     */
    public void validate(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            throw AuthException.captchaError();
        }
        String storedCode = redisTemplate.opsForValue().get(KEY_PREFIX + captchaKey);
        // 无论成功失败都删除（一次有效）
        redisTemplate.delete(KEY_PREFIX + captchaKey);

        if (storedCode == null) {
            throw AuthException.captchaExpired();
        }
        if (!storedCode.equals(captchaCode.toLowerCase())) {
            throw AuthException.captchaError();
        }
    }
}
