package com.qoobot.opencloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.auth.domain.entity.SysUserQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * sys_user 表查询 Mapper（auth 模块只读，单体架构共享 Schema）
 *
 * <p>登录时通过 username 查找用户 id（Long），再关联 auth_user_credential 表。
 */
@Mapper
public interface SysUserQueryMapper extends BaseMapper<SysUserQuery> {
}
