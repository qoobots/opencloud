package com.qoobot.opencloud.cloud.domain.enums;

import lombok.Getter;

/**
 * 任务类型枚举
 */
@Getter
public enum TaskType {
    INSTANCE_CREATE("INSTANCE_CREATE", "创建云主机"),
    INSTANCE_DELETE("INSTANCE_DELETE", "删除云主机"),
    INSTANCE_RESIZE("INSTANCE_RESIZE", "调整云主机规格"),
    VOLUME_CREATE("VOLUME_CREATE", "创建卷"),
    VOLUME_DELETE("VOLUME_DELETE", "删除卷"),
    VOLUME_ATTACH("VOLUME_ATTACH", "挂载卷"),
    VOLUME_DETACH("VOLUME_DETACH", "卸载卷"),
    IMAGE_UPLOAD("IMAGE_UPLOAD", "上传镜像"),
    IMAGE_DELETE("IMAGE_DELETE", "删除镜像"),
    CLUSTER_SYNC("CLUSTER_SYNC", "集群资源同步");

    private final String code;
    private final String desc;

    TaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
