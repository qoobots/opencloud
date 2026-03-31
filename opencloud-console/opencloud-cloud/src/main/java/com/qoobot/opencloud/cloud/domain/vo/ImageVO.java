package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 镜像 VO
 */
@Data
public class ImageVO {

    private String id;
    private String name;
    private String status;
    private Long size;
    private String containerFormat;
    private String diskFormat;
    private Integer minDisk;
    private Integer minRam;
    private Boolean isPublic;
    private Boolean isProtected;
    private String checksum;
    private String owner;
    private Map<String, String> properties;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
