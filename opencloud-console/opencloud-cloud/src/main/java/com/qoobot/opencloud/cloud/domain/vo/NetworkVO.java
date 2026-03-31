package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网络 VO
 */
@Data
public class NetworkVO {

    private String id;
    private String name;
    private String status;
    private Boolean adminStateUp;
    private Boolean shared;
    private Boolean external;
    private String projectId;
    private List<SubnetVO> subnets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class SubnetVO {
        private String id;
        private String name;
        private String cidr;
        private String gatewayIp;
        private Boolean dhcpEnabled;
        private List<String> dnsNameservers;
        private List<String> allocationPools;
        private String ipVersion;
    }
}
