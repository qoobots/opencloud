package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卷快照 VO
 */
@Data
public class VolumeSnapshotVO {

    private String id;
    private String name;
    private String description;
    private String volumeId;
    private String volumeName;
    private Integer size;
    private String status;
    private String projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
