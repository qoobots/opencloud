package com.qoobot.opencloud.monitor.metrics.controller;

import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 指标查询控制器
 */
@RestController
@RequestMapping("/api/monitor/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * Prometheus 即时查询
     */
    @GetMapping("/query")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<Map<String, Object>> query(
            @RequestParam String query,
            @RequestParam(required = false) Long time) {
        return R.ok(metricsService.query(query, time));
    }

    /**
     * Prometheus 范围查询
     */
    @GetMapping("/query_range")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<Map<String, Object>> queryRange(
            @RequestParam String query,
            @RequestParam Long start,
            @RequestParam Long end,
            @RequestParam String step) {
        return R.ok(metricsService.queryRange(query, start, end, step));
    }

    /**
     * 集群总览指标
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<Map<String, Object>> overview() {
        return R.ok(metricsService.getClusterOverview());
    }

    /**
     * 节点指标列表
     */
    @GetMapping("/nodes")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<List<Map<String, Object>>> nodes() {
        return R.ok(metricsService.getNodeMetrics());
    }

    /**
     * 指标名称列表（用于 PromQL 补全）
     */
    @GetMapping("/labels")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<List<String>> labels(@RequestParam(required = false) String prefix) {
        return R.ok(metricsService.getLabelValues(prefix));
    }
}
