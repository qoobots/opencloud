package com.qoobot.opencloud.system.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.dict.domain.dto.DictDataDTO;
import com.qoobot.opencloud.system.dict.domain.dto.DictTypeDTO;
import com.qoobot.opencloud.system.dict.domain.entity.SysDictData;
import com.qoobot.opencloud.system.dict.domain.entity.SysDictType;
import com.qoobot.opencloud.system.dict.domain.vo.DictDataVO;
import com.qoobot.opencloud.system.dict.domain.vo.DictTypeVO;
import com.qoobot.opencloud.system.dict.mapper.SysDictDataMapper;
import com.qoobot.opencloud.system.dict.mapper.SysDictTypeMapper;
import com.qoobot.opencloud.system.exception.SystemException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    @Override
    public PageResult<DictTypeVO> pageDictTypes(long current, long size, String keyword) {
        Page<DictTypeVO> page = new Page<>(current, size);
        return PageResult.of(dictTypeMapper.selectPageVO(page, keyword));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictType(DictTypeDTO dto) {
        long count = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<SysDictType>()
                        .eq(SysDictType::getDictType, dto.getDictType())
                        .eq(SysDictType::getDeleted, 0)
        );
        if (count > 0) throw SystemException.dictTypeCodeExists();

        SysDictType type = new SysDictType();
        BeanUtils.copyProperties(dto, type);
        type.setIsSystem(false);
        if (type.getStatus() == null) type.setStatus("ACTIVE");
        dictTypeMapper.insert(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictType(Long id, DictTypeDTO dto) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw SystemException.dictTypeNotFound();
        BeanUtils.copyProperties(dto, type, "id", "dictType", "isSystem");
        dictTypeMapper.updateById(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw SystemException.dictTypeNotFound();
        if (Boolean.TRUE.equals(type.getIsSystem())) throw SystemException.systemDictNotAllowed();
        dictTypeMapper.deleteById(id);
        // 级联删除字典数据
        dictDataMapper.delete(
                new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, type.getDictType())
        );
    }

    @Override
    public List<DictDataVO> listDictData(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictData(DictDataDTO dto) {
        // 校验字典类型存在
        long typeCount = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<SysDictType>()
                        .eq(SysDictType::getDictType, dto.getDictType())
                        .eq(SysDictType::getDeleted, 0)
        );
        if (typeCount == 0) throw SystemException.dictTypeNotFound();

        // 校验同类型下值唯一
        long count = dictDataMapper.selectCount(
                new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, dto.getDictType())
                        .eq(SysDictData::getDictValue, dto.getDictValue())
                        .eq(SysDictData::getDeleted, 0)
        );
        if (count > 0) throw SystemException.dictValueExists();

        SysDictData data = new SysDictData();
        BeanUtils.copyProperties(dto, data);
        if (data.getStatus() == null) data.setStatus("ACTIVE");
        if (data.getIsDefault() == null) data.setIsDefault(false);
        if (data.getSort() == null) data.setSort(0);
        dictDataMapper.insert(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(Long id, DictDataDTO dto) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) throw SystemException.dictTypeNotFound();
        BeanUtils.copyProperties(dto, data, "id", "dictType");
        dictDataMapper.updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(Long id) {
        if (dictDataMapper.selectById(id) == null) throw SystemException.dictTypeNotFound();
        dictDataMapper.deleteById(id);
    }
}
