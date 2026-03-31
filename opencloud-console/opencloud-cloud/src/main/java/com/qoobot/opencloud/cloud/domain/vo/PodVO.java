package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Pod VO
 */
@Data
public class PodVO {

    private String name;
    private String namespace;
    private String status;
    private String phase;
    private String nodeName;
    private String podIp;
    private String hostIp;
    private Integer restartCount;
    private String image;
    private Map<String, String> labels;
    private List<ContainerStatusVO> containerStatuses;
    private LocalDateTime creationTimestamp;
    private LocalDateTime startTime;

    @Data
    public static class ContainerStatusVO {
        private String name;
        private String state;
        private String reason;
        private String message;
        private Integer restartCount;
        private Boolean ready;
    }
}
