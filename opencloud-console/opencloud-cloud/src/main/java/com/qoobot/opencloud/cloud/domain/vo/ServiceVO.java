package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service VO
 */
@Data
public class ServiceVO {

    private String name;
    private String namespace;
    private String type;
    private String clusterIp;
    private List<String> externalIps;
    private List<PortVO> ports;
    private Map<String, String> selectors;
    private Map<String, String> labels;
    private LocalDateTime creationTimestamp;

    @Data
    public static class PortVO {
        private String name;
        private Integer port;
        private Integer targetPort;
        private Integer nodePort;
        private String protocol;
    }
}
