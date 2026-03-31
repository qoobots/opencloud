package com.qoobot.opencloud.monitor.grafana.service;

import java.util.List;
import java.util.Map;

/**
 * Grafana 集成服务接口
 */
public interface GrafanaService {

    /**
     * 获取面板列表
     */
    List<Map<String, Object>> listDashboards();

    /**
     * 获取面板嵌入 URL
     */
    Map<String, String> getEmbedUrl(String uid, String from, String to);
}
