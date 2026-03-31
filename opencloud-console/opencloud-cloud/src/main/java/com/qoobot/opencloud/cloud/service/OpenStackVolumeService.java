package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.dto.SnapshotCreateDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeSnapshotVO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeVO;
import com.qoobot.opencloud.common.core.page.PageResult;

/**
 * OpenStack 存储服务接口
 */
public interface OpenStackVolumeService {

    /**
     * 获取卷列表
     */
    PageResult<VolumeVO> listVolumes(String clusterId, String status, String name, int page, int size);

    /**
     * 创建卷
     */
    VolumeVO createVolume(VolumeCreateDTO dto);

    /**
     * 删除卷
     */
    void deleteVolume(String clusterId, String volumeId);

    /**
     * 卷操作
     */
    void performAction(String clusterId, String volumeId, VolumeActionDTO dto);

    /**
     * 获取快照列表
     */
    PageResult<VolumeSnapshotVO> listSnapshots(String clusterId, String volumeId, int page, int size);

    /**
     * 创建快照
     */
    VolumeSnapshotVO createSnapshot(SnapshotCreateDTO dto);

    /**
     * 删除快照
     */
    void deleteSnapshot(String clusterId, String snapshotId);
}
