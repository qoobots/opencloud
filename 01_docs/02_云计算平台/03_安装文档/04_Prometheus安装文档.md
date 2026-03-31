# Prometheus 安装文档

## 一、概述

本文档介绍在 Kubernetes 集群中使用 **kube-prometheus-stack** Helm Chart 部署 Prometheus 监控体系，包含：

- **Prometheus Server** — 指标采集与存储
- **Alertmanager** — 告警通知
- **Node Exporter** — 主机指标
- **kube-state-metrics** — K8s 资源状态指标
- **Prometheus Operator** — 自定义资源管理

---

## 二、前置条件

- Kubernetes 集群正常运行（≥ 1.24）
- Helm 3.x 已安装
- 有可用的 StorageClass（持久化存储）
- kubectl 已配置访问集群

---

## 三、安装 Helm

```bash
# 下载 Helm
curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# 验证安装
helm version
```

---

## 四、使用 Helm 安装 kube-prometheus-stack

### 4.1 添加 Helm 仓库

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### 4.2 创建命名空间

```bash
kubectl create namespace monitoring
```

### 4.3 准备自定义配置文件

创建 `values-prometheus.yaml`：

```yaml
# Prometheus 配置
prometheus:
  prometheusSpec:
    # 数据保留时间
    retention: 30d
    # 持久化存储
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: rook-ceph-block
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 100Gi
    # 资源限制
    resources:
      requests:
        memory: 2Gi
        cpu: 500m
      limits:
        memory: 4Gi
        cpu: 2000m
    # 副本数（高可用）
    replicas: 2
    # 抓取所有 ServiceMonitor（不限命名空间）
    serviceMonitorSelectorNilUsesHelmValues: false
    podMonitorSelectorNilUsesHelmValues: false
    ruleSelectorNilUsesHelmValues: false

# Alertmanager 配置
alertmanager:
  alertmanagerSpec:
    replicas: 3
    storage:
      volumeClaimTemplate:
        spec:
          storageClassName: rook-ceph-block
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi
  config:
    global:
      smtp_smarthost: 'smtp.example.com:587'
      smtp_from: 'alertmanager@example.com'
      smtp_auth_username: 'alertmanager@example.com'
      smtp_auth_password: '<your-smtp-password>'
    route:
      group_by: ['alertname', 'cluster', 'service']
      group_wait: 10s
      group_interval: 10m
      repeat_interval: 12h
      receiver: 'default-receiver'
      routes:
        - match:
            severity: critical
          receiver: 'critical-receiver'
    receivers:
      - name: 'default-receiver'
        email_configs:
          - to: 'ops-team@example.com'
      - name: 'critical-receiver'
        email_configs:
          - to: 'oncall@example.com'
        webhook_configs:
          - url: 'http://dingtalk-webhook:8060/dingtalk/webhook/send'

# Grafana（随 kube-prometheus-stack 一起部署）
grafana:
  enabled: true
  adminPassword: "<your-grafana-password>"
  persistence:
    enabled: true
    storageClassName: rook-ceph-block
    size: 20Gi
  ingress:
    enabled: true
    ingressClassName: nginx
    hosts:
      - grafana.opencloud.local
    annotations:
      nginx.ingress.kubernetes.io/ssl-redirect: "false"

# Node Exporter
nodeExporter:
  enabled: true

# kube-state-metrics
kubeStateMetrics:
  enabled: true

# Prometheus Operator
prometheusOperator:
  enabled: true
  resources:
    requests:
      memory: 200Mi
      cpu: 100m
    limits:
      memory: 400Mi
      cpu: 200m
```

### 4.4 执行安装

```bash
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring \
  -f values-prometheus.yaml \
  --version 55.5.0
```

---

## 五、验证安装

```bash
# 查看所有 Pod
kubectl get pods -n monitoring

# 查看 Service
kubectl get svc -n monitoring

# 查看 PVC 状态
kubectl get pvc -n monitoring
```

预期输出（所有 Pod 处于 Running 状态）：
```
NAME                                             READY   STATUS    RESTARTS   AGE
alertmanager-kube-prometheus-stack-0             2/2     Running   0          5m
alertmanager-kube-prometheus-stack-1             2/2     Running   0          5m
kube-prometheus-stack-grafana-xxx                3/3     Running   0          5m
kube-prometheus-stack-kube-state-metrics-xxx     1/1     Running   0          5m
kube-prometheus-stack-operator-xxx               1/1     Running   0          5m
kube-prometheus-stack-prometheus-node-exporter   1/1     Running   0          5m
prometheus-kube-prometheus-stack-0               2/2     Running   0          5m
prometheus-kube-prometheus-stack-1               2/2     Running   0          5m
```

---

## 六、访问 Prometheus UI

### 6.1 临时端口转发（测试用）

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090 --address=0.0.0.0
```

访问：`http://<node-ip>:9090`

### 6.2 创建 Ingress（生产推荐）

```yaml
# prometheus-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: prometheus-ingress
  namespace: monitoring
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
    - host: prometheus.opencloud.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: kube-prometheus-stack-prometheus
                port:
                  number: 9090
```

```bash
kubectl apply -f prometheus-ingress.yaml
```

---

## 七、自定义监控目标（ServiceMonitor）

### 7.1 监控自定义应用示例

```yaml
# custom-servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: my-app-monitor
  namespace: monitoring
  labels:
    release: kube-prometheus-stack
spec:
  selector:
    matchLabels:
      app: my-app
  namespaceSelector:
    matchNames:
      - default
  endpoints:
    - port: metrics
      interval: 15s
      path: /metrics
```

```bash
kubectl apply -f custom-servicemonitor.yaml
```

---

## 八、配置 Prometheus 规则（PrometheusRule）

```yaml
# custom-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: custom-alert-rules
  namespace: monitoring
  labels:
    release: kube-prometheus-stack
spec:
  groups:
    - name: node-alerts
      interval: 1m
      rules:
        - alert: HighCPUUsage
          expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "High CPU usage on {{ $labels.instance }}"
            description: "CPU usage is {{ $value }}%"

        - alert: HighMemoryUsage
          expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "High memory usage on {{ $labels.instance }}"
            description: "Memory usage is {{ $value }}%"

        - alert: DiskSpaceLow
          expr: (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100 > 85
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Low disk space on {{ $labels.instance }}"
```

```bash
kubectl apply -f custom-rules.yaml
```

---

## 九、集成 OpenStack / Ceph 监控

### 9.1 OpenStack Exporter

```bash
helm repo add openstack-exporter https://openstack-exporter.github.io/charts
helm install openstack-exporter openstack-exporter/prometheus-openstack-exporter \
  -n monitoring \
  --set openstackCloud.authUrl=http://192.168.1.100:5000/v3 \
  --set openstackCloud.username=admin \
  --set openstackCloud.password=<admin-password>
```

### 9.2 Ceph Exporter

Ceph 自带 Prometheus metrics 端点，只需创建 ServiceMonitor：

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: ceph-monitor
  namespace: monitoring
spec:
  endpoints:
    - interval: 30s
      port: http-metrics
      scheme: http
  namespaceSelector:
    matchNames:
      - rook-ceph
  selector:
    matchLabels:
      app: rook-ceph-mgr
```

---

## 十、Thanos 集成（长期存储，可选）

```bash
# 安装 Thanos
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install thanos bitnami/thanos \
  -n monitoring \
  --set query.enabled=true \
  --set storegateway.enabled=true \
  --set objstoreConfig="type: S3\nconfig:\n  bucket: thanos\n  endpoint: minio.opencloud.local:9000\n  access_key: <access-key>\n  secret_key: <secret-key>\n  insecure: true"
```

在 `values-prometheus.yaml` 中启用 Thanos sidecar：

```yaml
prometheus:
  prometheusSpec:
    thanos:
      image: quay.io/thanos/thanos:v0.32.0
      objectStorageConfig:
        key: objstore.yml
        name: thanos-objstore-secret
```

---

## 十一、常见问题排查

| 问题 | 排查命令 |
|------|---------|
| Prometheus 抓取失败 | 在 UI `Status > Targets` 查看 Target 状态 |
| 告警未触发 | 在 UI `Alerts` 页面查看规则评估状态 |
| PVC 未绑定 | `kubectl describe pvc -n monitoring` |
| Alertmanager 未发送通知 | 查看 Alertmanager UI `Status > Config` 和 `Logs` |

---

## 十二、参考资料

- [kube-prometheus-stack 文档](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
- [Prometheus 官方文档](https://prometheus.io/docs/introduction/overview/)
- [Prometheus Operator 文档](https://prometheus-operator.dev/)
- [Thanos 文档](https://thanos.io/tip/thanos/getting-started.md/)
