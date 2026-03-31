package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 卷 VO
 */
@Data
public class VolumeVO {

    private String id;
    private String name;
    private String description;
    private Integer size;
    private String status;
    private String volumeType;
    private Boolean bootable;
    private Boolean encrypted;
    private String availabilityZone;
    private String projectId;
    private List<AttachmentVO> attachments;
    private Map<String, String> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class AttachmentVO {
        private String attachmentId;
        private String instanceId;
        private String instanceName;
        private String device;
    }
}
