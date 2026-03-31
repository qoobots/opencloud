package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Deployment VO
 */
@Data
public class DeploymentVO {

    private String name;
    private String namespace;
    private Integer replicas;
    private Integer availableReplicas;
    private Integer readyReplicas;
    private Integer updatedReplicas;
    private String strategy;
    private String status;
    private String image;
    private Map<String, String> labels;
    private Map<String, String> selectors;
    private List<ContainerVO> containers;
    private LocalDateTime creationTimestamp;

    @Data
    public static class ContainerVO {
        private String name;
        private String image;
        private List<String> ports;
        private Map<String, String> resources;
    }
}
