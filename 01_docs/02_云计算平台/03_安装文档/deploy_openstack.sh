#!/bin/bash
# =============================================================================
# OpenStack 自动化部署脚本
# 版本: 1.0.0
# 描述: 使用 Kolla-Ansible 容器化自动化部署 OpenStack 云平台
# 适用系统: Ubuntu 22.04 LTS / Rocky Linux 9
# 使用方式: bash deploy_openstack.sh [--all | --prepare | --deploy | --post | --status]
# =============================================================================

set -euo pipefail

# ========================== 颜色定义 ==========================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ========================== 配置变量 ==========================
# !! 部署前请根据实际环境修改以下配置 !!

# OpenStack 版本
OS_RELEASE="2023.2"

# Kolla-Ansible 虚拟环境路径
KOLLA_VENV="/opt/kolla-venv"

# 配置目录
KOLLA_CONF_DIR="/etc/kolla"

# 管理网接口（节点间通信）
MGMT_INTERFACE="eth0"

# 外部网接口（浮动 IP 使用，无需配 IP）
EXTERNAL_INTERFACE="eth3"

# VIP 地址（HAProxy 虚拟 IP，需在管理网段内且未被占用）
INTERNAL_VIP="192.168.1.100"
EXTERNAL_VIP="10.0.0.100"

# Ceph 对接配置（需与 Ceph 集群配置一致）
CEPH_ENABLED=true
CEPH_NODE="ceph-node1"  # 用于获取 Ceph 配置的节点
CEPH_GLANCE_POOL="images"
CEPH_CINDER_POOL="volumes"
CEPH_NOVA_POOL="vms"

# OpenStack 管理员密码
ADMIN_PASSWORD="OpenStack@Admin2024"

# 是否启用高可用（3 个 controller 节点）
ENABLE_HA=true

# Inventory 文件路径
INVENTORY_FILE="$KOLLA_CONF_DIR/multinode"

# 日志文件
LOG_FILE="/var/log/deploy_openstack_$(date +%Y%m%d_%H%M%S).log"

# ========================== 工具函数 ==========================

log()   { echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$LOG_FILE"; }
warn()  { echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$LOG_FILE"; exit 1; }
info()  { echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$LOG_FILE"; }

check_root() {
  [[ $EUID -ne 0 ]] && error "请使用 root 用户运行此脚本"
}

activate_venv() {
  if [[ -f "$KOLLA_VENV/bin/activate" ]]; then
    source "$KOLLA_VENV/bin/activate"
    log "已激活虚拟环境: $KOLLA_VENV"
  else
    error "虚拟环境不存在: $KOLLA_VENV，请先执行 --prepare"
  fi
}

# ========================== 阶段一: 系统准备 ==========================

phase_prepare() {
  info "========== 阶段一: 系统与环境准备 =========="

  log "更新系统包..."
  if command -v apt &>/dev/null; then
    apt update -y
    apt upgrade -y
    apt install -y python3 python3-pip python3-venv python3-dev \
      libffi-dev gcc libssl-dev git sshpass ansible curl wget
  elif command -v dnf &>/dev/null; then
    dnf update -y
    dnf install -y python3 python3-pip python3-devel \
      libffi-devel gcc openssl-devel git sshpass ansible curl wget
  fi

  log "关闭防火墙..."
  systemctl stop firewalld 2>/dev/null || true
  systemctl disable firewalld 2>/dev/null || true

  log "关闭 SELinux..."
  setenforce 0 2>/dev/null || true
  sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config 2>/dev/null || true

  log "时间同步..."
  timedatectl set-timezone Asia/Shanghai
  systemctl enable --now chronyd 2>/dev/null || true

  log "创建 Python 虚拟环境..."
  python3 -m venv "$KOLLA_VENV"
  source "$KOLLA_VENV/bin/activate"

  log "升级 pip..."
  pip install --upgrade pip

  log "安装 Kolla-Ansible..."
  pip install "kolla-ansible==${OS_RELEASE}.*" 2>/dev/null || \
    pip install kolla-ansible

  pip install "ansible>=6,<8"

  log "初始化配置目录..."
  mkdir -p "$KOLLA_CONF_DIR"

  # 复制默认配置文件
  KOLLA_SHARE="$KOLLA_VENV/share/kolla-ansible"
  if [[ -d "$KOLLA_SHARE/etc_examples/kolla" ]]; then
    cp -rn "$KOLLA_SHARE/etc_examples/kolla/"* "$KOLLA_CONF_DIR/" 2>/dev/null || true
    cp -n "$KOLLA_SHARE/ansible/inventory/multinode" "$KOLLA_CONF_DIR/multinode" 2>/dev/null || true
  fi

  log "安装 Ansible Galaxy 依赖..."
  kolla-ansible install-deps 2>/dev/null || true

  log "系统准备完成 ✓"
}

# ========================== 阶段二: 生成配置 ==========================

phase_configure() {
  info "========== 阶段二: 生成 Kolla-Ansible 配置 =========="

  activate_venv

  # ---- 生成 globals.yml ----
  log "生成 globals.yml 配置..."
  cat > "$KOLLA_CONF_DIR/globals.yml" << EOF
# ====== Kolla-Ansible 全局配置 ======
# 基础配置
kolla_base_distro: "ubuntu"
kolla_install_type: "source"
openstack_release: "${OS_RELEASE}"

# 网络配置
network_interface: "${MGMT_INTERFACE}"
neutron_external_interface: "${EXTERNAL_INTERFACE}"
kolla_internal_vip_address: "${INTERNAL_VIP}"
kolla_external_vip_address: "${EXTERNAL_VIP}"

# HA 配置
enable_haproxy: "yes"
enable_keepalived: "yes"

# Neutron 配置
neutron_plugin_agent: "ovn"
enable_neutron_provider_networks: "yes"
enable_neutron_trunk: "yes"

# 启用的核心服务
enable_openstack_core: "yes"
enable_glance: "yes"
enable_nova: "yes"
enable_neutron: "yes"
enable_cinder: "yes"
enable_horizon: "yes"
enable_heat: "yes"
enable_placement: "yes"

# 可选服务
enable_barbican: "yes"
enable_designate: "yes"
enable_octavia: "yes"
enable_manila: "no"

# Ceph 存储后端配置
glance_backend_ceph: "$([ "$CEPH_ENABLED" == true ] && echo yes || echo no)"
cinder_backend_ceph: "$([ "$CEPH_ENABLED" == true ] && echo yes || echo no)"
nova_backend_ceph: "$([ "$CEPH_ENABLED" == true ] && echo yes || echo no)"
ceph_glance_pool_name: "${CEPH_GLANCE_POOL}"
ceph_cinder_pool_name: "${CEPH_CINDER_POOL}"
ceph_nova_pool_name: "${CEPH_NOVA_POOL}"

# Docker 镜像仓库（国内加速，可选）
# docker_registry: "registry.cn-hangzhou.aliyuncs.com"
# docker_namespace: "openstack.kolla"
EOF

  log "globals.yml 生成完成 ✓"

  # ---- 生成 Inventory ----
  log "生成 Inventory 文件..."
  if [[ "$ENABLE_HA" == true ]]; then
    cat > "$INVENTORY_FILE" << 'EOF'
[control]
controller01
controller02
controller03

[network]
network01

[compute]
compute01
compute02

[storage]
storage01

[monitoring]
controller01

[deployment]
localhost ansible_connection=local

[baremetal:children]
control
network
compute
storage
monitoring
EOF
  else
    cat > "$INVENTORY_FILE" << 'EOF'
[control]
controller01

[network]
controller01

[compute]
compute01
compute02

[storage]
storage01

[monitoring]
controller01

[deployment]
localhost ansible_connection=local

[baremetal:children]
control
network
compute
storage
monitoring
EOF
  fi

  log "Inventory 生成完成 ✓"

  # ---- 从 Ceph 集群获取配置 ----
  if [[ "$CEPH_ENABLED" == true ]]; then
    log "从 Ceph 节点 $CEPH_NODE 获取配置和密钥..."
    mkdir -p "$KOLLA_CONF_DIR"

    ssh "root@$CEPH_NODE" "cat /etc/ceph/ceph.conf" > "$KOLLA_CONF_DIR/ceph.conf" || \
      warn "无法获取 Ceph 配置，请手动复制 /etc/ceph/ceph.conf 到 $KOLLA_CONF_DIR/"

    for keyring in glance cinder nova; do
      ssh "root@$CEPH_NODE" "cat /etc/ceph/ceph.client.${keyring}.keyring" \
        > "$KOLLA_CONF_DIR/ceph.client.${keyring}.keyring" || \
        warn "无法获取 client.${keyring} keyring，请手动复制"
    done
    log "Ceph 配置获取完成 ✓"
  fi
}

# ========================== 阶段三: 生成密码 ==========================

phase_generate_passwords() {
  info "========== 阶段三: 生成服务密码 =========="

  activate_venv

  log "生成所有服务密码..."
  kolla-genpwd

  log "设置管理员密码..."
  sed -i "s/^keystone_admin_password:.*/keystone_admin_password: ${ADMIN_PASSWORD}/" \
    "$KOLLA_CONF_DIR/passwords.yml"

  log "密码生成完成 ✓"
  log "密码文件: $KOLLA_CONF_DIR/passwords.yml（请妥善保管！）"
}

# ========================== 阶段四: 预检查 ==========================

phase_prechecks() {
  info "========== 阶段四: 部署预检查 =========="

  activate_venv

  log "初始化目标节点..."
  kolla-ansible -i "$INVENTORY_FILE" bootstrap-servers 2>&1 | tee -a "$LOG_FILE"

  log "执行预检查..."
  kolla-ansible -i "$INVENTORY_FILE" prechecks 2>&1 | tee -a "$LOG_FILE"

  log "预检查完成 ✓"
}

# ========================== 阶段五: 拉取镜像 ==========================

phase_pull_images() {
  info "========== 阶段五: 拉取 Docker 镜像 =========="

  activate_venv

  log "拉取 Kolla 镜像（可能需要较长时间）..."
  kolla-ansible -i "$INVENTORY_FILE" pull 2>&1 | tee -a "$LOG_FILE"

  log "镜像拉取完成 ✓"
}

# ========================== 阶段六: 正式部署 ==========================

phase_deploy() {
  info "========== 阶段六: 正式部署 OpenStack =========="

  activate_venv

  log "开始部署 OpenStack（预计 30-60 分钟）..."
  kolla-ansible -i "$INVENTORY_FILE" deploy 2>&1 | tee -a "$LOG_FILE"

  log "部署完成 ✓"
}

# ========================== 阶段七: 初始化环境 ==========================

phase_post_deploy() {
  info "========== 阶段七: 初始化 OpenStack 环境 =========="

  activate_venv

  log "执行 post-deploy 初始化..."
  kolla-ansible -i "$INVENTORY_FILE" post-deploy 2>&1 | tee -a "$LOG_FILE"

  log "生成 admin-openrc.sh..."
  if [[ -f "$KOLLA_CONF_DIR/admin-openrc.sh" ]]; then
    log "admin-openrc.sh 已生成: $KOLLA_CONF_DIR/admin-openrc.sh"
  fi

  # 加载环境变量
  source "$KOLLA_CONF_DIR/admin-openrc.sh" 2>/dev/null || true

  # 安装 OpenStack CLI
  log "安装 OpenStack CLI..."
  pip install python-openstackclient python-cinderclient python-neutronclient \
    python-glanceclient python-novaclient 2>&1 | tee -a "$LOG_FILE"

  log "初始化完成 ✓"
}

# ========================== 阶段八: 初始化资源 ==========================

phase_init_resources() {
  info "========== 阶段八: 创建基础资源 =========="

  source "$KOLLA_CONF_DIR/admin-openrc.sh" 2>/dev/null || \
    warn "无法加载 admin-openrc.sh，跳过资源初始化"

  # 上传测试镜像
  log "上传 Cirros 测试镜像..."
  if ! openstack image show cirros &>/dev/null; then
    wget -q -O /tmp/cirros.img \
      http://download.cirros-cloud.net/0.6.2/cirros-0.6.2-x86_64-disk.img || \
      warn "Cirros 镜像下载失败，请手动上传"
    openstack image create "cirros" \
      --file /tmp/cirros.img \
      --disk-format qcow2 \
      --container-format bare \
      --public
    log "Cirros 镜像上传完成 ✓"
  fi

  # 创建 Flavor
  log "创建 Flavor..."
  openstack flavor create --id 1 --ram 512  --disk 1   --vcpus 1  m1.tiny   2>/dev/null || true
  openstack flavor create --id 2 --ram 2048 --disk 20  --vcpus 1  m1.small  2>/dev/null || true
  openstack flavor create --id 3 --ram 4096 --disk 40  --vcpus 2  m1.medium 2>/dev/null || true
  openstack flavor create --id 4 --ram 8192 --disk 80  --vcpus 4  m1.large  2>/dev/null || true
  log "Flavor 创建完成 ✓"

  # 创建外部网络
  log "创建外部网络..."
  if ! openstack network show external-net &>/dev/null; then
    openstack network create \
      --provider-network-type flat \
      --provider-physical-network physnet1 \
      --external external-net
    openstack subnet create \
      --network external-net \
      --subnet-range 10.0.0.0/24 \
      --allocation-pool start=10.0.0.100,end=10.0.0.200 \
      --gateway 10.0.0.1 \
      --no-dhcp external-subnet
    log "外部网络创建完成 ✓"
  fi

  # 配置安全组规则
  log "配置默认安全组规则..."
  openstack security group rule create --protocol icmp --ingress default 2>/dev/null || true
  openstack security group rule create --protocol tcp --dst-port 22 --ingress default 2>/dev/null || true
  openstack security group rule create --protocol tcp --dst-port 80 --ingress default 2>/dev/null || true
  openstack security group rule create --protocol tcp --dst-port 443 --ingress default 2>/dev/null || true

  log "基础资源初始化完成 ✓"
}

# ========================== 查看状态 ==========================

phase_status() {
  source "$KOLLA_CONF_DIR/admin-openrc.sh" 2>/dev/null || \
    error "请先完成部署，admin-openrc.sh 未找到"

  info "===== OpenStack 服务状态 ====="
  openstack service list
  echo ""
  openstack compute service list
  echo ""
  openstack network agent list
  echo ""
  openstack volume service list
  echo ""
  log "Horizon 控制台: http://${INTERNAL_VIP}/"
  log "管理员账号: admin"
  log "管理员密码: $ADMIN_PASSWORD"
}

# ========================== 升级 ==========================

phase_upgrade() {
  info "========== 执行 OpenStack 升级 =========="
  activate_venv
  kolla-ansible -i "$INVENTORY_FILE" upgrade 2>&1 | tee -a "$LOG_FILE"
  log "升级完成 ✓"
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}用法: $0 [选项]${NC}"
  echo ""
  echo "选项:"
  echo "  --all            完整部署（默认，包含所有阶段）"
  echo "  --prepare        安装依赖和 Kolla-Ansible"
  echo "  --configure      生成配置文件"
  echo "  --passwords      生成密码"
  echo "  --prechecks      执行预检查"
  echo "  --pull           拉取镜像"
  echo "  --deploy         执行部署"
  echo "  --post           初始化环境"
  echo "  --init           创建基础资源"
  echo "  --status         查看服务状态"
  echo "  --upgrade        升级 OpenStack"
  echo "  --help           显示帮助"
  echo ""
}

main() {
  check_root
  mkdir -p "$(dirname $LOG_FILE)"
  log "日志文件: $LOG_FILE"

  local action="${1:---all}"

  case "$action" in
    --all)
      phase_prepare
      phase_configure
      phase_generate_passwords
      phase_prechecks
      phase_pull_images
      phase_deploy
      phase_post_deploy
      phase_init_resources
      log "=============================================="
      log "OpenStack 部署完成! 🎉"
      log "Horizon URL: http://${INTERNAL_VIP}/"
      log "管理员账号: admin"
      log "管理员密码: ${ADMIN_PASSWORD}"
      log "环境配置文件: ${KOLLA_CONF_DIR}/admin-openrc.sh"
      log "=============================================="
      ;;
    --prepare)    phase_prepare ;;
    --configure)  phase_configure ;;
    --passwords)  phase_generate_passwords ;;
    --prechecks)  phase_prechecks ;;
    --pull)       phase_pull_images ;;
    --deploy)     phase_deploy ;;
    --post)       phase_post_deploy ;;
    --init)       phase_init_resources ;;
    --status)     phase_status ;;
    --upgrade)    phase_upgrade ;;
    --help)       usage ;;
    *)            error "未知选项: $action，使用 --help 查看帮助" ;;
  esac
}

main "$@"
