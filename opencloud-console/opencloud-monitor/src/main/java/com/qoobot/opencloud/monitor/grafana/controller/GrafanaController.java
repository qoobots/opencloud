package com.qoobot.opencloud.monitor.grafana.controller;

import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.grafana.service.GrafanaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Grafana 集成控制器
 */
@RestController
@RequestMapping("/api/monitor/grafana")
@RequiredArgsConstructor
public class GrafanaController {

    private final GrafanaService grafanaService;

    /**
     * 面板列表
     */
    @GetMapping("/dashboards")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<List<Map<String, Object>>> listDashboards() {
        return R.ok(grafanaService.listDashboards());
    }

    /**
     * 面板嵌入 URL
     */
    @GetMapping("/dashboards/{uid}/embed")
    @PreAuthorize("hasAuthority('monitor:metrics:query')")
    public R<Map<String, String>> getEmbedUrl(
            @PathVariable String uid,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return R.ok(grafanaService.getEmbedUrl(uid, from, to));
    }
}
