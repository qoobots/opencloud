package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 云主机 VO
 */
@Data
public class InstanceVO {

    private String id;
    private String name;
    private String clusterId;
    private String clusterName;
    private String tenantId;
    private String status;
    private String powerState;
    private String flavorId;
    private String flavorName;
    private Integer vcpus;
    private Integer memoryMb;
    private String imageId;
    private String imageName;
    private List<NetworkInfo> networks;
    private List<VolumeInfo> volumes;
    private List<String> securityGroups;
    private String keyPairName;
    private String hostId;
    private String availabilityZone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, String> metadata;

    @Data
    public static class NetworkInfo {
        private String networkId;
        private String networkName;
        private List<String> fixedIps;
        private String floatingIp;
        private String macAddress;
    }

    @Data
    public static class VolumeInfo {
        private String volumeId;
        private String device;
        private Integer sizeGb;
        private Boolean bootable;
    }
}
