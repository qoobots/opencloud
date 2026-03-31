package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * S3 Bucket VO
 */
@Data
public class S3BucketVO {

    private String name;
    private LocalDateTime creationDate;
    private Long objectCount;
    private Long totalSize;
    private String region;
}
