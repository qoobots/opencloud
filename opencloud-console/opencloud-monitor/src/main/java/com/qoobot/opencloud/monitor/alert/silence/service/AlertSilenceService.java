package com.qoobot.opencloud.monitor.alert.silence.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceCreateDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceQueryDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.entity.MonitorAlertSilence;
import com.qoobot.opencloud.monitor.alert.silence.domain.vo.AlertSilenceVO;

import java.util.Map;
import java.util.List;

/**
 * 告警静默服务接口
 */
public interface AlertSilenceService {

    /** 分页查询静默规则 */
    IPage<AlertSilenceVO> listSilences(AlertSilenceQueryDTO queryDTO);

    /** 查询静默规则详情 */
    AlertSilenceVO getSilenceById(Long id);

    /** 创建静默规则 */
    AlertSilenceVO createSilence(AlertSilenceCreateDTO createDTO);

    /** 更新静默规则 */
    AlertSilenceVO updateSilence(Long id, AlertSilenceCreateDTO updateDTO);

    /** 停止静默（手动将状态置为 EXPIRED） */
    void stopSilence(Long id);

    /** 删除静默规则（仅允许删除已过期规则） */
    void deleteSilence(Long id);

    /**
     * 检查告警记录是否命中静默规则
     * @param labels 告警的 labels Map（含 alertname / severity / instance 等）
     * @return 是否被静默
     */
    boolean isAlertSilenced(Map<String, String> labels);
}
