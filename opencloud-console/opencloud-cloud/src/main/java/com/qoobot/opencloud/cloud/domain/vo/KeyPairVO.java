package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密钥对 VO
 */
@Data
public class KeyPairVO {

    private String name;
    private String fingerprint;
    private String publicKey;
    private LocalDateTime createdAt;
    private String privateKey;
}
