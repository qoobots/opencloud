package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.vo.ImageVO;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.service.OpenStackImageService;
import com.qoobot.opencloud.common.core.page.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.storage.block.builder.ImageUploadBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenStack 镜像服务实现
 */
@Slf4j
@Service
public class OpenStackImageServiceImpl implements OpenStackImageService {

    @Autowired
    private CloudClientFactory clientFactory;

    @Override
    public PageResult<ImageVO> listImages(String clusterId, String visibility, String name, int page, int size) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends Image> images = os.imagesV2().list();

        // 过滤
        List<Image> filtered = images.stream()
                .filter(img -> visibility == null ||
                        ("public".equals(visibility) && img.isPublic()) ||
                        ("private".equals(visibility) && !img.isPublic()))
                .filter(img -> name == null || img.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());

        // 分页
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Image> pageList = start < total ? filtered.subList(start, end) : List.of();

        List<ImageVO> voList = pageList.stream()
                .map(this::convertToImageVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public void deleteImage(String clusterId, String imageId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        Image image = os.imagesV2().get(imageId);
        if (image == null) {
            throw new CloudException("OS_0003", "镜像不存在", HttpStatus.NOT_FOUND);
        }

        if (image.isProtected()) {
            throw new CloudException("OS_0006", "镜像受保护，无法删除");
        }

        os.imagesV2().delete(imageId);
    }

    @Override
    public ImageVO uploadImage(String clusterId, String name, InputStream inputStream, long size,
                               String containerFormat, String diskFormat, int minDisk, int minRam, boolean isPublic) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        try {
            // 创建镜像元数据
            Image image = os.imagesV2().create(
                    org.openstack4j.model.storage.image.builder.ImageBuilders.image()
                            .name(name)
                            .containerFormat(containerFormat)
                            .diskFormat(diskFormat)
                            .minDisk(minDisk)
                            .minRam(minRam)
                            .isPublic(isPublic)
                            .build()
            );

            if (image == null) {
                throw new CloudException("OS_0011", "创建镜像元数据失败");
            }

            // 上传镜像数据
            os.imagesV2().upload(image.getId(), inputStream, size);

            // 刷新并返回
            Image uploaded = os.imagesV2().get(image.getId());
            return convertToImageVO(uploaded);
        } catch (Exception e) {
            log.error("Failed to upload image {} in cluster {}", name, clusterId, e);
            throw new CloudException("OS_0012", "上传镜像失败: " + e.getMessage());
        }
    }

    private ImageVO convertToImageVO(Image image) {
        ImageVO vo = new ImageVO();
        vo.setId(image.getId());
        vo.setName(image.getName());
        vo.setStatus(image.getStatus().name());
        vo.setSize(image.getSize());
        vo.setContainerFormat(image.getContainerFormat());
        vo.setDiskFormat(image.getDiskFormat());
        vo.setMinDisk(image.getMinDisk());
        vo.setMinRam(image.getMinRam());
        vo.setIsPublic(image.isPublic());
        vo.setIsProtected(image.isProtected());
        vo.setChecksum(image.getChecksum());
        vo.setOwner(image.getOwner());
        vo.setCreatedAt(image.getCreatedAt());
        vo.setUpdatedAt(image.getUpdatedAt());
        return vo;
    }
}
