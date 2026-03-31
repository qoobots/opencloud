package com.qoobot.opencloud.system.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.common.cache.AuthPermCacheHelper;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.config.TenantContext;
import com.qoobot.opencloud.system.exception.SystemException;
import com.qoobot.opencloud.system.role.domain.dto.RoleCreateDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleMenuDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleUpdateDTO;
import com.qoobot.opencloud.system.role.domain.entity.SysRole;
import com.qoobot.opencloud.system.role.domain.entity.SysRoleMenu;
import com.qoobot.opencloud.system.role.domain.vo.RoleOptionVO;
import com.qoobot.opencloud.system.role.domain.vo.RoleVO;
import com.qoobot.opencloud.system.role.mapper.SysRoleMapper;
import com.qoobot.opencloud.system.role.mapper.SysRoleMenuMapper;
import com.qoobot.opencloud.system.role.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper       roleMapper;
    private final SysRoleMenuMapper   roleMenuMapper;
    private final SysUserRoleMapper   userRoleMapper;
    private final AuthPermCacheHelper permCacheHelper;

    @Override
    public PageResult<RoleVO> pageRoles(long current, long size, String keyword) {
        Page<RoleVO> page = new Page<>(current, size);
        return PageResult.of(roleMapper.selectPageVO(page, keyword));
    }

    @Override
    public List<RoleOptionVO> getOptions() {
        String tenantId = TenantContext.getTenantId();
        return roleMapper.selectOptions(tenantId);
    }

    @Override
    public RoleVO getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw SystemException.roleNotFound();
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateDTO dto) {
        String tenantId = TenantContext.getTenantId();

        // 校验 roleCode 唯一性
        long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, dto.getRoleCode())
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDeleted, 0)
        );
        if (count > 0) throw SystemException.roleCodeExists();

        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        role.setTenantId(tenantId);
        role.setIsBuiltin(false);
        if (role.getStatus() == null) role.setStatus("ACTIVE");
        if (role.getSort() == null) role.setSort(0);
        roleMapper.insert(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw SystemException.roleNotFound();
        if (Boolean.TRUE.equals(role.getIsBuiltin()) && dto.getStatus() != null) {
            // 内置角色允许改名/描述，但不允许改 code（已无 code 字段在更新 DTO 中）
        }
        BeanUtils.copyProperties(dto, role, "id", "tenantId", "roleCode", "isBuiltin");
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw SystemException.roleNotFound();
        if (Boolean.TRUE.equals(role.getIsBuiltin())) throw SystemException.builtinRoleNotAllowed();

        // 检查角色下是否有用户
        int userCount = userRoleMapper.countByRoleId(id);
        if (userCount > 0) throw SystemException.roleInUse();

        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long id, RoleMenuDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw SystemException.roleNotFound();

        // 查询该角色下所有用户（用于清缓存）
        List<String> userIds = roleMenuMapper.selectUserIdsByRoleId(id);

        // 删除旧绑定
        roleMenuMapper.deleteByRoleId(id);

        // 批量写入新绑定
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            for (Long menuId : dto.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(id);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }

        // 批量清除权限缓存
        permCacheHelper.evictPerms(userIds);
        log.info("分配菜单: roleId={}, menuCount={}", id, dto.getMenuIds() == null ? 0 : dto.getMenuIds().size());
    }

    @Override
    public List<Long> getMenuIds(Long id) {
        return roleMenuMapper.selectMenuIdsByRoleId(id);
    }
}
