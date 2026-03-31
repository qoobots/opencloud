package com.qoobot.opencloud.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.cloud.domain.entity.CloudQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配额配置 Mapper
 */
@Mapper
public interface CloudQuotaMapper extends BaseMapper<CloudQuota> {

    @Select("SELECT * FROM cloud.cloud_quota WHERE tenant_id = #{tenantId}")
    CloudQuota selectByTenantId(@Param("tenantId") String tenantId);
}
