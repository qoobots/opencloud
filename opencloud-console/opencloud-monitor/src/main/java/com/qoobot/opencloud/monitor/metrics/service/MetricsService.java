package com.qoobot.opencloud.monitor.metrics.service;

import java.util.List;
import java.util.Map;

/**
 * 指标查询服务接口
 */
public interface MetricsService {

    /**
     * Prometheus 即时查询
     */
    Map<String, Object> query(String expr, Long time);

    /**
     * Prometheus 范围查询
     */
    Map<String, Object> queryRange(String expr, long start, long end, String step);

    /**
     * 获取集群总览指标
     */
    Map<String, Object> getClusterOverview();

    /**
     * 获取节点指标列表
     */
    List<Map<String, Object>> getNodeMetrics();

    /**
     * 获取指标名称列表
     */
    List<String> getLabelValues(String prefix);
}
