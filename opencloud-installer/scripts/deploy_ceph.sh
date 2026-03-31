#!/bin/bash
# =============================================================================
# Ceph 自动化部署脚本
# 版本: 1.0.0
# 描述: 使用 cephadm 自动化部署 Ceph 分布式存储集群
# 适用系统: Ubuntu 22.04 LTS / Rocky Linux 9
# 使用方式: bash deploy_ceph.sh [--bootstrap | --add-nodes | --create-pools | --status]
# =============================================================================

set -euo pipefail

# ========================== 颜色定义 ==========================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ========================== 配置变量 ==========================
# !! 部署前请根据实际环境修改以下配置 !!

# 当前节点 IP（MON 引导 IP）
MON_IP="192.168.1.11"

# 集群网络（OSD 节点之间的数据复制网）
CLUSTER_NETWORK="192.168.2.0/24"

# 公共访问网络
PUBLIC_NETWORK="192.168.1.0/24"

# Ceph Dashboard 管理员密码
DASHBOARD_PASSWORD="Ceph@Admin2024"

# 集群中所有节点（空格分隔）
CEPH_NODES=(
  "ceph-node1:192.168.1.11"
  "ceph-node2:192.168.1.12"
  "ceph-node3:192.168.1.13"
  "ceph-node4:192.168.1.14"
  "ceph-node5:192.168.1.15"
)

# MON 节点列表（建议奇数个，3/5/7）
MON_NODES="ceph-node1,ceph-node2,ceph-node3"

# MGR 节点列表
MGR_NODES="ceph-node1,ceph-node2"

# MDS 节点列表（CephFS 使用）
MDS_NODES="ceph-node4,ceph-node5"

# RGW 节点列表（对象存储使用）
RGW_NODES="ceph-node4 ceph-node5"

# CephFS 文件系统名称
CEPHFS_NAME="cephfs"

# RGW Realm / Zone 配置
RGW_REALM="opencloud"
RGW_ZONE="cn-east-1"
RGW_PORT=7480

# 需要创建的存储池（用于 OpenStack 对接）
POOLS=(
  "rbd:128"        # 通用块存储
  "volumes:128"    # Cinder 卷
  "images:128"     # Glance 镜像
  "vms:128"        # Nova 虚拟机
  "backups:64"     # Cinder 备份
)

# 日志文件
LOG_FILE="/var/log/deploy_ceph_$(date +%Y%m%d_%H%M%S).log"

# ========================== 工具函数 ==========================

log() {
  echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$LOG_FILE"
}

warn() {
  echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$LOG_FILE"
}

error() {
  echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$LOG_FILE"
  exit 1
}

info() {
  echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$LOG_FILE"
}

check_root() {
  if [[ $EUID -ne 0 ]]; then
    error "请使用 root 用户运行此脚本"
  fi
}

check_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    log "检测到操作系统: $NAME $VERSION_ID"
    if [[ "$ID" != "ubuntu" && "$ID" != "rocky" && "$ID" != "centos" ]]; then
      warn "当前系统 $ID 未经充分测试，继续执行..."
    fi
  fi
}

wait_for_health() {
  local timeout=${1:-300}
  local interval=10
  local elapsed=0
  log "等待 Ceph 集群健康状态..."
  while [[ $elapsed -lt $timeout ]]; do
    if ceph health | grep -q "HEALTH_OK"; then
      log "Ceph 集群状态: HEALTH_OK ✓"
      return 0
    fi
    sleep $interval
    elapsed=$((elapsed + interval))
    warn "等待中... (${elapsed}s / ${timeout}s)"
  done
  warn "超时，当前集群状态:"
  ceph health detail
}

# ========================== 阶段一: 系统初始化 ==========================

phase_prepare() {
  info "========== 阶段一: 系统初始化 =========="

  log "关闭防火墙..."
  systemctl stop firewalld 2>/dev/null || true
  systemctl disable firewalld 2>/dev/null || true

  log "关闭 SELinux..."
  setenforce 0 2>/dev/null || true
  sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config 2>/dev/null || true

  log "设置时区..."
  timedatectl set-timezone Asia/Shanghai
  systemctl enable --now chronyd 2>/dev/null || systemctl enable --now ntpd 2>/dev/null || true

  log "安装基础依赖..."
  if command -v apt &>/dev/null; then
    apt update -y
    apt install -y curl wget python3 lvm2 chrony
  elif command -v dnf &>/dev/null; then
    dnf install -y curl wget python3 lvm2 chrony
  fi

  log "系统初始化完成 ✓"
}

# ========================== 阶段二: 安装 cephadm ==========================

phase_install_cephadm() {
  info "========== 阶段二: 安装 cephadm =========="

  if command -v cephadm &>/dev/null; then
    log "cephadm 已安装，跳过..."
    return 0
  fi

  if command -v apt &>/dev/null; then
    apt update -y
    apt install -y cephadm
  elif command -v dnf &>/dev/null; then
    dnf install -y cephadm
  else
    log "通过官方脚本安装 cephadm..."
    curl --silent --remote-name --location \
      https://github.com/ceph/ceph/raw/quincy/src/cephadm/cephadm
    chmod +x cephadm
    mv cephadm /usr/local/bin/cephadm
  fi

  log "cephadm 安装完成: $(cephadm version) ✓"
}

# ========================== 阶段三: 引导集群 ==========================

phase_bootstrap() {
  info "========== 阶段三: 引导 Ceph 集群 =========="

  if ceph -s &>/dev/null 2>&1; then
    warn "Ceph 集群已存在，跳过引导..."
    return 0
  fi

  log "开始引导 Ceph 集群，MON IP: $MON_IP"
  cephadm bootstrap \
    --mon-ip "$MON_IP" \
    --cluster-network "$CLUSTER_NETWORK" \
    --initial-dashboard-user admin \
    --initial-dashboard-password "$DASHBOARD_PASSWORD" \
    --skip-monitoring-stack \
    --skip-pull \
    2>&1 | tee -a "$LOG_FILE"

  log "安装 ceph-common CLI..."
  cephadm install ceph-common

  # 将 ceph 命令加入 PATH
  echo 'export PATH=$PATH:/usr/bin' >> /etc/profile.d/ceph.sh
  source /etc/profile.d/ceph.sh 2>/dev/null || true

  log "集群引导完成 ✓"
  ceph -s
}

# ========================== 阶段四: 添加节点 ==========================

phase_add_nodes() {
  info "========== 阶段四: 添加集群节点 =========="

  log "配置 SSH 免密访问..."
  for node_info in "${CEPH_NODES[@]}"; do
    node_name="${node_info%%:*}"
    node_ip="${node_info##*:}"
    if [[ "$node_ip" == "$MON_IP" ]]; then
      log "跳过本机节点 $node_name"
      continue
    fi
    log "拷贝 Ceph 公钥到 $node_name ($node_ip)..."
    ssh-copy-id -f -i /etc/ceph/ceph.pub "root@$node_ip" 2>/dev/null || \
      warn "无法自动拷贝密钥到 $node_name，请手动执行: ssh-copy-id -f -i /etc/ceph/ceph.pub root@$node_ip"
  done

  log "向集群添加节点..."
  for node_info in "${CEPH_NODES[@]}"; do
    node_name="${node_info%%:*}"
    node_ip="${node_info##*:}"
    if [[ "$node_ip" == "$MON_IP" ]]; then
      continue
    fi
    log "添加节点: $node_name ($node_ip)"
    ceph orch host add "$node_name" "$node_ip" || warn "节点 $node_name 可能已存在"
  done

  log "当前节点列表:"
  ceph orch host ls
}

# ========================== 阶段五: 部署服务 ==========================

phase_deploy_services() {
  info "========== 阶段五: 部署 Ceph 服务 =========="

  # 部署 MON
  log "部署 MON 节点: $MON_NODES"
  ceph orch apply mon "$MON_NODES"
  sleep 10

  # 部署 MGR
  log "部署 MGR 节点: $MGR_NODES"
  ceph orch apply mgr "$MGR_NODES"
  sleep 10

  # 部署 OSD（自动发现所有可用裸盘）
  log "自动发现并添加 OSD..."
  ceph orch apply osd --all-available-devices
  log "等待 OSD 部署完成（60秒）..."
  sleep 60

  log "查看 OSD 状态:"
  ceph osd tree

  # 部署 MDS（CephFS）
  log "创建 CephFS: $CEPHFS_NAME"
  ceph fs volume create "$CEPHFS_NAME" || warn "CephFS $CEPHFS_NAME 可能已存在"

  log "部署 MDS 节点: $MDS_NODES"
  ceph orch apply mds "$CEPHFS_NAME" "$MDS_NODES"
  sleep 10

  # 部署 RGW（对象存储）
  log "配置 RGW Realm/Zone..."
  radosgw-admin realm create --rgw-realm="$RGW_REALM" --default 2>/dev/null || warn "Realm 已存在"
  radosgw-admin zonegroup create --rgw-zonegroup=default --master --default 2>/dev/null || warn "Zonegroup 已存在"
  radosgw-admin zone create --rgw-zonegroup=default --rgw-zone="$RGW_ZONE" --master --default 2>/dev/null || warn "Zone 已存在"

  log "部署 RGW 服务..."
  ceph orch apply rgw "$RGW_REALM" "$RGW_ZONE" \
    --placement="2 $RGW_NODES" \
    --port="$RGW_PORT"

  log "服务部署完成 ✓"
  ceph orch ls
}

# ========================== 阶段六: 创建存储池 ==========================

phase_create_pools() {
  info "========== 阶段六: 创建存储池 =========="

  for pool_info in "${POOLS[@]}"; do
    pool_name="${pool_info%%:*}"
    pg_num="${pool_info##*:}"

    if ceph osd pool ls | grep -q "^${pool_name}$"; then
      warn "存储池 $pool_name 已存在，跳过..."
      continue
    fi

    log "创建存储池: $pool_name (PG数: $pg_num)"
    ceph osd pool create "$pool_name" "$pg_num"
    ceph osd pool application enable "$pool_name" rbd

    if [[ "$pool_name" != "vms" && "$pool_name" != "backups" ]]; then
      rbd pool init "$pool_name" 2>/dev/null || true
    fi
  done

  log "创建 OpenStack 对接用户..."

  # Glance 用户
  ceph auth get-or-create client.glance \
    mon 'profile rbd' \
    osd 'profile rbd pool=images' \
    mgr 'profile rbd pool=images' \
    > /etc/ceph/ceph.client.glance.keyring

  # Cinder 用户
  ceph auth get-or-create client.cinder \
    mon 'profile rbd' \
    osd 'profile rbd pool=volumes, profile rbd pool=vms, profile rbd-read-only pool=images' \
    mgr 'profile rbd pool=volumes, profile rbd pool=vms' \
    > /etc/ceph/ceph.client.cinder.keyring

  # Nova 用户
  ceph auth get-or-create client.nova \
    mon 'profile rbd' \
    osd 'profile rbd pool=vms' \
    mgr 'profile rbd pool=vms' \
    > /etc/ceph/ceph.client.nova.keyring

  log "存储池创建完成 ✓"
  ceph osd pool ls detail
}

# ========================== 阶段七: 验证 ==========================

phase_verify() {
  info "========== 阶段七: 集群验证 =========="

  log "集群整体状态:"
  ceph -s

  log "集群容量:"
  ceph df

  log "OSD 状态:"
  ceph osd stat

  log "MON 状态:"
  ceph mon stat

  log "服务列表:"
  ceph orch ls

  log "节点列表:"
  ceph orch host ls

  wait_for_health 120

  log "=========================================="
  log "Ceph 集群部署完成! 🎉"
  log "Dashboard: https://${MON_IP}:8443"
  log "Dashboard 用户: admin"
  log "Dashboard 密码: $DASHBOARD_PASSWORD"
  log "KeyRing 文件目录: /etc/ceph/"
  log "=========================================="
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}用法: $0 [选项]${NC}"
  echo ""
  echo "选项:"
  echo "  --all            完整部署（默认）"
  echo "  --prepare        仅执行系统初始化"
  echo "  --bootstrap      仅引导集群"
  echo "  --add-nodes      仅添加节点"
  echo "  --deploy         仅部署服务"
  echo "  --create-pools   仅创建存储池"
  echo "  --status         查看集群状态"
  echo "  --help           显示帮助"
  echo ""
}

main() {
  check_root
  check_os
  mkdir -p "$(dirname $LOG_FILE)"
  log "日志文件: $LOG_FILE"

  local action="${1:---all}"

  case "$action" in
    --all)
      phase_prepare
      phase_install_cephadm
      phase_bootstrap
      phase_add_nodes
      phase_deploy_services
      phase_create_pools
      phase_verify
      ;;
    --prepare)      phase_prepare ;;
    --bootstrap)    phase_install_cephadm && phase_bootstrap ;;
    --add-nodes)    phase_add_nodes ;;
    --deploy)       phase_deploy_services ;;
    --create-pools) phase_create_pools ;;
    --status)
      ceph -s
      ceph df
      ceph orch ls
      ;;
    --help) usage ;;
    *)
      error "未知选项: $action，使用 --help 查看帮助"
      ;;
  esac
}

main "$@"
