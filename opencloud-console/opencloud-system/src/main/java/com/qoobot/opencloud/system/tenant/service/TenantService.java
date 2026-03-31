package com.qoobot.opencloud.system.tenant.service;

import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantCreateDTO;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantUpdateDTO;
import com.qoobot.opencloud.system.tenant.domain.vo.TenantVO;
import com.qoobot.opencloud.system.user.domain.dto.UserStatusDTO;

import java.util.List;

/**
 * 租户管理 Service
 */
public interface TenantService {

    /** 分页查询租户 */
    PageResult<TenantVO> pageTenants(long current, long size, String keyword);

    /** 查询租户详情 */
    TenantVO getTenantById(String id);

    /** 查询租户下拉选项 */
    List<TenantVO> getOptions();

    /** 新增租户 */
    void createTenant(TenantCreateDTO dto);

    /** 编辑租户 */
    void updateTenant(String id, TenantUpdateDTO dto);

    /** 删除租户 */
    void deleteTenant(String id);

    /** 切换租户状态 */
    void updateStatus(String id, UserStatusDTO dto);
}
