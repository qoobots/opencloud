#!/bin/bash
# =============================================================================
# Prometheus 自动化部署脚本
# 版本: 1.0.0
# 描述: 在 Kubernetes 集群中使用 Helm 部署 kube-prometheus-stack
#       包含 Prometheus、Alertmanager、Node Exporter、kube-state-metrics
# 使用方式: bash deploy_prometheus.sh [--all | --install | --configure | --status]
# =============================================================================

set -euo pipefail

# ========================== 颜色定义 ==========================
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

# ========================== 配置变量 ==========================
# !! 部署前请根据实际环境修改以下配置 !!

# Helm Chart 版本
CHART_VERSION="55.5.0"
CHART_NAME="kube-prometheus-stack"

# 命名空间
NAMESPACE="monitoring"

# Prometheus 数据保留时间
RETENTION="30d"

# 持久化存储类（需与集群中实际 StorageClass 一致）
STORAGE_CLASS="rook-ceph-block"

# Prometheus 存储大小
PROMETHEUS_STORAGE="100Gi"

# Alertmanager 存储大小
ALERTMANAGER_STORAGE="10Gi"

# Grafana 存储大小
GRAFANA_STORAGE="20Gi"

# Grafana 管理员密码
GRAFANA_ADMIN_PASSWORD="Grafana@Admin2024"

# 域名配置（用于 Ingress）
PROMETHEUS_DOMAIN="prometheus.opencloud.local"
ALERTMANAGER_DOMAIN="alertmanager.opencloud.local"
GRAFANA_DOMAIN="grafana.opencloud.local"

# 告警邮件配置（可选，留空则禁用邮件告警）
SMTP_HOST="smtp.example.com:587"
SMTP_FROM="alertmanager@example.com"
SMTP_USERNAME="alertmanager@example.com"
SMTP_PASSWORD=""
ALERT_EMAIL_TO="ops-team@example.com"

# 钉钉 Webhook（可选，留空则禁用）
DINGTALK_WEBHOOK=""

# 企业微信 Webhook（可选，留空则禁用）
WECHAT_WEBHOOK=""

# Prometheus 副本数（1=单副本，2=高可用）
PROMETHEUS_REPLICAS=2

# Alertmanager 副本数（3=高可用）
ALERTMANAGER_REPLICAS=3

# 日志文件
LOG_FILE="/tmp/deploy_prometheus_$(date +%Y%m%d_%H%M%S).log"

# 配置文件输出路径
VALUES_FILE="/tmp/prometheus-values.yaml"

# ========================== 工具函数 ==========================

log()   { echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$LOG_FILE"; }
warn()  { echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$LOG_FILE"; exit 1; }
info()  { echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$LOG_FILE"; }

check_prerequisites() {
  command -v kubectl &>/dev/null || error "kubectl 未安装"
  command -v helm &>/dev/null || error "helm 未安装，请先安装 Helm 3"
  kubectl cluster-info &>/dev/null || error "无法连接到 Kubernetes 集群"
  log "前置检查通过 ✓"
}

wait_deployment_ready() {
  local name="$1"
  local ns="${2:-$NAMESPACE}"
  local timeout=300
  log "等待 Deployment $name 就绪..."
  kubectl rollout status deployment/"$name" -n "$ns" --timeout="${timeout}s" || \
    warn "Deployment $name 等待超时"
}

wait_statefulset_ready() {
  local name="$1"
  local ns="${2:-$NAMESPACE}"
  local timeout=300
  log "等待 StatefulSet $name 就绪..."
  kubectl rollout status statefulset/"$name" -n "$ns" --timeout="${timeout}s" || \
    warn "StatefulSet $name 等待超时"
}

# ========================== 阶段一: 安装 Helm ==========================

phase_install_helm() {
  info "========== 阶段一: 安装/检查 Helm =========="

  if command -v helm &>/dev/null; then
    log "Helm 已安装: $(helm version --short)"
    return 0
  fi

  log "下载并安装 Helm..."
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash \
    2>&1 | tee -a "$LOG_FILE"

  log "Helm 安装完成: $(helm version --short) ✓"
}

# ========================== 阶段二: 添加 Helm 仓库 ==========================

phase_add_repos() {
  info "========== 阶段二: 配置 Helm 仓库 =========="

  log "添加 prometheus-community 仓库..."
  helm repo add prometheus-community \
    https://prometheus-community.github.io/helm-charts 2>/dev/null || true

  log "更新 Helm 仓库..."
  helm repo update 2>&1 | tee -a "$LOG_FILE"

  log "Helm 仓库配置完成 ✓"
}

# ========================== 阶段三: 生成 values.yaml ==========================

phase_generate_values() {
  info "========== 阶段三: 生成 Prometheus values.yaml =========="

  # 构建告警接收器配置
  local receivers_config
  receivers_config=$(cat << 'RECV_EOF'
    receivers:
      - name: 'default-receiver'
        email_configs: []
      - name: 'critical-receiver'
        email_configs: []
RECV_EOF
)

  if [[ -n "$SMTP_PASSWORD" ]]; then
    receivers_config=$(cat << RECV_EOF
    receivers:
      - name: 'default-receiver'
        email_configs:
          - to: '${ALERT_EMAIL_TO}'
            from: '${SMTP_FROM}'
            smarthost: '${SMTP_HOST}'
            auth_username: '${SMTP_USERNAME}'
            auth_password: '${SMTP_PASSWORD}'
            send_resolved: true
      - name: 'critical-receiver'
        email_configs:
          - to: '${ALERT_EMAIL_TO}'
            from: '${SMTP_FROM}'
            smarthost: '${SMTP_HOST}'
            auth_username: '${SMTP_USERNAME}'
            auth_password: '${SMTP_PASSWORD}'
            send_resolved: true
RECV_EOF
)
  fi

  log "生成 $VALUES_FILE..."
  cat > "$VALUES_FILE" << EOF
# ====== kube-prometheus-stack values.yaml ======
# 生成时间: $(date)

# ===========================
# Prometheus 配置
# ===========================
prometheus:
  prometheusSpec:
    retention: ${RETENTION}
    replicas: ${PROMETHEUS_REPLICAS}
    replicaExternalLabelName: prometheus_replica
    # 抓取所有命名空间的 ServiceMonitor
    serviceMonitorSelectorNilUsesHelmValues: false
    podMonitorSelectorNilUsesHelmValues: false
    ruleSelectorNilUsesHelmValues: false
    probeSelectorNilUsesHelmValues: false
    # 资源限制
    resources:
      requests:
        memory: 2Gi
        cpu: 500m
      limits:
        memory: 4Gi
        cpu: 2000m
    # 持久化存储
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: ${STORAGE_CLASS}
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: ${PROMETHEUS_STORAGE}
    # 全局抓取配置
    externalLabels:
      cluster: opencloud
      region: cn-east-1
    # 额外抓取配置
    additionalScrapeConfigs: []
  # Ingress 配置
  ingress:
    enabled: true
    ingressClassName: nginx
    hosts:
      - ${PROMETHEUS_DOMAIN}
    annotations:
      nginx.ingress.kubernetes.io/ssl-redirect: "false"

# ===========================
# Alertmanager 配置
# ===========================
alertmanager:
  alertmanagerSpec:
    replicas: ${ALERTMANAGER_REPLICAS}
    storage:
      volumeClaimTemplate:
        spec:
          storageClassName: ${STORAGE_CLASS}
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: ${ALERTMANAGER_STORAGE}
    resources:
      requests:
        memory: 200Mi
        cpu: 100m
      limits:
        memory: 400Mi
        cpu: 500m
  # 告警路由配置
  config:
    global:
      resolve_timeout: 5m
      smtp_require_tls: true
    route:
      group_by: ['alertname', 'cluster', 'service', 'severity']
      group_wait: 10s
      group_interval: 10m
      repeat_interval: 12h
      receiver: 'default-receiver'
      routes:
        - matchers:
            - severity = "critical"
          receiver: 'critical-receiver'
          continue: false
        - matchers:
            - severity = "warning"
          receiver: 'default-receiver'
    ${receivers_config}
    inhibit_rules:
      - source_matchers:
          - severity = "critical"
        target_matchers:
          - severity = "warning"
        equal: ['alertname', 'cluster', 'service']
  # Ingress 配置
  ingress:
    enabled: true
    ingressClassName: nginx
    hosts:
      - ${ALERTMANAGER_DOMAIN}
    annotations:
      nginx.ingress.kubernetes.io/ssl-redirect: "false"

# ===========================
# Grafana 配置
# ===========================
grafana:
  enabled: true
  adminUser: admin
  adminPassword: "${GRAFANA_ADMIN_PASSWORD}"
  replicas: 1
  persistence:
    enabled: true
    storageClassName: ${STORAGE_CLASS}
    size: ${GRAFANA_STORAGE}
  resources:
    requests:
      cpu: 200m
      memory: 256Mi
    limits:
      cpu: 1000m
      memory: 512Mi
  ingress:
    enabled: true
    ingressClassName: nginx
    hosts:
      - ${GRAFANA_DOMAIN}
    annotations:
      nginx.ingress.kubernetes.io/ssl-redirect: "false"
  # 预配置数据源
  additionalDataSources:
    - name: Loki
      type: loki
      url: http://loki.monitoring:3100
      access: proxy
  # 预装插件
  plugins:
    - grafana-clock-panel
    - grafana-piechart-panel
    - vonage-status-panel
  # sidecar 自动导入 Dashboard
  sidecar:
    dashboards:
      enabled: true
      label: grafana_dashboard
      labelValue: "1"
      searchNamespace: ALL
    datasources:
      enabled: true

# ===========================
# Node Exporter
# ===========================
nodeExporter:
  enabled: true
  hostRootfs: true

# ===========================
# kube-state-metrics
# ===========================
kubeStateMetrics:
  enabled: true

# ===========================
# Prometheus Operator
# ===========================
prometheusOperator:
  enabled: true
  resources:
    requests:
      memory: 200Mi
      cpu: 100m
    limits:
      memory: 400Mi
      cpu: 200m

# ===========================
# 默认告警规则（全部启用）
# ===========================
defaultRules:
  create: true
  rules:
    alertmanager: true
    etcd: true
    configReloaders: true
    general: true
    k8sContainerCpuUsageSecondsTotal: true
    k8sContainerMemoryCache: true
    k8sContainerMemoryRss: true
    k8sContainerMemorySwap: true
    k8sContainerResource: true
    k8sPodOwner: true
    kubeApiserverAvailability: true
    kubeApiserverBurnrate: true
    kubeApiserverHistogram: true
    kubeApiserverSlos: true
    kubeControllerManager: true
    kubelet: true
    kubeProxy: true
    kubePrometheusGeneral: true
    kubePrometheusNodeRecording: true
    kubernetesApps: true
    kubernetesResources: true
    kubernetesStorage: true
    kubernetesSystem: true
    kubeSchedulerAlerting: true
    kubeSchedulerRecording: true
    kubeStateMetrics: true
    network: true
    node: true
    nodeExporterAlerting: true
    nodeExporterRecording: true
    prometheus: true
    prometheusOperator: true
EOF

  log "values.yaml 生成完成: $VALUES_FILE ✓"
}

# ========================== 阶段四: 安装 Prometheus Stack ==========================

phase_install() {
  info "========== 阶段四: 安装 kube-prometheus-stack =========="

  log "创建命名空间..."
  kubectl create namespace "$NAMESPACE" 2>/dev/null || \
    log "命名空间 $NAMESPACE 已存在"

  if helm status "$CHART_NAME" -n "$NAMESPACE" &>/dev/null; then
    log "kube-prometheus-stack 已安装，执行 upgrade..."
    helm upgrade "$CHART_NAME" prometheus-community/kube-prometheus-stack \
      -n "$NAMESPACE" \
      -f "$VALUES_FILE" \
      --version "$CHART_VERSION" \
      --wait \
      --timeout 10m \
      2>&1 | tee -a "$LOG_FILE"
  else
    log "安装 kube-prometheus-stack v${CHART_VERSION}..."
    helm install "$CHART_NAME" prometheus-community/kube-prometheus-stack \
      -n "$NAMESPACE" \
      -f "$VALUES_FILE" \
      --version "$CHART_VERSION" \
      --wait \
      --timeout 10m \
      2>&1 | tee -a "$LOG_FILE"
  fi

  log "kube-prometheus-stack 安装完成 ✓"
}

# ========================== 阶段五: 部署自定义告警规则 ==========================

phase_deploy_alert_rules() {
  info "========== 阶段五: 部署自定义告警规则 =========="

  cat << 'EOF' | kubectl apply -f -
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: opencloud-custom-rules
  namespace: monitoring
  labels:
    release: kube-prometheus-stack
spec:
  groups:
    # ===== 节点资源告警 =====
    - name: node.rules
      interval: 1m
      rules:
        - alert: NodeCPUHighUsage
          expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "节点 CPU 使用率过高"
            description: "节点 {{ $labels.instance }} CPU 使用率: {{ $value | printf \"%.2f\" }}%"

        - alert: NodeMemoryHighUsage
          expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "节点内存使用率过高"
            description: "节点 {{ $labels.instance }} 内存使用率: {{ $value | printf \"%.2f\" }}%"

        - alert: NodeDiskSpaceLow
          expr: (1 - (node_filesystem_avail_bytes{fstype!~"tmpfs|fuse.lxcfs"} / node_filesystem_size_bytes{fstype!~"tmpfs|fuse.lxcfs"})) * 100 > 85
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "节点磁盘空间不足"
            description: "节点 {{ $labels.instance }} 挂载点 {{ $labels.mountpoint }} 使用率: {{ $value | printf \"%.2f\" }}%"

        - alert: NodeDown
          expr: up{job="node-exporter"} == 0
          for: 2m
          labels:
            severity: critical
          annotations:
            summary: "节点不可达"
            description: "节点 {{ $labels.instance }} 已离线超过 2 分钟"

    # ===== Kubernetes 容器告警 =====
    - name: k8s.rules
      interval: 1m
      rules:
        - alert: PodCrashLooping
          expr: kube_pod_container_status_restarts_total > 5
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Pod 频繁重启"
            description: "Pod {{ $labels.namespace }}/{{ $labels.pod }} 重启次数: {{ $value }}"

        - alert: PodOOMKilled
          expr: kube_pod_container_status_last_terminated_reason{reason="OOMKilled"} == 1
          for: 1m
          labels:
            severity: warning
          annotations:
            summary: "Pod 因内存不足被杀死"
            description: "Pod {{ $labels.namespace }}/{{ $labels.pod }} 容器 {{ $labels.container }} 被 OOM Kill"

        - alert: DeploymentUnavailable
          expr: kube_deployment_status_replicas_unavailable > 0
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Deployment 存在不可用副本"
            description: "Deployment {{ $labels.namespace }}/{{ $labels.deployment }} 有 {{ $value }} 个副本不可用"

        - alert: PVCSpaceLow
          expr: (kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes) * 100 > 85
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "PVC 存储空间不足"
            description: "PVC {{ $labels.namespace }}/{{ $labels.persistentvolumeclaim }} 使用率: {{ $value | printf \"%.2f\" }}%"

    # ===== API Server 告警 =====
    - name: apiserver.rules
      interval: 1m
      rules:
        - alert: APIServerHighErrorRate
          expr: sum(rate(apiserver_request_total{code=~"5.."}[5m])) / sum(rate(apiserver_request_total[5m])) * 100 > 5
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "API Server 错误率过高"
            description: "API Server 5xx 错误率: {{ $value | printf \"%.2f\" }}%"
EOF

  log "自定义告警规则部署完成 ✓"
}

# ========================== 阶段六: 导入常用 Dashboard ==========================

phase_import_dashboards() {
  info "========== 阶段六: 导入常用 Grafana Dashboard =========="

  # Dashboard 列表（ID:名称）
  local dashboards=(
    "1860:Node Exporter Full"
    "7249:Kubernetes Cluster Overview"
    "6781:Kubernetes Pod Resources"
    "9614:Nginx Ingress Controller"
    "13770:Kubernetes Overview"
  )

  for dashboard_info in "${dashboards[@]}"; do
    local id="${dashboard_info%%:*}"
    local name="${dashboard_info##*:}"
    log "创建 Dashboard ConfigMap: $name (ID: $id)"

    kubectl create configmap "grafana-dashboard-${id}" \
      -n "$NAMESPACE" \
      --from-literal="dashboard-${id}.json={}" \
      --dry-run=client -o yaml | \
      kubectl annotate --local -f - \
        "grafana_folder=OpenCloud" 2>/dev/null | \
      kubectl apply -f - 2>/dev/null || true
  done

  log "提示: 请在 Grafana UI 手动导入 Dashboard，ID: 1860, 7249, 6781, 9614, 2842"
}

# ========================== 查看状态 ==========================

phase_status() {
  info "===== Prometheus 监控栈状态 ====="

  echo ""
  log "Pod 状态:"
  kubectl get pods -n "$NAMESPACE" -o wide

  echo ""
  log "Service 状态:"
  kubectl get svc -n "$NAMESPACE"

  echo ""
  log "PVC 状态:"
  kubectl get pvc -n "$NAMESPACE"

  echo ""
  log "Ingress 状态:"
  kubectl get ingress -n "$NAMESPACE" 2>/dev/null || true

  echo ""
  log "PrometheusRule 数量:"
  kubectl get prometheusrule -n "$NAMESPACE" 2>/dev/null | wc -l

  echo ""
  log "=============================="
  log "访问地址（需配置 DNS 或 hosts）:"
  log "  Prometheus:   http://${PROMETHEUS_DOMAIN}"
  log "  Alertmanager: http://${ALERTMANAGER_DOMAIN}"
  log "  Grafana:      http://${GRAFANA_DOMAIN}"
  log "  Grafana 账号: admin / ${GRAFANA_ADMIN_PASSWORD}"
  log "=============================="
}

# ========================== 卸载 ==========================

phase_uninstall() {
  warn "开始卸载 kube-prometheus-stack..."
  helm uninstall "$CHART_NAME" -n "$NAMESPACE" 2>&1 | tee -a "$LOG_FILE" || true
  kubectl delete namespace "$NAMESPACE" 2>/dev/null || true
  log "卸载完成"
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}用法: $0 [选项]${NC}"
  echo ""
  echo "选项:"
  echo "  --all        完整部署（默认）"
  echo "  --helm       安装 Helm"
  echo "  --repos      添加 Helm 仓库"
  echo "  --values     生成 values.yaml"
  echo "  --install    安装/升级 Prometheus Stack"
  echo "  --rules      部署自定义告警规则"
  echo "  --dashboards 导入 Dashboard"
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
      phase_install_helm
      phase_add_repos
      phase_generate_values
      phase_install
      phase_deploy_alert_rules
      log "=============================================="
      log "Prometheus 监控栈部署完成! 🎉"
      log "Prometheus:   http://${PROMETHEUS_DOMAIN}"
      log "Alertmanager: http://${ALERTMANAGER_DOMAIN}"
      log "Grafana:      http://${GRAFANA_DOMAIN}"
      log "Grafana 账号: admin / ${GRAFANA_ADMIN_PASSWORD}"
      log "=============================================="
      phase_status
      ;;
    --helm)       phase_install_helm ;;
    --repos)      phase_add_repos ;;
    --values)     phase_generate_values ;;
    --install)    check_prerequisites && phase_install ;;
    --rules)      phase_deploy_alert_rules ;;
    --dashboards) phase_import_dashboards ;;
    --status)     phase_status ;;
    --uninstall)  phase_uninstall ;;
    --help)       usage ;;
    *)            error "未知选项: $action，使用 --help 查看帮助" ;;
  esac
}

main "$@"
