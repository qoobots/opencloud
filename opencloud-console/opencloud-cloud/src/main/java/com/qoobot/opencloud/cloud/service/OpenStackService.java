package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.dto.InstanceActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.InstanceCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.FlavorVO;
import com.qoobot.opencloud.cloud.domain.vo.InstanceVO;
import com.qoobot.opencloud.cloud.domain.vo.KeyPairVO;
import com.qoobot.opencloud.common.core.page.PageResult;

import java.util.List;

/**
 * OpenStack 服务接口
 */
public interface OpenStackService {

    /**
     * 获取云主机列表
     */
    PageResult<InstanceVO> listInstances(String clusterId, String status, String name, int page, int size);

    /**
     * 获取云主机详情
     */
    InstanceVO getInstance(String clusterId, String instanceId);

    /**
     * 创建云主机
     */
    String createInstance(InstanceCreateDTO dto);

    /**
     * 执行云主机操作
     */
    void performAction(String clusterId, String instanceId, InstanceActionDTO dto);

    /**
     * 删除云主机
     */
    void deleteInstance(String clusterId, String instanceId, boolean force);

    /**
     * 获取规格列表
     */
    List<FlavorVO> listFlavors(String clusterId);

    /**
     * 获取密钥对列表
     */
    List<KeyPairVO> listKeyPairs(String clusterId);

    /**
     * 创建密钥对
     */
    KeyPairVO createKeyPair(String clusterId, String name, String publicKey);

    /**
     * 删除密钥对
     */
    void deleteKeyPair(String clusterId, String name);
}
