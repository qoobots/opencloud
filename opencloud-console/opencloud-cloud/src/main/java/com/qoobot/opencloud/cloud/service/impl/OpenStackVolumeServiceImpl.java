package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.dto.SnapshotCreateDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.VolumeCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeSnapshotVO;
import com.qoobot.opencloud.cloud.domain.vo.VolumeVO;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.service.OpenStackVolumeService;
import com.qoobot.opencloud.common.core.page.PageResult;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeAttachment;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenStack 存储服务实现
 */
@Service
public class OpenStackVolumeServiceImpl implements OpenStackVolumeService {

    @Autowired
    private CloudClientFactory clientFactory;

    @Override
    public PageResult<VolumeVO> listVolumes(String clusterId, String status, String name, int page, int size) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends Volume> volumes = os.blockStorage().volumes().list();

        // 过滤
        List<Volume> filtered = volumes.stream()
                .filter(v -> status == null || v.getStatus().name().equalsIgnoreCase(status))
                .filter(v -> name == null || (v.getName() != null && v.getName().toLowerCase().contains(name.toLowerCase())))
                .collect(Collectors.toList());

        // 分页
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Volume> pageList = start < total ? filtered.subList(start, end) : List.of();

        List<VolumeVO> voList = pageList.stream()
                .map(this::convertToVolumeVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public VolumeVO createVolume(VolumeCreateDTO dto) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(dto.getClusterId());

        VolumeBuilder builder = org.openstack4j.model.storage.block.builder.BlockStorageBuilders.volume()
                .name(dto.getName())
                .size(dto.getSize());

        if (dto.getDescription() != null) {
            builder.description(dto.getDescription());
        }

        if (dto.getVolumeType() != null) {
            builder.volumeType(dto.getVolumeType());
        }

        if (dto.getSnapshotId() != null) {
            builder.snapshot(dto.getSnapshotId());
        }

        if (dto.getImageId() != null) {
            builder.imageRef(dto.getImageId());
        }

        if (dto.getMetadata() != null) {
            builder.metadata(dto.getMetadata());
        }

        Volume volume = os.blockStorage().volumes().create(builder.build());
        return convertToVolumeVO(volume);
    }

    @Override
    public void deleteVolume(String clusterId, String volumeId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        Volume volume = os.blockStorage().volumes().get(volumeId);
        if (volume == null) {
            throw new CloudException("OS_0007", "卷不存在", HttpStatus.NOT_FOUND);
        }

        if (volume.getAttachments() != null && !volume.getAttachments().isEmpty()) {
            throw new CloudException("OS_0008", "卷已挂载，请先卸载");
        }

        os.blockStorage().volumes().delete(volumeId);
    }

    @Override
    public void performAction(String clusterId, String volumeId, VolumeActionDTO dto) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        switch (dto.getAction()) {
            case "attach":
                if (dto.getInstanceId() == null) {
                    throw new CloudException("OS_0009", "挂载操作需要提供云主机 ID");
                }
                os.compute().servers().attachVolume(dto.getInstanceId(), volumeId, dto.getDevice());
                break;
            case "detach":
                if (dto.getInstanceId() == null) {
                    throw new CloudException("OS_0009", "卸载操作需要提供云主机 ID");
                }
                os.compute().servers().detachVolume(dto.getInstanceId(), volumeId);
                break;
            case "extend":
                if (dto.getNewSize() == null) {
                    throw new CloudException("OS_0010", "扩容操作需要提供新大小");
                }
                os.blockStorage().volumes().extend(volumeId, dto.getNewSize());
                break;
            default:
                throw new CloudException("OS_0004", "不支持的操作类型: " + dto.getAction());
        }
    }

    @Override
    public PageResult<VolumeSnapshotVO> listSnapshots(String clusterId, String volumeId, int page, int size) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends VolumeSnapshot> snapshots = os.blockStorage().snapshots().list();

        // 过滤
        List<VolumeSnapshot> filtered = snapshots.stream()
                .filter(s -> volumeId == null || volumeId.equals(s.getVolumeId()))
                .collect(Collectors.toList());

        // 分页
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<VolumeSnapshot> pageList = start < total ? filtered.subList(start, end) : List.of();

        List<VolumeSnapshotVO> voList = pageList.stream()
                .map(this::convertToSnapshotVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public VolumeSnapshotVO createSnapshot(SnapshotCreateDTO dto) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(dto.getClusterId());

        org.openstack4j.model.storage.block.builder.SnapshotBuilder builder =
                org.openstack4j.model.storage.block.builder.BlockStorageBuilders.snapshot()
                        .volume(dto.getVolumeId())
                        .name(dto.getName())
                        .description(dto.getDescription());

        VolumeSnapshot snapshot = os.blockStorage().snapshots().create(builder.build());
        return convertToSnapshotVO(snapshot);
    }

    @Override
    public void deleteSnapshot(String clusterId, String snapshotId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        os.blockStorage().snapshots().delete(snapshotId);
    }

    private VolumeVO convertToVolumeVO(Volume volume) {
        VolumeVO vo = new VolumeVO();
        vo.setId(volume.getId());
        vo.setName(volume.getName());
        vo.setDescription(volume.getDescription());
        vo.setSize(volume.getSize());
        vo.setStatus(volume.getStatus().name());
        vo.setVolumeType(volume.getVolumeType());
        vo.setBootable(volume.bootable());
        vo.setEncrypted(volume.isEncrypted());
        vo.setAvailabilityZone(volume.getZone());
        vo.setProjectId(volume.getTenantId());
        vo.setCreatedAt(volume.getCreated());
        vo.setUpdatedAt(volume.getUpdated());

        if (volume.getAttachments() != null) {
            vo.setAttachments(volume.getAttachments().stream()
                    .map(this::convertToAttachmentVO)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    private VolumeVO.AttachmentVO convertToAttachmentVO(VolumeAttachment attachment) {
        VolumeVO.AttachmentVO vo = new VolumeVO.AttachmentVO();
        vo.setAttachmentId(attachment.getId());
        vo.setInstanceId(attachment.getServerId());
        vo.setDevice(attachment.getDevice());
        return vo;
    }

    private VolumeSnapshotVO convertToSnapshotVO(VolumeSnapshot snapshot) {
        VolumeSnapshotVO vo = new VolumeSnapshotVO();
        vo.setId(snapshot.getId());
        vo.setName(snapshot.getName());
        vo.setDescription(snapshot.getDescription());
        vo.setVolumeId(snapshot.getVolumeId());
        vo.setSize(snapshot.getSize());
        vo.setStatus(snapshot.getStatus().name());
        vo.setProjectId(snapshot.getTenantId());
        vo.setCreatedAt(snapshot.getCreated());
        vo.setUpdatedAt(snapshot.getUpdated());
        return vo;
    }
}
