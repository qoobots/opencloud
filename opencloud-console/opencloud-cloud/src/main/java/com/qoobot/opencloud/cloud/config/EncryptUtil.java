package com.qoobot.opencloud.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 * 用于加密存储集群配置的敏感信息
 */
@Component
public class EncryptUtil {

    @Value("${opencloud.encrypt.key:default-encrypt-key-32bytes}")
    private String encryptKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 加密
     */
    public String encrypt(String plaintext) {
        try {
            if (plaintext == null || plaintext.isEmpty()) {
                return plaintext;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     */
    public String decrypt(String ciphertext) {
        try {
            if (ciphertext == null || ciphertext.isEmpty()) {
                return ciphertext;
            }

            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            if (decoded.length < GCM_IV_LENGTH) {
                throw new RuntimeException("密文格式错误");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        // 确保密钥长度为 32 字节（256 位）
        byte[] keyBytes32 = Arrays.copyOf(keyBytes, 32);
        return new SecretKeySpec(keyBytes32, "AES");
    }
}
