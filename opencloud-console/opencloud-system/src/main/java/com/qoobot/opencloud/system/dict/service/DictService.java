package com.qoobot.opencloud.system.dict.service;

import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.dict.domain.dto.DictDataDTO;
import com.qoobot.opencloud.system.dict.domain.dto.DictTypeDTO;
import com.qoobot.opencloud.system.dict.domain.vo.DictDataVO;
import com.qoobot.opencloud.system.dict.domain.vo.DictTypeVO;

import java.util.List;

/**
 * 字典管理 Service
 */
public interface DictService {

    /** 分页查询字典类型 */
    PageResult<DictTypeVO> pageDictTypes(long current, long size, String keyword);

    /** 新增字典类型 */
    void createDictType(DictTypeDTO dto);

    /** 编辑字典类型 */
    void updateDictType(Long id, DictTypeDTO dto);

    /** 删除字典类型 */
    void deleteDictType(Long id);

    /** 按类型查询字典数据列表 */
    List<DictDataVO> listDictData(String dictType);

    /** 新增字典数据 */
    void createDictData(DictDataDTO dto);

    /** 编辑字典数据 */
    void updateDictData(Long id, DictDataDTO dto);

    /** 删除字典数据 */
    void deleteDictData(Long id);
}
