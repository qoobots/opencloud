package com.qoobot.opencloud.system.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.log.domain.dto.LoginLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.entity.AuthLoginLog;
import com.qoobot.opencloud.system.log.domain.vo.LoginLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录日志只读 Mapper（跨 Schema 访问 auth.auth_login_log）
 */
@Mapper
public interface AuthLoginLogMapper extends BaseMapper<AuthLoginLog> {

    /**
     * 分页查询登录日志
     */
    IPage<LoginLogVO> selectPageVO(Page<LoginLogVO> page, @Param("q") LoginLogQueryDTO query, @Param("tenantId") String tenantId);

    /**
     * 导出登录日志（最大 10000 条）
     */
    List<LoginLogVO> selectForExport(@Param("q") LoginLogQueryDTO query, @Param("tenantId") String tenantId);
}
