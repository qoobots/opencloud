package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.vo.FloatingIpVO;
import com.qoobot.opencloud.cloud.domain.vo.NetworkVO;
import com.qoobot.opencloud.cloud.domain.vo.SecurityGroupVO;

import java.util.List;

/**
 * OpenStack 网络服务接口
 */
public interface OpenStackNetworkService {

    /**
     * 获取网络列表
     */
    List<NetworkVO> listNetworks(String clusterId);

    /**
     * 获取子网列表
     */
    List<NetworkVO.SubnetVO> listSubnets(String clusterId, String networkId);

    /**
     * 获取安全组列表
     */
    List<SecurityGroupVO> listSecurityGroups(String clusterId);

    /**
     * 获取浮动 IP 列表
     */
    List<FloatingIpVO> listFloatingIps(String clusterId);
}
