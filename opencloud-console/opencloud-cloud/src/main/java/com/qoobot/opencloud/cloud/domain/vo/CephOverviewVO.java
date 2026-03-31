package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * Ceph 集群总览 VO
 */
@Data
public class CephOverviewVO {

    private String clusterId;
    private String clusterName;
    private String healthStatus;
    private String healthMessage;
    private Long totalBytes;
    private Long usedBytes;
    private Long availableBytes;
    private Double usagePercent;
    private Integer totalObjects;
    private Integer poolCount;
    private Integer osdCount;
    private Integer osdUpCount;
    private Integer osdInCount;
    private List<OsdStatus> osdStatuses;

    @Data
    public static class OsdStatus {
        private Integer osdId;
        private String status;
        private String deviceClass;
        private Double weight;
        private Long kbUsed;
        private Long kbAvailable;
        private Long kbTotal;
    }
}
