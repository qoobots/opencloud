package com.qoobot.opencloud.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.cloud.domain.entity.ClusterConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 集群配置 Mapper
 */
@Mapper
public interface ClusterConfigMapper extends BaseMapper<ClusterConfig> {

    @Select("SELECT * FROM cloud.cloud_cluster_config WHERE tenant_id = #{tenantId} AND deleted = 0")
    List<ClusterConfig> selectByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM cloud.cloud_cluster_config WHERE status = 'ACTIVE' AND deleted = 0")
    List<ClusterConfig> selectActiveClusters();
}
