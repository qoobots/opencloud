package com.qoobot.opencloud.monitor.metrics.service.impl;

import com.qoobot.opencloud.monitor.metrics.client.PrometheusClient;
import com.qoobot.opencloud.monitor.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private final PrometheusClient prometheusClient;

    @Override
    public Map<String, Object> query(String expr, Long time) {
        return prometheusClient.query(expr, time);
    }

    @Override
    public Map<String, Object> queryRange(String expr, long start, long end, String step) {
        return prometheusClient.queryRange(expr, start, end, step);
    }

    @Override
    public Map<String, Object> getClusterOverview() {
        return prometheusClient.queryClusterOverview();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNodeMetrics() {
        Map<String, Object> metrics = prometheusClient.queryNodeMetrics();
        List<Map<String, Object>> nodeList = new ArrayList<>();

        // 解析在线状态
        Map<String, Object> upResult = (Map<String, Object>) metrics.get("up");
        Map<String, Boolean> onlineMap = new HashMap<>();
        if (upResult != null) {
            Map<String, Object> data = (Map<String, Object>) upResult.get("data");
            if (data != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
                if (results != null) {
                    for (Map<String, Object> item : results) {
                        Map<String, String> metric = (Map<String, String>) item.get("metric");
                        List<Object> values = (List<Object>) item.get("value");
                        if (metric != null && values != null && values.size() > 1) {
                            String instance = metric.get("instance");
                            double value = Double.parseDouble((String) values.get(1));
                            onlineMap.put(instance, value > 0);
                        }
                    }
                }
            }
        }

        // 解析 CPU 使用率
        Map<String, Double> cpuMap = extractInstanceValues((Map<String, Object>) metrics.get("cpu"));
        // 解析内存使用率
        Map<String, Double> memMap = extractInstanceValues((Map<String, Object>) metrics.get("memory"));
        // 解析磁盘使用率
        Map<String, Double> diskMap = extractInstanceValues((Map<String, Object>) metrics.get("disk"));

        // 合并所有实例
        for (String instance : onlineMap.keySet()) {
            Map<String, Object> node = new HashMap<>();
            node.put("instance", instance);
            node.put("online", onlineMap.getOrDefault(instance, false));
            node.put("cpuUsage", cpuMap.getOrDefault(instance, 0.0));
            node.put("memUsage", memMap.getOrDefault(instance, 0.0));
            node.put("diskUsage", diskMap.getOrDefault(instance, 0.0));
            nodeList.add(node);
        }

        return nodeList;
    }

    @Override
    public List<String> getLabelValues(String prefix) {
        return prometheusClient.labelValues(prefix);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> extractInstanceValues(Map<String, Object> result) {
        Map<String, Double> map = new HashMap<>();
        if (result == null) {
            return map;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return map;
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
        if (results == null) {
            return map;
        }
        for (Map<String, Object> item : results) {
            Map<String, String> metric = (Map<String, String>) item.get("metric");
            List<Object> values = (List<Object>) item.get("value");
            if (metric != null && values != null && values.size() > 1) {
                String instance = metric.get("instance");
                try {
                    double value = Double.parseDouble((String) values.get(1));
                    map.put(instance, value);
                } catch (NumberFormatException e) {
                    log.warn("解析指标值失败: {}", values.get(1));
                }
            }
        }
        return map;
    }
}
