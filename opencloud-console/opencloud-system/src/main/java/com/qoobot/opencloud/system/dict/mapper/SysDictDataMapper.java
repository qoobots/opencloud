package com.qoobot.opencloud.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.system.dict.domain.entity.SysDictData;
import com.qoobot.opencloud.system.dict.domain.vo.DictDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据 Mapper
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /**
     * 按字典类型查询字典数据列表
     */
    List<DictDataVO> selectByDictType(@Param("dictType") String dictType);
}
