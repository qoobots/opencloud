package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浮动 IP VO
 */
@Data
public class FloatingIpVO {

    private String id;
    private String floatingIpAddress;
    private String fixedIpAddress;
    private String status;
    private String portId;
    private String networkId;
    private String networkName;
    private String routerId;
    private String description;
    private String projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
