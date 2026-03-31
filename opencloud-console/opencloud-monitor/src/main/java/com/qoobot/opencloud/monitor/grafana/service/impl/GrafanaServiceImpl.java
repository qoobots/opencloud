package com.qoobot.opencloud.monitor.grafana.service.impl;

import com.qoobot.opencloud.monitor.grafana.client.GrafanaClient;
import com.qoobot.opencloud.monitor.grafana.service.GrafanaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafana 集成服务实现
 */
@Service
@RequiredArgsConstructor
public class GrafanaServiceImpl implements GrafanaService {

    private final GrafanaClient grafanaClient;

    @Override
    public List<Map<String, Object>> listDashboards() {
        return grafanaClient.searchDashboards();
    }

    @Override
    public Map<String, String> getEmbedUrl(String uid, String from, String to) {
        String embedUrl = grafanaClient.buildEmbedUrl(uid, from, to);
        Map<String, String> result = new HashMap<>();
        result.put("embedUrl", embedUrl);
        return result;
    }
}
