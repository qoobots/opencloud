package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全组 VO
 */
@Data
public class SecurityGroupVO {

    private String id;
    private String name;
    private String description;
    private String projectId;
    private List<RuleVO> rules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class RuleVO {
        private String id;
        private String direction;
        private String protocol;
        private String ethertype;
        private Integer portRangeMin;
        private Integer portRangeMax;
        private String remoteIpPrefix;
        private String remoteGroupId;
    }
}
