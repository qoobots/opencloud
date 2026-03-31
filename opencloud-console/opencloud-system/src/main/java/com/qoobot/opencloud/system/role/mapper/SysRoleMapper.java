package com.qoobot.opencloud.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.role.domain.entity.SysRole;
import com.qoobot.opencloud.system.role.domain.vo.RoleOptionVO;
import com.qoobot.opencloud.system.role.domain.vo.RoleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 分页查询角色
     */
    IPage<RoleVO> selectPageVO(Page<RoleVO> page, @Param("keyword") String keyword);

    /**
     * 查询全量角色选项（下拉用）
     */
    List<RoleOptionVO> selectOptions(@Param("tenantId") String tenantId);

    /**
     * 查询用户已分配的角色 ID 列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") String userId);
}
