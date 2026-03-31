package com.qoobot.opencloud.monitor.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 * 用于加密通知渠道中的敏感字段（密码、secret、token 等）
 */
@Component
public class AesEncryptUtil {

    @Value("${opencloud.monitor.encrypt-key:default-32bytes-key!!!!!!!}")
    private String encryptKeyConfig;

    private static String ENCRYPT_KEY;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @PostConstruct
    public void init() {
        ENCRYPT_KEY = encryptKeyConfig;
    }

    /**
     * 加密明文
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（IV + ciphertext）
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] combined = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * 解密密文
     *
     * @param ciphertext Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] data = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    /**
     * 获取密钥
     */
    private static SecretKeySpec getKey() {
        byte[] keyBytes = ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key32, "AES");
    }

    /**
     * 判断字符串是否已加密（简单判断是否为 Base64 格式）
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(text);
            return text.length() > 20; // 加密后的字符串通常较长
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
