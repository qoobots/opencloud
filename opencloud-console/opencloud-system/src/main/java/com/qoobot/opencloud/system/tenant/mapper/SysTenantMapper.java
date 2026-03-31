package com.qoobot.opencloud.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.tenant.domain.entity.SysTenant;
import com.qoobot.opencloud.system.tenant.domain.vo.TenantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 租户 Mapper
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 分页查询租户
     */
    IPage<TenantVO> selectPageVO(Page<TenantVO> page, @Param("keyword") String keyword);

    /**
     * 查询租户下的用户数量（删除前校验）
     */
    int countUsersByTenantId(@Param("tenantId") String tenantId);
}
