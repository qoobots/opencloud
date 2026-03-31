package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.vo.FloatingIpVO;
import com.qoobot.opencloud.cloud.domain.vo.NetworkVO;
import com.qoobot.opencloud.cloud.domain.vo.SecurityGroupVO;
import com.qoobot.opencloud.cloud.service.OpenStackNetworkService;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * OpenStack 网络管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/openstack")
public class OpenStackNetworkController {

    @Autowired
    private OpenStackNetworkService networkService;

    /**
     * 网络列表
     */
    @GetMapping("/networks")
    public R<List<NetworkVO>> listNetworks(@RequestParam @NotBlank String clusterId) {
        return R.ok(networkService.listNetworks(clusterId));
    }

    /**
     * 子网列表
     */
    @GetMapping("/subnets")
    public R<List<NetworkVO.SubnetVO>> listSubnets(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String networkId) {
        return R.ok(networkService.listSubnets(clusterId, networkId));
    }

    /**
     * 安全组列表
     */
    @GetMapping("/security-groups")
    public R<List<SecurityGroupVO>> listSecurityGroups(@RequestParam @NotBlank String clusterId) {
        return R.ok(networkService.listSecurityGroups(clusterId));
    }

    /**
     * 浮动 IP 列表
     */
    @GetMapping("/floating-ips")
    public R<List<FloatingIpVO>> listFloatingIps(@RequestParam @NotBlank String clusterId) {
        return R.ok(networkService.listFloatingIps(clusterId));
    }
}
