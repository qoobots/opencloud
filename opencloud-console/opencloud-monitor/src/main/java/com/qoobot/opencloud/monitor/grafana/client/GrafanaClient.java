package com.qoobot.opencloud.monitor.grafana.client;

import com.qoobot.opencloud.monitor.exception.MonitorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Grafana HTTP API 客户端
 */
@Slf4j
@Component
public class GrafanaClient {

    @Value("${opencloud.grafana.url:http://localhost:3000}")
    private String grafanaUrl;

    @Value("${opencloud.grafana.token:}")
    private String grafanaToken;

    @Value("${opencloud.grafana.kiosk:true}")
    private boolean kioskMode;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(grafanaUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024));

        if (grafanaToken != null && !grafanaToken.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + grafanaToken);
        }

        this.webClient = builder.build();
    }

    /**
     * 搜索面板列表
     */
    public List<Map<String, Object>> searchDashboards() {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search")
                            .queryParam("type", "dash-db")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                        if (resp.statusCode().value() == 401) {
                            return resp.bodyToMono(String.class)
                                    .map(body -> MonitorException.grafanaAuthFailed());
                        }
                        return resp.bodyToMono(String.class)
                                .map(body -> MonitorException.dashboardNotFound());
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .onErrorMap(WebClientRequestException.class, e -> {
                        log.error("Grafana 连接失败: {}", e.getMessage());
                        return MonitorException.grafanaUnavailable();
                    })
                    .block();
        } catch (MonitorException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询 Grafana 面板列表异常: {}", e.getMessage());
            throw MonitorException.grafanaUnavailable();
        }
    }

    /**
     * 获取面板详情
     */
    public Map<String, Object> getDashboard(String uid) {
        try {
            return webClient.get()
                    .uri("/api/dashboards/uid/{uid}", uid)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> MonitorException.dashboardNotFound()))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .onErrorMap(WebClientRequestException.class, e -> MonitorException.grafanaUnavailable())
                    .block();
        } catch (MonitorException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 Grafana 面板详情异常: {}", e.getMessage());
            throw MonitorException.grafanaUnavailable();
        }
    }

    /**
     * 构建嵌入 URL
     */
    public String buildEmbedUrl(String uid, String from, String to) {
        StringBuilder url = new StringBuilder(grafanaUrl);
        url.append("/d/").append(uid);

        // 添加查询参数
        url.append("?orgId=1");

        if (kioskMode) {
            url.append("&kiosk");
        }

        url.append("&theme=light");

        if (from != null && !from.isEmpty()) {
            url.append("&from=").append(URLEncoder.encode(from, StandardCharsets.UTF_8));
        } else {
            url.append("&from=now-1h");
        }

        if (to != null && !to.isEmpty()) {
            url.append("&to=").append(URLEncoder.encode(to, StandardCharsets.UTF_8));
        } else {
            url.append("&to=now");
        }

        return url.toString();
    }
}
