package com.qoobot.opencloud.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * 权限查询 Mapper —— 跨模块查 system 表权限（单体架构，直接共享 Schema）
 */
@Mapper
public interface PermissionMapper {

    /**
     * 查询用户拥有的所有权限标识（通过角色中间表关联）
     * SQL 定义在 PermissionMapper.xml 中
     */
    Set<String> selectPermissionsByUserId(@Param("userId") String userId);

    /**
     * 查询用户拥有的角色 code 列表
     */
    Set<String> selectRoleCodesByUserId(@Param("userId") String userId);
}
