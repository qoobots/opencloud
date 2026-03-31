# Grafana 安装文档

## 一、概述

本文档介绍在 Kubernetes 集群中独立部署 **Grafana**（或与 kube-prometheus-stack 集成）的完整流程，包含：

- Grafana 部署与持久化配置
- 数据源配置（Prometheus / Loki / Elasticsearch）
- Dashboard 导入与配置
- 告警通知配置
- LDAP / OAuth 单点登录
- Ingress 访问配置

> 如果已通过 `kube-prometheus-stack` 部署了 Grafana，可直接跳至第五节进行数据源和 Dashboard 配置。

---

## 二、前置条件

- Kubernetes 集群正常运行
- Helm 3.x 已安装
- Prometheus 已部署并正常采集数据
- 有可用的 StorageClass

---

## 三、使用 Helm 独立安装 Grafana

### 3.1 添加 Helm 仓库

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

### 3.2 准备配置文件

创建 `values-grafana.yaml`：

```yaml
# 管理员账号
adminUser: admin
adminPassword: "<your-grafana-password>"

# 持久化存储
persistence:
  enabled: true
  storageClassName: rook-ceph-block
  size: 20Gi
  accessModes:
    - ReadWriteOnce

# 副本数
replicas: 1

# 资源限制
resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 1000m
    memory: 512Mi

# Service 配置
service:
  type: ClusterIP
  port: 80

# Ingress 配置
ingress:
  enabled: true
  ingressClassName: nginx
  hosts:
    - grafana.opencloud.local
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"

# 数据源（自动配置 Prometheus）
datasources:
  datasources.yaml:
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        url: http://kube-prometheus-stack-prometheus.monitoring:9090
        access: proxy
        isDefault: true
        jsonData:
          timeInterval: "15s"
          httpMethod: POST
      - name: Alertmanager
        type: alertmanager
        url: http://kube-prometheus-stack-alertmanager.monitoring:9093
        access: proxy
        jsonData:
          handleGrafanaManagedAlerts: false
          implementation: prometheus

# 预装 Dashboard 插件
plugins:
  - grafana-clock-panel
  - grafana-piechart-panel
  - grafana-worldmap-panel
  - natel-discrete-panel
  - vonage-status-panel

# Grafana 主配置
grafana.ini:
  server:
    domain: grafana.opencloud.local
    root_url: "%(protocol)s://%(domain)s/"
  analytics:
    check_for_updates: false
  auth:
    disable_login_form: false
  log:
    mode: console
    level: info
  explore:
    enabled: true
  alerting:
    enabled: true
  unified_alerting:
    enabled: true
```

### 3.3 执行安装

```bash
kubectl create namespace monitoring  # 若未创建
helm install grafana grafana/grafana \
  -n monitoring \
  -f values-grafana.yaml \
  --version 7.0.0
```

---

## 四、验证安装

```bash
# 查看 Pod 状态
kubectl get pods -n monitoring -l app.kubernetes.io/name=grafana

# 查看 PVC
kubectl get pvc -n monitoring

# 获取管理员密码（若未指定）
kubectl get secret -n monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode
```

---

## 五、配置数据源

### 5.1 通过 UI 添加数据源

1. 进入 Grafana UI → **Configuration（齿轮图标）→ Data Sources**
2. 点击 **Add data source**

#### 添加 Prometheus

| 字段 | 值 |
|------|-----|
| Name | Prometheus |
| Type | Prometheus |
| URL | `http://kube-prometheus-stack-prometheus.monitoring:9090` |
| Scrape interval | 15s |

点击 **Save & Test** 确认连接成功。

#### 添加 Loki（日志，可选）

| 字段 | 值 |
|------|-----|
| Name | Loki |
| Type | Loki |
| URL | `http://loki.monitoring:3100` |

#### 添加 Elasticsearch（可选）

| 字段 | 值 |
|------|-----|
| Name | Elasticsearch |
| Type | Elasticsearch |
| URL | `http://elasticsearch.monitoring:9200` |
| Index name | `logstash-*` |
| Time field | `@timestamp` |

---

## 六、导入常用 Dashboard

### 6.1 通过 Dashboard ID 导入

进入 Grafana → **Dashboards → Import**，输入以下 Dashboard ID：

| Dashboard 名称 | ID | 用途 |
|--------------|-----|------|
| Node Exporter Full | 1860 | 主机资源监控 |
| Kubernetes Cluster Overview | 7249 | K8s 集群概览 |
| Kubernetes Pod Resources | 6781 | Pod 资源使用 |
| Ceph Cluster | 2842 | Ceph 存储监控 |
| OpenStack Overview | 5984 | OpenStack 监控 |
| Nginx Ingress Controller | 9614 | Nginx 流量监控 |
| MySQL Overview | 7362 | MySQL 监控 |
| Redis Overview | 11835 | Redis 监控 |
| Kafka Overview | 7589 | Kafka 监控 |

### 6.2 通过 ConfigMap 预装 Dashboard

```yaml
# grafana-dashboards-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: node-exporter-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"  # 被 Grafana sidecar 自动导入
data:
  node-exporter.json: |
    { ... }  # 粘贴 Dashboard JSON 内容
```

```bash
kubectl apply -f grafana-dashboards-configmap.yaml
```

在 `values-grafana.yaml` 中启用 sidecar：

```yaml
sidecar:
  dashboards:
    enabled: true
    label: grafana_dashboard
    labelValue: "1"
    searchNamespace: ALL
```

---

## 七、配置告警通知

### 7.1 配置通知渠道（Contact Points）

进入 **Alerting → Contact points → Add contact point**

#### 邮件通知

```yaml
Type: Email
Addresses: ops-team@example.com
Subject: [Grafana] {{ .GroupLabels.alertname }}
```

#### 钉钉 Webhook

```yaml
Type: DingDing
URL: https://oapi.dingtalk.com/robot/send?access_token=<token>
Message type: actionCard
```

#### 企业微信

```yaml
Type: WeCom
URL: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<key>
```

### 7.2 创建告警规则

进入 **Alerting → Alert rules → New alert rule**

```
Rule name: High CPU Usage
Query: 
  A: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
Condition: A is above 85
For: 5m
Labels: severity=warning
Annotations:
  Summary: High CPU usage on {{ $labels.instance }}
  Description: CPU usage is {{ $value | printf "%.2f" }}%
```

---

## 八、配置 LDAP 单点登录（可选）

编辑 `grafana.ini`：

```ini
[auth.ldap]
enabled = true
config_file = /etc/grafana/ldap.toml
allow_sign_up = true
```

创建 `ldap.toml`：

```toml
[[servers]]
host = "ldap.opencloud.local"
port = 636
use_ssl = true
ssl_skip_verify = false
bind_dn = "cn=grafana-bind,dc=opencloud,dc=local"
bind_password = "<bind-password>"
search_filter = "(sAMAccountName=%s)"
search_base_dns = ["ou=Users,dc=opencloud,dc=local"]

[servers.attributes]
name = "givenName"
surname = "sn"
username = "sAMAccountName"
member_of = "memberOf"
email = "mail"

[[servers.group_mappings]]
group_dn = "cn=grafana-admins,ou=Groups,dc=opencloud,dc=local"
org_role = "Admin"

[[servers.group_mappings]]
group_dn = "cn=grafana-editors,ou=Groups,dc=opencloud,dc=local"
org_role = "Editor"

[[servers.group_mappings]]
group_dn = "*"
org_role = "Viewer"
```

---

## 九、配置 OAuth 单点登录（可选）

以 GitLab 为例，在 `grafana.ini` 中添加：

```ini
[auth.gitlab]
enabled = true
allow_sign_up = true
client_id = <gitlab-app-id>
client_secret = <gitlab-app-secret>
scopes = read_user
auth_url = https://gitlab.opencloud.local/oauth/authorize
token_url = https://gitlab.opencloud.local/oauth/token
api_url = https://gitlab.opencloud.local/api/v4/user
allowed_groups = opencloud/ops
```

---

## 十、备份与恢复

### 10.1 导出 Dashboard

```bash
# 通过 API 导出所有 Dashboard
GRAFANA_URL="http://grafana.opencloud.local"
GRAFANA_TOKEN="<api-token>"

for uid in $(curl -s -H "Authorization: Bearer $GRAFANA_TOKEN" "$GRAFANA_URL/api/dashboards/home" | jq -r '.uid'); do
  curl -s -H "Authorization: Bearer $GRAFANA_TOKEN" \
    "$GRAFANA_URL/api/dashboards/uid/$uid" \
    -o "dashboard-$uid.json"
done
```

### 10.2 备份 Grafana 数据库

```bash
# 找到 Grafana Pod
GRAFANA_POD=$(kubectl get pod -n monitoring -l app.kubernetes.io/name=grafana -o jsonpath='{.items[0].metadata.name}')

# 复制 SQLite 数据库
kubectl cp monitoring/$GRAFANA_POD:/var/lib/grafana/grafana.db ./grafana-backup-$(date +%Y%m%d).db
```

---

## 十一、常见问题排查

| 问题 | 排查方法 |
|------|---------|
| 数据源无法连接 | 检查 Service 名称和端口，查看 Network Policy |
| Dashboard 无数据 | 检查时间范围，确认 Prometheus 数据正常 |
| 告警未触发 | 查看 `Alerting > Alert rules` 中规则状态 |
| 持久化数据丢失 | 检查 PVC 绑定状态和 StorageClass |
| OAuth 登录失败 | 查看 Grafana Pod 日志 `kubectl logs -n monitoring <pod>` |

---

## 十二、参考资料

- [Grafana 官方文档](https://grafana.com/docs/grafana/latest/)
- [Grafana Helm Chart](https://github.com/grafana/helm-charts/tree/main/charts/grafana)
- [Grafana Dashboard 社区](https://grafana.com/grafana/dashboards/)
- [Grafana Alert 文档](https://grafana.com/docs/grafana/latest/alerting/)
