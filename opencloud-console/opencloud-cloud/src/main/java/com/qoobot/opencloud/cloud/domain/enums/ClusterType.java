package com.qoobot.opencloud.cloud.domain.enums;

import lombok.Getter;

/**
 * 集群类型枚举
 */
@Getter
public enum ClusterType {
    OPENSTACK("OPENSTACK", "OpenStack"),
    CEPH("CEPH", "Ceph"),
    KUBERNETES("KUBERNETES", "Kubernetes");

    private final String code;
    private final String desc;

    ClusterType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ClusterType fromCode(String code) {
        for (ClusterType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
