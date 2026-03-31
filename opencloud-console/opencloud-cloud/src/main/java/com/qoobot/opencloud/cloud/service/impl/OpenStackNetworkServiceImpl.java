package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.vo.FloatingIpVO;
import com.qoobot.opencloud.cloud.domain.vo.NetworkVO;
import com.qoobot.opencloud.cloud.domain.vo.SecurityGroupVO;
import com.qoobot.opencloud.cloud.service.OpenStackNetworkService;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenStack 网络服务实现
 */
@Service
public class OpenStackNetworkServiceImpl implements OpenStackNetworkService {

    @Autowired
    private CloudClientFactory clientFactory;

    @Override
    public List<NetworkVO> listNetworks(String clusterId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends Network> networks = os.networking().network().list();

        return networks.stream()
                .map(this::convertToNetworkVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NetworkVO.SubnetVO> listSubnets(String clusterId, String networkId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        List<? extends Subnet> subnets;
        if (networkId != null) {
            subnets = os.networking().subnet().list().stream()
                    .filter(s -> networkId.equals(s.getNetworkId()))
                    .collect(Collectors.toList());
        } else {
            subnets = os.networking().subnet().list();
        }

        return subnets.stream()
                .map(this::convertToSubnetVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SecurityGroupVO> listSecurityGroups(String clusterId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends SecurityGroup> groups = os.networking().securitygroup().list();

        return groups.stream()
                .map(this::convertToSecurityGroupVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FloatingIpVO> listFloatingIps(String clusterId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends NetFloatingIP> floatingIPs = os.networking().floatingip().list();

        return floatingIPs.stream()
                .map(this::convertToFloatingIpVO)
                .collect(Collectors.toList());
    }

    private NetworkVO convertToNetworkVO(Network network) {
        NetworkVO vo = new NetworkVO();
        vo.setId(network.getId());
        vo.setName(network.getName());
        vo.setStatus(network.getStatus().name());
        vo.setAdminStateUp(network.isAdminStateUp());
        vo.setShared(network.isShared());
        vo.setExternal(network.isRouterExternal());
        vo.setProjectId(network.getTenantId());
        vo.setCreatedAt(network.getCreatedAt());
        vo.setUpdatedAt(network.getUpdatedAt());

        // 加载子网信息
        if (network.getSubnets() != null) {
            vo.setSubnets(network.getSubnets().stream()
                    .map(subnetId -> {
                        // 简化处理，实际需要查询子网详情
                        NetworkVO.SubnetVO subnetVO = new NetworkVO.SubnetVO();
                        subnetVO.setId(subnetId);
                        return subnetVO;
                    })
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    private NetworkVO.SubnetVO convertToSubnetVO(Subnet subnet) {
        NetworkVO.SubnetVO vo = new NetworkVO.SubnetVO();
        vo.setId(subnet.getId());
        vo.setName(subnet.getName());
        vo.setCidr(subnet.getCidr());
        vo.setGatewayIp(subnet.getGateway());
        vo.setDhcpEnabled(subnet.isDHCPEnabled());
        vo.setDnsNameservers(subnet.getDnsNames());
        vo.setIpVersion("IPv" + subnet.getIpVersion());
        return vo;
    }

    private SecurityGroupVO convertToSecurityGroupVO(SecurityGroup group) {
        SecurityGroupVO vo = new SecurityGroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setDescription(group.getDescription());
        vo.setProjectId(group.getTenantId());
        vo.setCreatedAt(group.getCreatedAt());
        vo.setUpdatedAt(group.getUpdatedAt());

        if (group.getRules() != null) {
            vo.setRules(group.getRules().stream()
                    .map(this::convertToRuleVO)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    private SecurityGroupVO.RuleVO convertToRuleVO(SecurityGroupRule rule) {
        SecurityGroupVO.RuleVO vo = new SecurityGroupVO.RuleVO();
        vo.setId(rule.getId());
        vo.setDirection(rule.getDirection().name());
        vo.setProtocol(rule.getProtocol());
        vo.setEthertype(rule.getEthertype().name());
        vo.setPortRangeMin(rule.getPortRangeMin());
        vo.setPortRangeMax(rule.getPortRangeMax());
        vo.setRemoteIpPrefix(rule.getRemoteIpPrefix());
        vo.setRemoteGroupId(rule.getRemoteGroupId());
        return vo;
    }

    private FloatingIpVO convertToFloatingIpVO(NetFloatingIP floatingIP) {
        FloatingIpVO vo = new FloatingIpVO();
        vo.setId(floatingIP.getId());
        vo.setFloatingIpAddress(floatingIP.getFloatingIpAddress());
        vo.setFixedIpAddress(floatingIP.getFixedIpAddress());
        vo.setPortId(floatingIP.getPortId());
        vo.setNetworkId(floatingIP.getFloatingNetworkId());
        vo.setRouterId(floatingIP.getRouterId());
        return vo;
    }
}
