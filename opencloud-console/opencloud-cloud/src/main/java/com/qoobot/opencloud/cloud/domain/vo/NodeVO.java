package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Node VO
 */
@Data
public class NodeVO {

    private String name;
    private String status;
    private List<String> roles;
    private String version;
    private String osImage;
    private String kernelVersion;
    private String containerRuntime;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private ResourceVO capacity;
    private ResourceVO allocatable;
    private ResourceVO usage;
    private List<ConditionVO> conditions;
    private LocalDateTime creationTimestamp;

    @Data
    public static class ResourceVO {
        private String cpu;
        private String memory;
        private String pods;
        private String ephemeralStorage;
    }

    @Data
    public static class ConditionVO {
        private String type;
        private String status;
        private String reason;
        private String message;
        private LocalDateTime lastHeartbeatTime;
        private LocalDateTime lastTransitionTime;
    }
}
