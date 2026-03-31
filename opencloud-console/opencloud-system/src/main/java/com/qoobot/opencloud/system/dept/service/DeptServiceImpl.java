package com.qoobot.opencloud.system.dept.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qoobot.opencloud.system.common.util.TreeBuilder;
import com.qoobot.opencloud.system.config.TenantContext;
import com.qoobot.opencloud.system.dept.domain.dto.DeptCreateDTO;
import com.qoobot.opencloud.system.dept.domain.dto.DeptUpdateDTO;
import com.qoobot.opencloud.system.dept.domain.entity.SysDept;
import com.qoobot.opencloud.system.dept.domain.vo.DeptOptionVO;
import com.qoobot.opencloud.system.dept.domain.vo.DeptTreeVO;
import com.qoobot.opencloud.system.dept.mapper.SysDeptMapper;
import com.qoobot.opencloud.system.exception.SystemException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<DeptTreeVO> getDeptTree() {
        String tenantId = TenantContext.getTenantId();
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getTenantId, tenantId)
                        .eq(SysDept::getDeleted, 0)
                        .orderByAsc(SysDept::getSort)
        );
        List<DeptTreeVO> nodes = all.stream().map(this::toTreeVO).toList();
        return TreeBuilder.build(nodes, 0L);
    }

    @Override
    public List<DeptOptionVO> getOptions() {
        String tenantId = TenantContext.getTenantId();
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getTenantId, tenantId)
                        .eq(SysDept::getDeleted, 0)
                        .eq(SysDept::getStatus, "ACTIVE")
        ).stream().map(d -> {
            DeptOptionVO vo = new DeptOptionVO();
            vo.setId(d.getId());
            vo.setDeptName(d.getDeptName());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(DeptCreateDTO dto) {
        // 校验层级不超过 5
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            int depth = deptMapper.countDepth(dto.getParentId());
            if (depth >= 4) throw SystemException.deptLevelExceeded(); // 父节点已有 4 层，加上子节点就是 5 层
        }

        SysDept dept = new SysDept();
        BeanUtils.copyProperties(dto, dept);
        dept.setTenantId(TenantContext.getTenantId());
        deptMapper.insert(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long id, DeptUpdateDTO dto) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) throw SystemException.deptNotFound();
        BeanUtils.copyProperties(dto, dept, "id", "tenantId", "parentId");
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) throw SystemException.deptNotFound();

        int childCount = deptMapper.countChildren(id);
        if (childCount > 0) throw SystemException.deptHasChildren();

        int userCount = deptMapper.countUsersByDeptId(id);
        if (userCount > 0) throw SystemException.deptHasUsers();

        deptMapper.deleteById(id);
    }

    private DeptTreeVO toTreeVO(SysDept d) {
        DeptTreeVO vo = new DeptTreeVO();
        BeanUtils.copyProperties(d, vo);
        return vo;
    }
}
