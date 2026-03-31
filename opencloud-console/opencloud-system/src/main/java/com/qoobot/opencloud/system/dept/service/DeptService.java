package com.qoobot.opencloud.system.dept.service;

import com.qoobot.opencloud.system.dept.domain.dto.DeptCreateDTO;
import com.qoobot.opencloud.system.dept.domain.dto.DeptUpdateDTO;
import com.qoobot.opencloud.system.dept.domain.vo.DeptOptionVO;
import com.qoobot.opencloud.system.dept.domain.vo.DeptTreeVO;

import java.util.List;

/**
 * 部门管理 Service
 */
public interface DeptService {

    /** 部门树（全量） */
    List<DeptTreeVO> getDeptTree();

    /** 部门下拉选项 */
    List<DeptOptionVO> getOptions();

    /** 新增部门 */
    void createDept(DeptCreateDTO dto);

    /** 编辑部门 */
    void updateDept(Long id, DeptUpdateDTO dto);

    /** 删除部门 */
    void deleteDept(Long id);
}
