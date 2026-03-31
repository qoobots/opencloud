package com.qoobot.opencloud.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.dict.domain.entity.SysDictType;
import com.qoobot.opencloud.system.dict.domain.vo.DictTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型 Mapper
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    /**
     * 分页查询字典类型
     */
    IPage<DictTypeVO> selectPageVO(Page<DictTypeVO> page, @Param("keyword") String keyword);
}
