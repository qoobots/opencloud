package com.qoobot.opencloud.monitor.metrics.client;

import com.qoobot.opencloud.monitor.exception.MonitorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Prometheus HTTP API 客户端
 */
@Slf4j
@Component
public class PrometheusClient {

    @Value("${opencloud.prometheus.url:http://localhost:9090}")
    private String prometheusUrl;

    @Value("${opencloud.prometheus.timeout:10000}")
    private long timeoutMs;

    @Value("${opencloud.prometheus.max-range-days:30}")
    private int maxRangeDays;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(prometheusUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * 即时查询：GET /api/v1/query
     */
    public Map<String, Object> query(String expr, Long time) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/query")
                            .queryParam("query", expr)
                            .queryParamIfPresent("time", Optional.ofNullable(time))
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> MonitorException.promqlError(body)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorMap(TimeoutException.class, e -> MonitorException.queryTimeout())
                    .onErrorMap(WebClientRequestException.class, e -> {
                        log.error("Prometheus 连接失败: {}", e.getMessage());
                        return MonitorException.prometheusUnavailable();
                    })
                    .block();
        } catch (MonitorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Prometheus 查询异常: {}", e.getMessage());
            throw MonitorException.prometheusUnavailable();
        }
    }

    /**
     * 范围查询：GET /api/v1/query_range
     */
    public Map<String, Object> queryRange(String expr, long start, long end, String step) {
        // 时间范围校验
        if ((end - start) > (long) maxRangeDays * 24 * 3600) {
            throw MonitorException.timeRangeExceeded();
        }

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/query_range")
                            .queryParam("query", expr)
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("step", step)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> MonitorException.promqlError(body)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorMap(TimeoutException.class, e -> MonitorException.queryTimeout())
                    .onErrorMap(WebClientRequestException.class, e -> MonitorException.prometheusUnavailable())
                    .block();
        } catch (MonitorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Prometheus 范围查询异常: {}", e.getMessage());
            throw MonitorException.prometheusUnavailable();
        }
    }

    /**
     * 获取指标名称列表：GET /api/v1/label/__name__/values
     */
    @SuppressWarnings("unchecked")
    public List<String> labelValues(String prefix) {
        try {
            Map<String, Object> resp = webClient.get()
                    .uri("/api/v1/label/__name__/values")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorMap(TimeoutException.class, e -> MonitorException.queryTimeout())
                    .onErrorMap(WebClientRequestException.class, e -> MonitorException.prometheusUnavailable())
                    .block();

            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            List<String> all = (List<String>) data.getOrDefault("result", List.of());

            if (prefix != null && !prefix.isEmpty()) {
                return all.stream()
                        .filter(s -> s.startsWith(prefix))
                        .collect(Collectors.toList());
            }
            return all;
        } catch (MonitorException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取指标名称列表异常: {}", e.getMessage());
            throw MonitorException.prometheusUnavailable();
        }
    }

    /**
     * 校验 PromQL 语法（试执行）
     */
    public void validateExpr(String expr) {
        try {
            Map<String, Object> result = query(expr, null);
            String status = (String) result.get("status");
            if (!"success".equals(status)) {
                throw MonitorException.invalidPromql("Prometheus 返回错误状态");
            }
        } catch (MonitorException e) {
            if (e.getCode() == 1002) { // PromQL 错误
                throw MonitorException.invalidPromql(e.getMessage());
            }
            // Prometheus 不可达时不阻塞规则创建
            log.warn("Prometheus 不可达，跳过 PromQL 校验: {}", e.getMessage());
        }
    }

    /**
     * 查询节点指标
     */
    public Map<String, Object> queryNodeMetrics() {
        // CPU 使用率
        String cpuExpr = "rate(node_cpu_seconds_total{mode!=\"idle\"}[5m])";
        // 内存使用率
        String memExpr = "1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes";
        // 磁盘使用率
        String diskExpr = "(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes";
        // 在线状态
        String upExpr = "up{job=\"node\"}";

        Map<String, Object> result = new java.util.HashMap<>();
        try {
            result.put("cpu", query(cpuExpr, null));
        } catch (Exception e) {
            log.warn("查询 CPU 指标失败: {}", e.getMessage());
        }
        try {
            result.put("memory", query(memExpr, null));
        } catch (Exception e) {
            log.warn("查询内存指标失败: {}", e.getMessage());
        }
        try {
            result.put("disk", query(diskExpr, null));
        } catch (Exception e) {
            log.warn("查询磁盘指标失败: {}", e.getMessage());
        }
        try {
            result.put("up", query(upExpr, null));
        } catch (Exception e) {
            log.warn("查询在线状态失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 查询集群总览指标
     */
    public Map<String, Object> queryClusterOverview() {
        // CPU 使用率
        String cpuExpr = "1 - avg(rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))";
        // 内存使用率
        String memExpr = "1 - avg(node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)";
        // 磁盘使用率
        String diskExpr = "avg((node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes)";
        // 网络吞吐
        String netExpr = "sum(rate(node_network_receive_bytes_total[5m])) + sum(rate(node_network_transmit_bytes_total[5m]))";

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        try {
            Map<String, Object> cpuResult = query(cpuExpr, null);
            result.put("cpuUsage", extractValue(cpuResult));
        } catch (Exception e) {
            log.warn("查询 CPU 总览失败: {}", e.getMessage());
            result.put("cpuUsage", 0);
        }

        try {
            Map<String, Object> memResult = query(memExpr, null);
            result.put("memUsage", extractValue(memResult));
        } catch (Exception e) {
            log.warn("查询内存总览失败: {}", e.getMessage());
            result.put("memUsage", 0);
        }

        try {
            Map<String, Object> diskResult = query(diskExpr, null);
            result.put("diskUsage", extractValue(diskResult));
        } catch (Exception e) {
            log.warn("查询磁盘总览失败: {}", e.getMessage());
            result.put("diskUsage", 0);
        }

        try {
            Map<String, Object> netResult = query(netExpr, null);
            result.put("networkThroughput", extractValue(netResult));
        } catch (Exception e) {
            log.warn("查询网络吞吐失败: {}", e.getMessage());
            result.put("networkThroughput", 0);
        }

        return result;
    }

    /**
     * 从 Prometheus 响应中提取数值
     */
    @SuppressWarnings("unchecked")
    private double extractValue(Map<String, Object> result) {
        try {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
            if (results == null || results.isEmpty()) {
                return 0;
            }
            Map<String, Object> first = results.get(0);
            List<Object> values = (List<Object>) first.get("value");
            if (values == null || values.size() < 2) {
                return 0;
            }
            String valueStr = (String) values.get(1);
            return Double.parseDouble(valueStr);
        } catch (Exception e) {
            return 0;
        }
    }
}
