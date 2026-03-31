package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * S3 对象 VO
 */
@Data
public class S3ObjectVO {

    private String key;
    private Long size;
    private LocalDateTime lastModified;
    private String etag;
    private String storageClass;
    private String contentType;
    private Map<String, String> metadata;
    private Boolean isDirectory;
}
