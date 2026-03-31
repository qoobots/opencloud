#!/bin/bash
# =============================================================================
# Grafana 自动化部署脚本
# 版本: 1.0.0
# 描述: 在 Kubernetes 集群中使用 Helm 独立部署 Grafana
#       包含数据源配置、Dashboard 导入、告警通知渠道配置
# 使用方式: bash deploy_grafana.sh [--all | --install | --configure | --dashboards | --status]
# =============================================================================

set -euo pipefail

# ========================== 颜色定义 ==========================
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

# ========================== 配置变量 ==========================
# !! 部署前请根据实际环境修改以下配置 !!

# Helm Chart 版本
CHART_VERSION="7.0.0"
CHART_NAME="grafana"

# 命名空间
NAMESPACE="monitoring"

# Grafana 管理员账号
GRAFANA_ADMIN_USER="admin"
GRAFANA_ADMIN_PASSWORD="Grafana@Admin2024"

# 域名
GRAFANA_DOMAIN="grafana.opencloud.local"

# 存储配置
STORAGE_CLASS="rook-ceph-block"
GRAFANA_STORAGE="20Gi"

# Prometheus 地址（在集群内访问）
PROMETHEUS_URL="http://kube-prometheus-stack-prometheus.monitoring:9090"
ALERTMANAGER_URL="http://kube-prometheus-stack-alertmanager.monitoring:9093"

# Loki 地址（可选）
LOKI_URL="http://loki.monitoring:3100"
LOKI_ENABLED=false

# Elasticsearch 地址（可选）
ES_URL="http://elasticsearch.monitoring:9200"
ES_ENABLED=false

# 告警通知 - 钉钉（可选，留空禁用）
DINGTALK_WEBHOOK=""
DINGTALK_SECRET=""

# 告警通知 - 企业微信（可选，留空禁用）
WECHAT_WEBHOOK=""

# 告警通知 - 邮件（可选，留空禁用）
SMTP_HOST=""
SMTP_USER=""
SMTP_PASSWORD=""
ALERT_EMAIL_TO=""

# 日志文件
LOG_FILE="/tmp/deploy_grafana_$(date +%Y%m%d_%H%M%S).log"
VALUES_FILE="/tmp/grafana-values.yaml"

# ========================== 工具函数 ==========================

log()   { echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$LOG_FILE"; }
warn()  { echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$LOG_FILE"; exit 1; }
info()  { echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$LOG_FILE"; }

check_prerequisites() {
  command -v kubectl &>/dev/null || error "kubectl 未安装"
  command -v helm &>/dev/null || error "helm 未安装"
  kubectl cluster-info &>/dev/null || error "无法连接到 Kubernetes 集群"
  log "前置检查通过 ✓"
}

get_grafana_pod() {
  kubectl get pod -n "$NAMESPACE" -l "app.kubernetes.io/name=grafana" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null
}

grafana_api() {
  local method="$1"
  local path="$2"
  local data="${3:-}"
  local pod
  pod=$(get_grafana_pod)
  if [[ -z "$pod" ]]; then
    warn "Grafana Pod 未就绪"
    return 1
  fi
  if [[ -n "$data" ]]; then
    kubectl exec -n "$NAMESPACE" "$pod" -- \
      curl -sf -X "$method" \
      -H "Content-Type: application/json" \
      -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
      "http://localhost:3000${path}" \
      -d "$data"
  else
    kubectl exec -n "$NAMESPACE" "$pod" -- \
      curl -sf -X "$method" \
      -H "Content-Type: application/json" \
      -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
      "http://localhost:3000${path}"
  fi
}

wait_grafana_ready() {
  local timeout=300
  local elapsed=0
  log "等待 Grafana 就绪..."
  while [[ $elapsed -lt $timeout ]]; do
    if grafana_api GET "/api/health" 2>/dev/null | grep -q "ok"; then
      log "Grafana 已就绪 ✓"
      return 0
    fi
    sleep 10
    elapsed=$((elapsed + 10))
    warn "等待中... ${elapsed}s/${timeout}s"
  done
  warn "Grafana 等待超时"
}

# ========================== 阶段一: 生成 values.yaml ==========================

phase_generate_values() {
  info "========== 阶段一: 生成 Grafana values.yaml =========="

  # 构建数据源配置
  local datasources_extra=""
  if [[ "$LOKI_ENABLED" == true ]]; then
    datasources_extra="$datasources_extra
      - name: Loki
        type: loki
        url: ${LOKI_URL}
        access: proxy
        jsonData:
          maxLines: 1000"
  fi
  if [[ "$ES_ENABLED" == true ]]; then
    datasources_extra="$datasources_extra
      - name: Elasticsearch
        type: elasticsearch
        url: ${ES_URL}
        access: proxy
        database: 'logstash-*'
        jsonData:
          esVersion: '8.0.0'
          timeField: '@timestamp'
          logMessageField: message
          logLevelField: level"
  fi

  cat > "$VALUES_FILE" << EOF
# ====== Grafana Helm values.yaml ======
# 生成时间: $(date)

# 管理员账号
adminUser: "${GRAFANA_ADMIN_USER}"
adminPassword: "${GRAFANA_ADMIN_PASSWORD}"

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

# 持久化存储
persistence:
  enabled: true
  storageClassName: ${STORAGE_CLASS}
  size: ${GRAFANA_STORAGE}
  accessModes:
    - ReadWriteOnce

# Service
service:
  type: ClusterIP
  port: 80

# Ingress
ingress:
  enabled: true
  ingressClassName: nginx
  hosts:
    - ${GRAFANA_DOMAIN}
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"

# Grafana 主配置
grafana.ini:
  server:
    domain: ${GRAFANA_DOMAIN}
    root_url: "%(protocol)s://%(domain)s/"
  analytics:
    check_for_updates: false
    reporting_enabled: false
  explore:
    enabled: true
  alerting:
    enabled: false
  unified_alerting:
    enabled: true
  log:
    mode: console
    level: info
  security:
    admin_user: "${GRAFANA_ADMIN_USER}"
    admin_password: "${GRAFANA_ADMIN_PASSWORD}"
    allow_embedding: true
  users:
    allow_sign_up: false
    auto_assign_org_role: Viewer
  dashboards:
    default_home_dashboard_path: /var/lib/grafana/dashboards/default/k8s-cluster.json

# 数据源
datasources:
  datasources.yaml:
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        url: ${PROMETHEUS_URL}
        access: proxy
        isDefault: true
        jsonData:
          timeInterval: "15s"
          httpMethod: POST
          exemplarTraceIdDestinations:
            - name: traceID
              datasourceUid: jaeger
      - name: Alertmanager
        type: alertmanager
        url: ${ALERTMANAGER_URL}
        access: proxy
        jsonData:
          handleGrafanaManagedAlerts: false
          implementation: prometheus
${datasources_extra}

# 预装插件
plugins:
  - grafana-clock-panel
  - grafana-piechart-panel
  - grafana-worldmap-panel
  - vonage-status-panel
  - natel-discrete-panel
  - grafana-polystat-panel

# Sidecar - 自动导入 Dashboard ConfigMap
sidecar:
  dashboards:
    enabled: true
    label: grafana_dashboard
    labelValue: "1"
    searchNamespace: ALL
    folderAnnotation: grafana_folder
    provider:
      foldersFromFilesStructure: true
  datasources:
    enabled: true
    label: grafana_datasource
    labelValue: "1"
    searchNamespace: ALL

# 初始化 Dashboard 提供者
dashboardProviders:
  dashboardproviders.yaml:
    apiVersion: 1
    providers:
      - name: 'default'
        orgId: 1
        folder: 'OpenCloud'
        type: file
        disableDeletion: false
        editable: true
        options:
          path: /var/lib/grafana/dashboards/default
EOF

  log "values.yaml 生成完成: $VALUES_FILE ✓"
}

# ========================== 阶段二: 安装/升级 Grafana ==========================

phase_install() {
  info "========== 阶段二: 安装/升级 Grafana =========="

  log "创建命名空间..."
  kubectl create namespace "$NAMESPACE" 2>/dev/null || \
    log "命名空间 $NAMESPACE 已存在"

  log "添加 Grafana Helm 仓库..."
  helm repo add grafana https://grafana.github.io/helm-charts 2>/dev/null || true
  helm repo update 2>&1 | tee -a "$LOG_FILE"

  if helm status "$CHART_NAME" -n "$NAMESPACE" &>/dev/null; then
    log "Grafana 已安装，执行 upgrade..."
    helm upgrade "$CHART_NAME" grafana/grafana \
      -n "$NAMESPACE" \
      -f "$VALUES_FILE" \
      --version "$CHART_VERSION" \
      --wait \
      --timeout 10m \
      2>&1 | tee -a "$LOG_FILE"
  else
    log "安装 Grafana v${CHART_VERSION}..."
    helm install "$CHART_NAME" grafana/grafana \
      -n "$NAMESPACE" \
      -f "$VALUES_FILE" \
      --version "$CHART_VERSION" \
      --wait \
      --timeout 10m \
      2>&1 | tee -a "$LOG_FILE"
  fi

  log "Grafana 安装完成 ✓"
}

# ========================== 阶段三: 配置告警通知渠道 ==========================

phase_configure_alerts() {
  info "========== 阶段三: 配置告警通知渠道 =========="

  wait_grafana_ready

  # 配置钉钉
  if [[ -n "$DINGTALK_WEBHOOK" ]]; then
    log "配置钉钉告警..."
    grafana_api POST "/api/v1/provisioning/contact-points" \
      "{\"name\":\"DingTalk\",\"type\":\"dingding\",\"settings\":{\"url\":\"${DINGTALK_WEBHOOK}\",\"msgType\":\"actionCard\"},\"disableResolveMessage\":false}" \
      2>/dev/null || warn "钉钉告警配置失败"
  fi

  # 配置企业微信
  if [[ -n "$WECHAT_WEBHOOK" ]]; then
    log "配置企业微信告警..."
    grafana_api POST "/api/v1/provisioning/contact-points" \
      "{\"name\":\"WeChat\",\"type\":\"wecom\",\"settings\":{\"url\":\"${WECHAT_WEBHOOK}\"},\"disableResolveMessage\":false}" \
      2>/dev/null || warn "企业微信告警配置失败"
  fi

  # 配置邮件
  if [[ -n "$SMTP_HOST" && -n "$ALERT_EMAIL_TO" ]]; then
    log "配置邮件告警..."
    grafana_api POST "/api/v1/provisioning/contact-points" \
      "{\"name\":\"Email\",\"type\":\"email\",\"settings\":{\"addresses\":\"${ALERT_EMAIL_TO}\",\"singleEmail\":false},\"disableResolveMessage\":false}" \
      2>/dev/null || warn "邮件告警配置失败"
  fi

  log "告警通知渠道配置完成 ✓"
}

# ========================== 阶段四: 导入常用 Dashboard ==========================

phase_import_dashboards() {
  info "========== 阶段四: 导入常用 Dashboard =========="

  wait_grafana_ready

  # 创建 OpenCloud 文件夹
  log "创建 Dashboard 文件夹..."
  local folder_uid
  folder_uid=$(grafana_api POST "/api/folders" \
    '{"uid":"opencloud","title":"OpenCloud"}' 2>/dev/null | \
    python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('uid',''))" 2>/dev/null) || \
    folder_uid="opencloud"

  # 需要导入的 Dashboard（Grafana.com ID）
  local dashboard_ids=(
    1860   # Node Exporter Full
    7249   # Kubernetes Cluster Overview
    6781   # Kubernetes Pod Resources
    9614   # Nginx Ingress Controller
    13770  # Kubernetes Overview
    2842   # Ceph Cluster
    5342   # Ceph OSD
    7362   # MySQL Overview
    11835  # Redis Dashboard
  )

  for id in "${dashboard_ids[@]}"; do
    log "导入 Dashboard ID: $id..."
    local dashboard_json
    # 从 Grafana.com 下载 Dashboard JSON
    dashboard_json=$(curl -sf \
      "https://grafana.com/api/dashboards/${id}/revisions/latest/download" 2>/dev/null) || \
      { warn "Dashboard $id 下载失败（网络可能不通），跳过..."; continue; }

    # 通过 API 导入
    local import_payload
    import_payload=$(python3 -c "
import json, sys
d = json.loads(sys.stdin.read())
d['id'] = None
print(json.dumps({'dashboard': d, 'overwrite': True, 'folderId': 0, 'folderUid': 'opencloud'}))
" <<< "$dashboard_json" 2>/dev/null) || { warn "Dashboard $id 解析失败，跳过..."; continue; }

    grafana_api POST "/api/dashboards/import" "$import_payload" > /dev/null 2>&1 && \
      log "Dashboard $id 导入成功 ✓" || \
      warn "Dashboard $id 导入失败"
  done

  log "Dashboard 导入完成 ✓"
}

# ========================== 阶段五: 创建 API Token ==========================

phase_create_api_token() {
  info "========== 创建 Grafana API Token =========="

  wait_grafana_ready

  local token_result
  token_result=$(grafana_api POST "/api/auth/keys" \
    '{"name":"opencloud-admin-token","role":"Admin","secondsToLive":0}' 2>/dev/null) || \
    { warn "API Token 创建失败"; return 1; }

  local token
  token=$(echo "$token_result" | python3 -c \
    "import sys,json; print(json.load(sys.stdin).get('key',''))" 2>/dev/null)

  if [[ -n "$token" ]]; then
    log "API Token 创建成功"
    log "Token: $token"
    echo "$token" > /tmp/grafana-api-token.txt
    log "Token 已保存至: /tmp/grafana-api-token.txt"
  fi
}

# ========================== 查看状态 ==========================

phase_status() {
  info "===== Grafana 状态 ====="

  echo ""
  log "Pod 状态:"
  kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=grafana" -o wide

  echo ""
  log "Service:"
  kubectl get svc -n "$NAMESPACE" "$CHART_NAME" 2>/dev/null || true

  echo ""
  log "PVC:"
  kubectl get pvc -n "$NAMESPACE" 2>/dev/null | grep grafana || true

  echo ""
  log "Ingress:"
  kubectl get ingress -n "$NAMESPACE" 2>/dev/null | grep grafana || true

  echo ""
  log "=============================="
  log "Grafana 访问信息:"
  log "  URL:  http://${GRAFANA_DOMAIN}"
  log "  用户: ${GRAFANA_ADMIN_USER}"
  log "  密码: ${GRAFANA_ADMIN_PASSWORD}"
  log ""
  log "快速端口转发（测试用）:"
  log "  kubectl port-forward -n ${NAMESPACE} svc/${CHART_NAME} 3000:80 --address=0.0.0.0"
  log "  访问: http://localhost:3000"
  log "=============================="
}

# ========================== 卸载 ==========================

phase_uninstall() {
  warn "开始卸载 Grafana..."
  helm uninstall "$CHART_NAME" -n "$NAMESPACE" 2>&1 | tee -a "$LOG_FILE" || true
  kubectl delete pvc -n "$NAMESPACE" -l "app.kubernetes.io/name=grafana" 2>/dev/null || true
  log "Grafana 卸载完成"
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}用法: $0 [选项]${NC}"
  echo ""
  echo "选项:"
  echo "  --all        完整部署（默认）"
  echo "  --values     生成 values.yaml"
  echo "  --install    安装/升级 Grafana"
  echo "  --alerts     配置告警通知"
  echo "  --dashboards 导入 Dashboard"
  echo "  --token      创建 API Token"
  echo "  --status     查看状态"
  echo "  --uninstall  卸载"
  echo "  --help       显示帮助"
}

main() {
  mkdir -p "$(dirname $LOG_FILE)"
  log "日志文件: $LOG_FILE"

  local action="${1:---all}"

  case "$action" in
    --all)
      check_prerequisites
      phase_generate_values
      phase_install
      phase_configure_alerts
      phase_import_dashboards
      log "=============================================="
      log "Grafana 部署完成! 🎉"
      log "URL:  http://${GRAFANA_DOMAIN}"
      log "账号: ${GRAFANA_ADMIN_USER} / ${GRAFANA_ADMIN_PASSWORD}"
      log "=============================================="
      phase_status
      ;;
    --values)     phase_generate_values ;;
    --install)    check_prerequisites && phase_generate_values && phase_install ;;
    --alerts)     phase_configure_alerts ;;
    --dashboards) phase_import_dashboards ;;
    --token)      phase_create_api_token ;;
    --status)     phase_status ;;
    --uninstall)  phase_uninstall ;;
    --help)       usage ;;
    *)            error "未知选项: $action，使用 --help 查看帮助" ;;
  esac
}

main "$@"
