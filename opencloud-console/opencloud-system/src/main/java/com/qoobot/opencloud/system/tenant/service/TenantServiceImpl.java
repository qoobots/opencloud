package com.qoobot.opencloud.system.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.exception.SystemException;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantCreateDTO;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantUpdateDTO;
import com.qoobot.opencloud.system.tenant.domain.entity.SysTenant;
import com.qoobot.opencloud.system.tenant.domain.vo.TenantVO;
import com.qoobot.opencloud.system.tenant.mapper.SysTenantMapper;
import com.qoobot.opencloud.system.user.domain.dto.UserStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final SysTenantMapper tenantMapper;

    @Override
    public PageResult<TenantVO> pageTenants(long current, long size, String keyword) {
        Page<TenantVO> page = new Page<>(current, size);
        return PageResult.of(tenantMapper.selectPageVO(page, keyword));
    }

    @Override
    public TenantVO getTenantById(String id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw SystemException.tenantNotFound();
        TenantVO vo = new TenantVO();
        BeanUtils.copyProperties(tenant, vo);
        return vo;
    }

    @Override
    public List<TenantVO> getOptions() {
        return tenantMapper.selectList(
                new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getDeleted, 0)
                        .eq(SysTenant::getStatus, "ACTIVE")
        ).stream().map(t -> {
            TenantVO vo = new TenantVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTenant(TenantCreateDTO dto) {
        long count = tenantMapper.selectCount(
                new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getTenantCode, dto.getTenantCode())
                        .eq(SysTenant::getDeleted, 0)
        );
        if (count > 0) throw SystemException.tenantCodeExists();

        SysTenant tenant = new SysTenant();
        BeanUtils.copyProperties(dto, tenant);
        if (tenant.getStatus() == null) tenant.setStatus("ACTIVE");
        tenant.setIsDefault(false);
        tenantMapper.insert(tenant);

        log.info("新增租户: tenantCode={}", dto.getTenantCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(String id, TenantUpdateDTO dto) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw SystemException.tenantNotFound();
        BeanUtils.copyProperties(dto, tenant, "id", "tenantCode", "isDefault");
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(String id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw SystemException.tenantNotFound();
        if (Boolean.TRUE.equals(tenant.getIsDefault())) throw SystemException.defaultTenantNotAllowed();

        int userCount = tenantMapper.countUsersByTenantId(id);
        if (userCount > 0) throw SystemException.tenantHasUsers();

        tenantMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, UserStatusDTO dto) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) throw SystemException.tenantNotFound();

        tenantMapper.update(null,
                new LambdaUpdateWrapper<SysTenant>()
                        .eq(SysTenant::getId, id)
                        .set(SysTenant::getStatus, dto.getStatus()));
    }
}
