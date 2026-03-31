package com.qoobot.opencloud.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.common.cache.AuthPermCacheHelper;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.config.TenantContext;
import com.qoobot.opencloud.system.exception.SystemException;
import com.qoobot.opencloud.system.role.domain.entity.SysUserRole;
import com.qoobot.opencloud.system.role.mapper.SysUserRoleMapper;
import com.qoobot.opencloud.system.user.domain.dto.*;
import com.qoobot.opencloud.system.user.domain.entity.SysUser;
import com.qoobot.opencloud.system.user.domain.vo.UserDetailVO;
import com.qoobot.opencloud.system.user.domain.vo.UserOptionVO;
import com.qoobot.opencloud.system.user.domain.vo.UserVO;
import com.qoobot.opencloud.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper      userMapper;
    private final SysUserRoleMapper  userRoleMapper;
    private final AuthPermCacheHelper permCacheHelper;
    private final PasswordEncoder    passwordEncoder;

    // 跨模块访问 auth schema 修改凭证（通过 JDBC 直接操作）
    private final UserCredentialUpdater credentialUpdater;

    @Override
    public PageResult<UserVO> pageUsers(long current, long size, UserQueryDTO query) {
        Page<UserVO> page = new Page<>(current, size);
        return PageResult.of(userMapper.selectPageVO(page, query));
    }

    @Override
    public UserDetailVO getUserById(String id) {
        UserDetailVO vo = new UserDetailVO();
        UserVO user = userMapper.selectDetailById(id);
        if (user == null) throw SystemException.userNotFound();

        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setDeptId(user.getDeptId());
        vo.setDeptName(user.getDeptName());
        vo.setStatus(user.getStatus());
        vo.setIsBuiltin(user.getIsBuiltin());
        vo.setCreatedAt(user.getCreatedAt());

        // 查询已分配角色
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        ).stream().map(SysUserRole::getRoleId).toList();
        vo.setRoleIds(roleIds);

        return vo;
    }

    @Override
    public List<UserOptionVO> getOptions() {
        String tenantId = TenantContext.getTenantId();
        return userMapper.selectOptions(tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto) {
        String tenantId = TenantContext.getTenantId();

        // 校验用户名唯一性
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getDeleted, 0)
        );
        if (count > 0) throw SystemException.usernameExists();

        // 写入 sys_user
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setDeptId(dto.getDeptId());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        user.setIsBuiltin(false);
        user.setSort(dto.getSort() != null ? dto.getSort() : 0);
        userMapper.insert(user);

        // 写入 auth_user_credential（跨 Schema）
        String passwordHash = passwordEncoder.encode(dto.getPassword());
        credentialUpdater.createCredential(user.getId(), tenantId, passwordHash);

        // 写入 sys_user_role
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            batchInsertUserRoles(user.getId(), dto.getRoleIds());
        }

        log.info("新增用户: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(String id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw SystemException.userNotFound();

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id);
        if (dto.getNickname() != null) wrapper.set(SysUser::getNickname, dto.getNickname());
        if (dto.getEmail() != null)    wrapper.set(SysUser::getEmail, dto.getEmail());
        if (dto.getPhone() != null)    wrapper.set(SysUser::getPhone, dto.getPhone());
        if (dto.getAvatar() != null)   wrapper.set(SysUser::getAvatar, dto.getAvatar());
        if (dto.getDeptId() != null)   wrapper.set(SysUser::getDeptId, dto.getDeptId());
        if (dto.getSort() != null)     wrapper.set(SysUser::getSort, dto.getSort());
        userMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw SystemException.userNotFound();
        if (Boolean.TRUE.equals(user.getIsBuiltin())) throw SystemException.builtinUserNotAllowed();

        // 校验不能删除自身
        String currentUserId = getCurrentUserId();
        if (id.equals(currentUserId)) throw SystemException.selfOperationNotAllowed();

        userMapper.deleteById(id);
        // 清除角色绑定
        userRoleMapper.deleteByUserId(id);
        // 清除权限缓存
        permCacheHelper.evictPerm(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, UserStatusDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw SystemException.userNotFound();
        if (Boolean.TRUE.equals(user.getIsBuiltin())) throw SystemException.builtinUserNotAllowed();

        String currentUserId = getCurrentUserId();
        if (id.equals(currentUserId)) throw SystemException.selfOperationNotAllowed();

        // 同步更新 sys_user 和 auth_user_credential
        userMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, id)
                        .set(SysUser::getStatus, dto.getStatus()));
        credentialUpdater.updateStatus(id, dto.getStatus());

        // 如果禁用，强制下线
        if ("DISABLED".equals(dto.getStatus())) {
            permCacheHelper.forceLogout(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String id, UserResetPasswordDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw SystemException.userNotFound();

        String passwordHash = passwordEncoder.encode(dto.getNewPassword());
        credentialUpdater.updatePasswordHash(id, passwordHash);
        // 强制下线（Token 失效）
        permCacheHelper.forceLogout(id);

        log.info("重置密码: userId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String id, UserRoleDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw SystemException.userNotFound();

        // 删除旧绑定
        userRoleMapper.deleteByUserId(id);

        // 批量写入新绑定
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            batchInsertUserRoles(id, dto.getRoleIds());
        }

        // 清除权限缓存
        permCacheHelper.evictPerm(id);
        log.info("分配角色: userId={}, roleIds={}", id, dto.getRoleIds());
    }

    private void batchInsertUserRoles(String userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return null;
    }
}
