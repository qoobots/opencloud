package com.qoobot.opencloud.system.profile.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qoobot.opencloud.system.exception.SystemException;
import com.qoobot.opencloud.system.profile.domain.dto.ProfileUpdateDTO;
import com.qoobot.opencloud.system.profile.domain.vo.ProfileVO;
import com.qoobot.opencloud.system.role.domain.entity.SysRole;
import com.qoobot.opencloud.system.role.domain.entity.SysUserRole;
import com.qoobot.opencloud.system.role.mapper.SysRoleMapper;
import com.qoobot.opencloud.system.role.mapper.SysUserRoleMapper;
import com.qoobot.opencloud.system.user.domain.entity.SysUser;
import com.qoobot.opencloud.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 个人中心 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final SysUserMapper     userMapper;
    private final SysRoleMapper     roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public ProfileVO getProfile() {
        String userId = getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw SystemException.userNotFound();

        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        // 手机号脱敏
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setAvatar(user.getAvatar());

        // 查询角色
        List<Long> roleIds = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        ).stream().map(SysUserRole::getRoleId).toList();

        if (!roleIds.isEmpty()) {
            List<String> roleNames = roleMapper.selectBatchIds(roleIds)
                    .stream().map(SysRole::getRoleName).toList();
            vo.setRoleNames(roleNames);
        } else {
            vo.setRoleNames(Collections.emptyList());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(ProfileUpdateDTO dto) {
        String userId = getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw SystemException.userNotFound();

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId);
        if (dto.getNickname() != null) wrapper.set(SysUser::getNickname, dto.getNickname());
        if (dto.getEmail() != null)    wrapper.set(SysUser::getEmail, dto.getEmail());
        if (dto.getPhone() != null)    wrapper.set(SysUser::getPhone, dto.getPhone());
        if (dto.getAvatar() != null)   wrapper.set(SysUser::getAvatar, dto.getAvatar());
        userMapper.update(null, wrapper);
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String uid) return uid;
        return null;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
