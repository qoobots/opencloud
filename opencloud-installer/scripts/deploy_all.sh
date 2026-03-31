#!/bin/bash
# =============================================================================
# OpenCloud 云平台 — 一键自动化部署总入口脚本
# 版本: 1.0.0
# 描述: 按顺序自动化部署 Ceph → OpenStack → Kubernetes → Prometheus → Grafana
# 使用方式: bash deploy_all.sh [--all | --help | 单独组件选项]
# =============================================================================

set -euo pipefail

# ========================== 颜色 & Banner ==========================
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="/var/log/opencloud-deploy"
DEPLOY_LOG="$LOG_DIR/deploy_all_$(date +%Y%m%d_%H%M%S).log"

# ========================== 全局配置 ==========================
# 控制哪些组件需要部署（true=部署，false=跳过）
DEPLOY_CEPH=true
DEPLOY_OPENSTACK=true
DEPLOY_KUBERNETES=true
DEPLOY_PROMETHEUS=true
DEPLOY_GRAFANA=true

# 子脚本路径
SCRIPT_CEPH="$SCRIPT_DIR/deploy_ceph.sh"
SCRIPT_OPENSTACK="$SCRIPT_DIR/deploy_openstack.sh"
SCRIPT_KUBERNETES="$SCRIPT_DIR/deploy_kubernetes.sh"
SCRIPT_PROMETHEUS="$SCRIPT_DIR/deploy_prometheus.sh"
SCRIPT_GRAFANA="$SCRIPT_DIR/deploy_grafana.sh"

# 部署状态记录文件
STATUS_FILE="$LOG_DIR/deploy_status.txt"

# ========================== 工具函数 ==========================

log()     { echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$DEPLOY_LOG"; }
warn()    { echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$DEPLOY_LOG"; }
error()   { echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$DEPLOY_LOG"; exit 1; }
info()    { echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$DEPLOY_LOG"; }
success() { echo -e "${GREEN}${BOLD}[$(date '+%Y-%m-%d %H:%M:%S')] [OK]${NC} $*" | tee -a "$DEPLOY_LOG"; }
failed()  { echo -e "${RED}${BOLD}[$(date '+%Y-%m-%d %H:%M:%S')] [FAIL]${NC} $*" | tee -a "$DEPLOY_LOG"; }

print_banner() {
  echo -e "${BLUE}${BOLD}"
  cat << 'BANNER'
  ██████╗ ██████╗ ███████╗███╗   ██╗ ██████╗██╗      ██████╗ ██╗   ██╗██████╗
 ██╔═══██╗██╔══██╗██╔════╝████╗  ██║██╔════╝██║     ██╔═══██╗██║   ██║██╔══██╗
 ██║   ██║██████╔╝█████╗  ██╔██╗ ██║██║     ██║     ██║   ██║██║   ██║██║  ██║
 ██║   ██║██╔═══╝ ██╔══╝  ██║╚██╗██║██║     ██║     ██║   ██║██║   ██║██║  ██║
 ╚██████╔╝██║     ███████╗██║ ╚████║╚██████╗███████╗╚██████╔╝╚██████╔╝██████╔╝
  ╚═════╝ ╚═╝     ╚══════╝╚═╝  ╚═══╝ ╚═════╝╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝

         云计算平台自动化部署系统 v1.0.0
BANNER
  echo -e "${NC}"
}

print_deploy_plan() {
  echo -e "${CYAN}${BOLD}========== 部署计划 ==========${NC}"
  local step=1
  [[ "$DEPLOY_CEPH" == true ]]       && echo -e "  ${step}. ${GREEN}✓${NC} Ceph 分布式存储" && ((step++)) || echo -e "  -. ${YELLOW}○${NC} Ceph（已跳过）"
  [[ "$DEPLOY_OPENSTACK" == true ]]  && echo -e "  ${step}. ${GREEN}✓${NC} OpenStack 虚拟化平台" && ((step++)) || echo -e "  -. ${YELLOW}○${NC} OpenStack（已跳过）"
  [[ "$DEPLOY_KUBERNETES" == true ]] && echo -e "  ${step}. ${GREEN}✓${NC} Kubernetes 容器平台" && ((step++)) || echo -e "  -. ${YELLOW}○${NC} Kubernetes（已跳过）"
  [[ "$DEPLOY_PROMETHEUS" == true ]] && echo -e "  ${step}. ${GREEN}✓${NC} Prometheus 监控系统" && ((step++)) || echo -e "  -. ${YELLOW}○${NC} Prometheus（已跳过）"
  [[ "$DEPLOY_GRAFANA" == true ]]    && echo -e "  ${step}. ${GREEN}✓${NC} Grafana 可视化平台" && ((step++)) || echo -e "  -. ${YELLOW}○${NC} Grafana（已跳过）"
  echo -e "${CYAN}================================${NC}"
  echo ""
}

update_status() {
  local component="$1"
  local status="$2"    # SUCCESS / FAILED / SKIPPED / RUNNING
  local timestamp
  timestamp=$(date '+%Y-%m-%d %H:%M:%S')

  # 更新状态文件
  if grep -q "^$component " "$STATUS_FILE" 2>/dev/null; then
    sed -i "s/^$component .*/$component $status $timestamp/" "$STATUS_FILE"
  else
    echo "$component $status $timestamp" >> "$STATUS_FILE"
  fi
}

print_summary() {
  echo ""
  echo -e "${BLUE}${BOLD}╔══════════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}${BOLD}║           OpenCloud 部署结果汇总                     ║${NC}"
  echo -e "${BLUE}${BOLD}╚══════════════════════════════════════════════════════╝${NC}"

  local all_ok=true
  while IFS=' ' read -r component status timestamp rest; do
    local icon color
    case "$status" in
      SUCCESS) icon="✅"; color="$GREEN" ;;
      FAILED)  icon="❌"; color="$RED"; all_ok=false ;;
      SKIPPED) icon="⏭️ "; color="$YELLOW" ;;
      RUNNING) icon="🔄"; color="$CYAN"; all_ok=false ;;
      *)       icon="❓"; color="$NC" ;;
    esac
    printf "  %s ${color}%-20s${NC} %s\n" "$icon" "$component" "$timestamp"
  done < "$STATUS_FILE"

  echo ""
  if [[ "$all_ok" == true ]]; then
    echo -e "${GREEN}${BOLD}  🎉 所有组件部署成功！${NC}"
  else
    echo -e "${RED}${BOLD}  ⚠️  部分组件部署失败，请检查日志: $DEPLOY_LOG${NC}"
  fi
  echo ""
  echo -e "${CYAN}  部署日志: $DEPLOY_LOG${NC}"
  echo -e "${CYAN}  状态文件: $STATUS_FILE${NC}"
}

check_script_exists() {
  local script="$1"
  local name="$2"
  if [[ ! -f "$script" ]]; then
    error "部署脚本不存在: $script，请确保所有脚本在同一目录下"
  fi
  # 确保脚本有执行权限
  chmod +x "$script"
}

confirm_deploy() {
  local skip_confirm="${1:-false}"
  echo -e "${YELLOW}${BOLD}⚠️  即将开始自动化部署，此操作将修改系统配置！${NC}"
  echo ""
  print_deploy_plan
  # --yes / -y 参数时跳过交互（适用于 GUI/CI 非交互环境）
  if [[ "$skip_confirm" == "true" ]]; then
    log "已通过 --yes 参数自动确认部署"
    echo ""
    return 0
  fi
  read -r -p "确认继续部署？[y/N] " confirm
  if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    log "部署已取消"
    exit 0
  fi
  echo ""
}

# ========================== 部署函数 ==========================

run_component() {
  local name="$1"
  local script="$2"
  local args="${3:---all}"

  info "=========================================="
  info "开始部署: $name"
  info "脚本: $script $args"
  info "=========================================="

  update_status "$name" "RUNNING"
  local start_time
  start_time=$(date +%s)

  if bash "$script" "$args" 2>&1 | tee -a "$DEPLOY_LOG"; then
    local end_time elapsed
    end_time=$(date +%s)
    elapsed=$((end_time - start_time))
    success "$name 部署成功 ✓ (耗时: ${elapsed}s)"
    update_status "$name" "SUCCESS"
    return 0
  else
    failed "$name 部署失败 ✗"
    update_status "$name" "FAILED"
    return 1
  fi
}

deploy_ceph() {
  check_script_exists "$SCRIPT_CEPH" "Ceph"
  run_component "Ceph" "$SCRIPT_CEPH" "--all"
}

deploy_openstack() {
  check_script_exists "$SCRIPT_OPENSTACK" "OpenStack"
  run_component "OpenStack" "$SCRIPT_OPENSTACK" "--all"
}

deploy_kubernetes() {
  check_script_exists "$SCRIPT_KUBERNETES" "Kubernetes"
  run_component "Kubernetes" "$SCRIPT_KUBERNETES" "--all"
}

deploy_prometheus() {
  check_script_exists "$SCRIPT_PROMETHEUS" "Prometheus"
  run_component "Prometheus" "$SCRIPT_PROMETHEUS" "--all"
}

deploy_grafana() {
  check_script_exists "$SCRIPT_GRAFANA" "Grafana"
  run_component "Grafana" "$SCRIPT_GRAFANA" "--all"
}

# ========================== 主部署流程 ==========================

deploy_all() {
  local failed_components=()

  # ---- Step 1: Ceph ----
  if [[ "$DEPLOY_CEPH" == true ]]; then
    if ! deploy_ceph; then
      failed_components+=("Ceph")
      warn "Ceph 部署失败，后续依赖 Ceph 的组件可能受影响"
    fi
  else
    update_status "Ceph" "SKIPPED"
    log "跳过 Ceph 部署"
  fi

  # ---- Step 2: OpenStack ----
  if [[ "$DEPLOY_OPENSTACK" == true ]]; then
    if ! deploy_openstack; then
      failed_components+=("OpenStack")
    fi
  else
    update_status "OpenStack" "SKIPPED"
    log "跳过 OpenStack 部署"
  fi

  # ---- Step 3: Kubernetes ----
  if [[ "$DEPLOY_KUBERNETES" == true ]]; then
    if ! deploy_kubernetes; then
      failed_components+=("Kubernetes")
      warn "Kubernetes 部署失败，Prometheus 和 Grafana 无法部署"
      DEPLOY_PROMETHEUS=false
      DEPLOY_GRAFANA=false
    fi
  else
    update_status "Kubernetes" "SKIPPED"
    log "跳过 Kubernetes 部署"
  fi

  # ---- Step 4: Prometheus ----
  if [[ "$DEPLOY_PROMETHEUS" == true ]]; then
    if ! deploy_prometheus; then
      failed_components+=("Prometheus")
    fi
  else
    update_status "Prometheus" "SKIPPED"
    log "跳过 Prometheus 部署"
  fi

  # ---- Step 5: Grafana ----
  if [[ "$DEPLOY_GRAFANA" == true ]]; then
    if ! deploy_grafana; then
      failed_components+=("Grafana")
    fi
  else
    update_status "Grafana" "SKIPPED"
    log "跳过 Grafana 部署"
  fi

  # ---- 输出汇总 ----
  print_summary

  if [[ ${#failed_components[@]} -gt 0 ]]; then
    error "以下组件部署失败: ${failed_components[*]}，请检查日志: $DEPLOY_LOG"
  fi
}

# ========================== 状态查询 ==========================

show_status() {
  info "========== 查询各组件部署状态 =========="

  echo -e "\n${CYAN}--- Ceph 状态 ---${NC}"
  ceph -s 2>/dev/null || warn "Ceph 未部署或不可达"

  echo -e "\n${CYAN}--- OpenStack 状态 ---${NC}"
  if [[ -f "/etc/kolla/admin-openrc.sh" ]]; then
    source /etc/kolla/admin-openrc.sh
    openstack service list 2>/dev/null || warn "OpenStack 不可达"
  else
    warn "OpenStack 未部署（/etc/kolla/admin-openrc.sh 不存在）"
  fi

  echo -e "\n${CYAN}--- Kubernetes 状态 ---${NC}"
  kubectl get nodes 2>/dev/null || warn "Kubernetes 未部署或 kubeconfig 未配置"

  echo -e "\n${CYAN}--- Prometheus 状态 ---${NC}"
  kubectl get pods -n monitoring -l "app=prometheus" 2>/dev/null || \
    warn "Prometheus 未部署"

  echo -e "\n${CYAN}--- Grafana 状态 ---${NC}"
  kubectl get pods -n monitoring -l "app.kubernetes.io/name=grafana" 2>/dev/null || \
    warn "Grafana 未部署"

  echo ""
  if [[ -f "$STATUS_FILE" ]]; then
    print_summary
  fi
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}${BOLD}用法: $0 [选项]${NC}"
  echo ""
  echo -e "${BOLD}总体选项:${NC}"
  echo "  --all              完整部署所有组件（按依赖顺序）"
  echo "  --status           查看所有组件部署状态"
  echo "  --yes, -y          跳过交互确认（适用于 GUI/CI 非交互环境）"
  echo "  --help             显示帮助"
  echo ""
  echo -e "${BOLD}单独部署:${NC}"
  echo "  --ceph             仅部署 Ceph"
  echo "  --openstack        仅部署 OpenStack"
  echo "  --kubernetes       仅部署 Kubernetes"
  echo "  --prometheus       仅部署 Prometheus"
  echo "  --grafana          仅部署 Grafana"
  echo ""
  echo -e "${BOLD}跳过某些组件（组合使用）:${NC}"
  echo "  --skip-ceph        跳过 Ceph"
  echo "  --skip-openstack   跳过 OpenStack"
  echo "  --skip-kubernetes  跳过 Kubernetes"
  echo "  --skip-prometheus  跳过 Prometheus"
  echo "  --skip-grafana     跳过 Grafana"
  echo ""
  echo -e "${BOLD}示例:${NC}"
  echo "  $0 --all                          # 全量部署"
  echo "  $0 --all --yes                    # 全量部署（跳过确认，GUI/CI 使用）"
  echo "  $0 --all --skip-openstack         # 跳过 OpenStack，部署其余"
  echo "  $0 --kubernetes                   # 仅部署 Kubernetes"
  echo "  $0 --prometheus --grafana         # 仅部署监控组件"
  echo ""
}

main() {
  mkdir -p "$LOG_DIR"
  > "$STATUS_FILE"
  log "日志文件: $DEPLOY_LOG"

  print_banner

  # 解析参数
  local specific_components=()
  local args=("$@")
  local yes_flag=false   # --yes / -y 跳过交互确认

  for arg in "${args[@]}"; do
    case "$arg" in
      --yes | -y)         yes_flag=true ;;
      --skip-ceph)        DEPLOY_CEPH=false ;;
      --skip-openstack)   DEPLOY_OPENSTACK=false ;;
      --skip-kubernetes)  DEPLOY_KUBERNETES=false ;;
      --skip-prometheus)  DEPLOY_PROMETHEUS=false ;;
      --skip-grafana)     DEPLOY_GRAFANA=false ;;
      --ceph)             specific_components+=("ceph") ;;
      --openstack)        specific_components+=("openstack") ;;
      --kubernetes)       specific_components+=("kubernetes") ;;
      --prometheus)       specific_components+=("prometheus") ;;
      --grafana)          specific_components+=("grafana") ;;
    esac
  done

  # 判断主操作
  local action="${1:---all}"

  # 如果指定了具体组件，只部署这些
  if [[ ${#specific_components[@]} -gt 0 ]]; then
    DEPLOY_CEPH=false
    DEPLOY_OPENSTACK=false
    DEPLOY_KUBERNETES=false
    DEPLOY_PROMETHEUS=false
    DEPLOY_GRAFANA=false
    for comp in "${specific_components[@]}"; do
      case "$comp" in
        ceph)        DEPLOY_CEPH=true ;;
        openstack)   DEPLOY_OPENSTACK=true ;;
        kubernetes)  DEPLOY_KUBERNETES=true ;;
        prometheus)  DEPLOY_PROMETHEUS=true ;;
        grafana)     DEPLOY_GRAFANA=true ;;
      esac
    done
  fi

  case "$action" in
    --help | -h)
      usage
      ;;
    --status)
      show_status
      ;;
    --all | --skip-* | --ceph | --openstack | --kubernetes | --prometheus | --grafana)
      [[ $EUID -ne 0 ]] || true  # 不强制 root，子脚本自己检查
      confirm_deploy "$yes_flag"
      log "开始 OpenCloud 全栈部署..."
      deploy_all
      ;;
    *)
      error "未知选项: $action，使用 --help 查看帮助"
      ;;
  esac
}

main "$@"
