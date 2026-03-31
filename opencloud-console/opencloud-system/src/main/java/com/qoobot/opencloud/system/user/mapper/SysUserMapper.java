package com.qoobot.opencloud.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.user.domain.dto.UserQueryDTO;
import com.qoobot.opencloud.system.user.domain.entity.SysUser;
import com.qoobot.opencloud.system.user.domain.vo.UserOptionVO;
import com.qoobot.opencloud.system.user.domain.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户（关联部门名称）
     */
    IPage<UserVO> selectPageVO(Page<UserVO> page, @Param("q") UserQueryDTO query);

    /**
     * 查询用户下拉选项列表
     */
    List<UserOptionVO> selectOptions(@Param("tenantId") String tenantId);

    /**
     * 查询用户详情（含部门名称）
     */
    UserVO selectDetailById(@Param("id") String id);
}
