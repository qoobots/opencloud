package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.dto.SnapshotCreateDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeSnapshotVO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeVO;
import com.qoobot.opencloud.cloud.service.OpenStackVolumeService;
import com.qoobot.opencloud.common.core.page.PageResult;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * OpenStack 存储管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/openstack")
public class OpenStackVolumeController {

    @Autowired
    private OpenStackVolumeService volumeService;

    /**
     * 卷列表
     */
    @GetMapping("/volumes")
    public R<PageResult<VolumeVO>> listVolumes(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(volumeService.listVolumes(clusterId, status, name, page, size));
    }

    /**
     * 创建卷
     */
    @PostMapping("/volumes")
    public R<VolumeVO> createVolume(@Valid @RequestBody VolumeCreateDTO dto) {
        return R.ok(volumeService.createVolume(dto));
    }

    /**
     * 删除卷
     */
    @DeleteMapping("/volumes/{id}")
    public R<Void> deleteVolume(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id) {
        volumeService.deleteVolume(clusterId, id);
        return R.ok();
    }

    /**
     * 卷操作
     */
    @PutMapping("/volumes/{id}/action")
    public R<Void> performAction(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id,
            @Valid @RequestBody VolumeActionDTO dto) {
        volumeService.performAction(clusterId, id, dto);
        return R.ok();
    }

    /**
     * 快照列表
     */
    @GetMapping("/snapshots")
    public R<PageResult<VolumeSnapshotVO>> listSnapshots(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String volumeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(volumeService.listSnapshots(clusterId, volumeId, page, size));
    }

    /**
     * 创建快照
     */
    @PostMapping("/snapshots")
    public R<VolumeSnapshotVO> createSnapshot(@Valid @RequestBody SnapshotCreateDTO dto) {
        return R.ok(volumeService.createSnapshot(dto));
    }

    /**
     * 删除快照
     */
    @DeleteMapping("/snapshots/{id}")
    public R<Void> deleteSnapshot(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id) {
        volumeService.deleteSnapshot(clusterId, id);
        return R.ok();
    }
}
