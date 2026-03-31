package com.qoobot.opencloud.system.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.user.domain.dto.*;
import com.qoobot.opencloud.system.user.domain.vo.UserDetailVO;
import com.qoobot.opencloud.system.user.domain.vo.UserOptionVO;
import com.qoobot.opencloud.system.user.domain.vo.UserVO;

import java.util.List;

/**
 * 用户管理 Service
 */
public interface UserService {

    /** 分页查询用户 */
    PageResult<UserVO> pageUsers(long current, long size, UserQueryDTO query);

    /** 查询用户详情（含角色列表） */
    UserDetailVO getUserById(String id);

    /** 查询用户下拉选项 */
    List<UserOptionVO> getOptions();

    /** 新增用户 */
    void createUser(UserCreateDTO dto);

    /** 编辑用户基本信息 */
    void updateUser(String id, UserUpdateDTO dto);

    /** 删除用户（逻辑删除） */
    void deleteUser(String id);

    /** 切换用户状态 */
    void updateStatus(String id, UserStatusDTO dto);

    /** 重置用户密码 */
    void resetPassword(String id, UserResetPasswordDTO dto);

    /** 分配用户角色 */
    void assignRoles(String id, UserRoleDTO dto);
}
