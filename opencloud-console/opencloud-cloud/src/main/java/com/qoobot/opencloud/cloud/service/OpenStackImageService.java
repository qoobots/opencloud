package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.vo.ImageVO;
import com.qoobot.opencloud.common.core.page.PageResult;

import java.io.InputStream;

/**
 * OpenStack 镜像服务接口
 */
public interface OpenStackImageService {

    /**
     * 获取镜像列表
     */
    PageResult<ImageVO> listImages(String clusterId, String visibility, String name, int page, int size);

    /**
     * 删除镜像
     */
    void deleteImage(String clusterId, String imageId);

    /**
     * 上传镜像
     * @param clusterId 集群ID
     * @param name 镜像名称
     * @param inputStream 镜像文件流
     * @param size 文件大小
     * @param containerFormat 容器格式（bare/ami/ari/aki）
     * @param diskFormat 磁盘格式（qcow2/raw/vhd/vddk/vdi）
     * @param minDisk 最小磁盘大小(GB)
     * @param minRam 最小内存(MB)
     * @param isPublic 是否公开
     */
    ImageVO uploadImage(String clusterId, String name, InputStream inputStream, long size,
                        String containerFormat, String diskFormat, int minDisk, int minRam, boolean isPublic);
}
