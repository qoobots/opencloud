#!/bin/bash
# =============================================================================
# Kubernetes 自动化部署脚本
# 版本: 1.0.0
# 描述: 使用 kubeadm 自动化部署高可用 Kubernetes 集群
#       包含 HAProxy+Keepalived VIP、Calico CNI、Rook-Ceph 存储
# 适用系统: Ubuntu 22.04 LTS / Rocky Linux 9
# 使用方式:
#   Master1(初始化): bash deploy_kubernetes.sh --init-master
#   Master2/3(加入): bash deploy_kubernetes.sh --join-master
#   Worker(加入):    bash deploy_kubernetes.sh --join-worker
#   完整(单节点):    bash deploy_kubernetes.sh --all
# =============================================================================

set -euo pipefail

# ========================== 颜色定义 ==========================
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'

# ========================== 配置变量 ==========================
# !! 部署前请根据实际环境修改以下配置 !!

# Kubernetes 版本
K8S_VERSION="1.29.0"

# VIP 地址（HAProxy 虚拟 IP）
VIP_IP="192.168.1.60"
VIP_PORT=6443

# 所有 Master 节点
MASTER_NODES=(
  "k8s-master1:192.168.1.61"
  "k8s-master2:192.168.1.62"
  "k8s-master3:192.168.1.63"
)

# Worker 节点（可按需扩展）
WORKER_NODES=(
  "k8s-worker1:192.168.1.71"
  "k8s-worker2:192.168.1.72"
  "k8s-worker3:192.168.1.73"
)

# 网络配置
SERVICE_CIDR="10.96.0.0/12"
POD_CIDR="10.244.0.0/16"
MGMT_INTERFACE="eth0"

# 镜像仓库（国内加速）
IMAGE_REPO="registry.aliyuncs.com/google_containers"

# CNI 配置
CALICO_VERSION="v3.27.0"

# Rook-Ceph 版本
ROOK_VERSION="v1.13.0"
ROOK_ENABLED=true

# Ingress-Nginx 版本
INGRESS_VERSION="controller-v1.9.5"
INGRESS_ENABLED=true

# 当前节点角色（由参数决定，不需手动修改）
NODE_ROLE=""

# join 命令保存路径
JOIN_CMD_FILE="/tmp/k8s_join_commands.txt"

# 日志文件
LOG_FILE="/var/log/deploy_kubernetes_$(date +%Y%m%d_%H%M%S).log"

# ========================== 工具函数 ==========================

log()   { echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]${NC} $*" | tee -a "$LOG_FILE"; }
warn()  { echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]${NC} $*" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR]${NC} $*" | tee -a "$LOG_FILE"; exit 1; }
info()  { echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] [STEP]${NC} $*" | tee -a "$LOG_FILE"; }

check_root() {
  [[ $EUID -ne 0 ]] && error "请使用 root 用户运行此脚本"
}

wait_pods_ready() {
  local namespace="${1:-kube-system}"
  local timeout=300
  local elapsed=0
  log "等待 $namespace 中所有 Pod 就绪..."
  while [[ $elapsed -lt $timeout ]]; do
    local not_ready
    not_ready=$(kubectl get pods -n "$namespace" --no-headers 2>/dev/null | \
      grep -v "Running\|Completed" | wc -l) || true
    if [[ "$not_ready" -eq 0 ]]; then
      log "命名空间 $namespace 所有 Pod 已就绪 ✓"
      return 0
    fi
    sleep 10
    elapsed=$((elapsed + 10))
    warn "等待中... ${elapsed}s/${timeout}s (未就绪: $not_ready)"
  done
  warn "等待超时，当前 Pod 状态:"
  kubectl get pods -n "$namespace"
}

# ========================== 阶段一: 系统准备（所有节点）==========================

phase_system_prepare() {
  info "========== 阶段一: 系统准备（所有节点通用）=========="

  log "关闭 Swap..."
  swapoff -a
  sed -i '/\bswap\b/d' /etc/fstab

  log "关闭防火墙..."
  systemctl stop firewalld 2>/dev/null || true
  systemctl disable firewalld 2>/dev/null || true
  # UFW
  ufw disable 2>/dev/null || true

  log "关闭 SELinux..."
  setenforce 0 2>/dev/null || true
  sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config 2>/dev/null || true

  log "设置时区..."
  timedatectl set-timezone Asia/Shanghai
  systemctl enable --now chronyd 2>/dev/null || systemctl enable --now ntpd 2>/dev/null || true

  log "加载内核模块..."
  cat > /etc/modules-load.d/k8s.conf << EOF
overlay
br_netfilter
EOF
  modprobe overlay
  modprobe br_netfilter

  log "配置内核参数..."
  cat > /etc/sysctl.d/k8s.conf << EOF
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
vm.swappiness                       = 0
net.ipv4.tcp_tw_reuse               = 1
net.ipv4.tcp_keepalive_time         = 600
EOF
  sysctl --system

  log "系统准备完成 ✓"
}

# ========================== 阶段二: 安装 containerd ==========================

phase_install_containerd() {
  info "========== 阶段二: 安装 containerd 运行时 =========="

  if command -v containerd &>/dev/null; then
    log "containerd 已安装，版本: $(containerd --version)"
  else
    log "安装 containerd..."
    if command -v apt &>/dev/null; then
      apt update -y
      apt install -y containerd
    elif command -v dnf &>/dev/null; then
      dnf config-manager --add-repo \
        https://download.docker.com/linux/centos/docker-ce.repo 2>/dev/null || true
      dnf install -y containerd.io
    fi
  fi

  log "配置 containerd..."
  mkdir -p /etc/containerd
  containerd config default > /etc/containerd/config.toml

  # 设置 SystemdCgroup = true
  sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

  # 设置 pause 镜像（使用阿里云镜像加速）
  sed -i "s|registry.k8s.io/pause:3.8|${IMAGE_REPO}/pause:3.9|g" \
    /etc/containerd/config.toml 2>/dev/null || true
  sed -i "s|registry.k8s.io/pause:3.9|${IMAGE_REPO}/pause:3.9|g" \
    /etc/containerd/config.toml 2>/dev/null || true

  systemctl enable --now containerd
  log "containerd 配置完成 ✓"
}

# ========================== 阶段三: 安装 kubeadm/kubelet/kubectl ==========================

phase_install_kube_tools() {
  info "========== 阶段三: 安装 kubeadm / kubelet / kubectl =========="

  local pkg_version="${K8S_VERSION}-00"

  if command -v kubeadm &>/dev/null; then
    log "kubeadm 已安装: $(kubeadm version -o short)"
    return 0
  fi

  if command -v apt &>/dev/null; then
    log "配置 Kubernetes apt 源（阿里云镜像）..."
    apt install -y apt-transport-https ca-certificates curl gpg

    mkdir -p /etc/apt/keyrings
    curl -fsSL https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg \
      | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg 2>/dev/null || \
      curl -fsSL https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg \
        > /etc/apt/trusted.gpg.d/kubernetes.gpg

    cat > /etc/apt/sources.list.d/kubernetes.list << EOF
deb https://mirrors.aliyun.com/kubernetes/apt/ kubernetes-xenial main
EOF
    apt update -y
    apt install -y \
      "kubelet=${pkg_version}" \
      "kubeadm=${pkg_version}" \
      "kubectl=${pkg_version}"
    apt-mark hold kubelet kubeadm kubectl

  elif command -v dnf &>/dev/null; then
    log "配置 Kubernetes yum 源（阿里云镜像）..."
    cat > /etc/yum.repos.d/kubernetes.repo << EOF
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
enabled=1
gpgcheck=0
repo_gpgcheck=0
EOF
    dnf install -y \
      "kubelet-${K8S_VERSION}" \
      "kubeadm-${K8S_VERSION}" \
      "kubectl-${K8S_VERSION}"
  fi

  systemctl enable kubelet
  log "kubeadm/kubelet/kubectl 安装完成 ✓"
}

# ========================== 阶段四: 配置 HAProxy + Keepalived ==========================

phase_setup_haproxy() {
  info "========== 阶段四: 配置 HAProxy + Keepalived (VIP: $VIP_IP) =========="

  if command -v apt &>/dev/null; then
    apt install -y haproxy keepalived
  elif command -v dnf &>/dev/null; then
    dnf install -y haproxy keepalived
  fi

  log "配置 HAProxy..."
  cat >> /etc/haproxy/haproxy.cfg << EOF

# ===== Kubernetes API Server =====
frontend k8s-api
    bind *:${VIP_PORT}
    mode tcp
    option tcplog
    default_backend k8s-masters

backend k8s-masters
    mode tcp
    balance roundrobin
    option tcp-check
EOF

  for master_info in "${MASTER_NODES[@]}"; do
    local name="${master_info%%:*}"
    local ip="${master_info##*:}"
    echo "    server $name ${ip}:6443 check fall 3 rise 2" >> /etc/haproxy/haproxy.cfg
  done

  log "配置 Keepalived（本节点 $HOSTNAME）..."
  # 判断当前节点是 Master1 还是其他
  local master1_ip="${MASTER_NODES[0]##*:}"
  local current_ip
  current_ip=$(ip route get 8.8.8.8 | awk '{print $7; exit}')

  if [[ "$current_ip" == "$master1_ip" ]]; then
    local state="MASTER"
    local priority=100
  else
    local state="BACKUP"
    local priority=90
  fi

  cat > /etc/keepalived/keepalived.conf << EOF
vrrp_instance VI_1 {
    state ${state}
    interface ${MGMT_INTERFACE}
    virtual_router_id 51
    priority ${priority}
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass k8svip2024
    }
    virtual_ipaddress {
        ${VIP_IP}/24
    }
    track_script {
        chk_apiserver
    }
}

vrrp_script chk_apiserver {
    script "/bin/bash -c 'curl -sk https://127.0.0.1:6443/healthz > /dev/null'"
    interval 3
    weight -2
    fall 10
    rise 2
}
EOF

  systemctl enable --now haproxy
  systemctl enable --now keepalived

  log "HAProxy + Keepalived 配置完成 ✓"
  log "VIP: $VIP_IP"
}

# ========================== 阶段五: 初始化第一个 Master ==========================

phase_init_first_master() {
  info "========== 阶段五: 初始化第一个 Master 节点 =========="

  if kubectl get nodes &>/dev/null; then
    warn "集群已初始化，跳过..."
    return 0
  fi

  log "拉取 Kubernetes 所需镜像..."
  kubeadm config images pull \
    --image-repository "$IMAGE_REPO" \
    --kubernetes-version "v${K8S_VERSION}" 2>&1 | tee -a "$LOG_FILE"

  log "生成 kubeadm 配置文件..."
  cat > /tmp/kubeadm-config.yaml << EOF
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: v${K8S_VERSION}
controlPlaneEndpoint: "${VIP_IP}:${VIP_PORT}"
imageRepository: ${IMAGE_REPO}
networking:
  serviceSubnet: "${SERVICE_CIDR}"
  podSubnet: "${POD_CIDR}"
etcd:
  local:
    dataDir: /var/lib/etcd
    extraArgs:
      auto-compaction-retention: "1h"
      quota-backend-bytes: "8589934592"
apiServer:
  extraArgs:
    event-ttl: "24h0m0s"
    max-requests-inflight: "2000"
    max-mutating-requests-inflight: "500"
---
apiVersion: kubeproxy.config.k8s.io/v1alpha1
kind: KubeProxyConfiguration
mode: ipvs
ipvs:
  minSyncPeriod: 1s
  syncPeriod: 30s
---
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
cgroupDriver: systemd
maxPods: 110
serializeImagePulls: false
EOF

  log "初始化集群..."
  kubeadm init --config /tmp/kubeadm-config.yaml \
    --upload-certs 2>&1 | tee "$JOIN_CMD_FILE" | tee -a "$LOG_FILE"

  log "配置 kubectl..."
  mkdir -p "$HOME/.kube"
  cp -i /etc/kubernetes/admin.conf "$HOME/.kube/config"
  chown "$(id -u):$(id -g)" "$HOME/.kube/config"

  log "提取 join 命令..."
  # 提取 worker join 命令
  grep -A 2 "kubeadm join" "$JOIN_CMD_FILE" | \
    grep -v "control-plane" | \
    head -3 > /tmp/worker-join.sh
  # 提取 control-plane join 命令
  grep -A 3 "control-plane" "$JOIN_CMD_FILE" | \
    head -5 > /tmp/master-join.sh

  log "=============================="
  log "Master 初始化完成 ✓"
  log "Join 命令已保存至: $JOIN_CMD_FILE"
  log "Worker join: /tmp/worker-join.sh"
  log "Master join: /tmp/master-join.sh"
  log "=============================="
}

# ========================== 阶段六: 安装 Calico CNI ==========================

phase_install_calico() {
  info "========== 阶段六: 安装 Calico CNI =========="

  if kubectl get pods -n calico-system 2>/dev/null | grep -q "Running"; then
    log "Calico 已部署，跳过..."
    return 0
  fi

  log "下载 Calico Operator..."
  kubectl create -f \
    "https://raw.githubusercontent.com/projectcalico/calico/${CALICO_VERSION}/manifests/tigera-operator.yaml" \
    2>&1 | tee -a "$LOG_FILE"

  log "生成 Calico 配置..."
  cat > /tmp/calico-config.yaml << EOF
apiVersion: operator.tigera.io/v1
kind: Installation
metadata:
  name: default
spec:
  calicoNetwork:
    ipPools:
    - blockSize: 26
      cidr: ${POD_CIDR}
      encapsulation: VXLANCrossSubnet
      natOutgoing: Enabled
      nodeSelector: all()
---
apiVersion: operator.tigera.io/v1
kind: APIServer
metadata:
  name: default
spec: {}
EOF

  kubectl create -f /tmp/calico-config.yaml 2>&1 | tee -a "$LOG_FILE"

  log "等待 Calico 就绪..."
  wait_pods_ready "calico-system"

  log "Calico CNI 安装完成 ✓"
}

# ========================== 阶段七: 安装 Rook-Ceph 存储 ==========================

phase_install_rook_ceph() {
  info "========== 阶段七: 安装 Rook-Ceph CSI 存储 =========="

  if [[ "$ROOK_ENABLED" != true ]]; then
    warn "Rook-Ceph 已禁用，跳过..."
    return 0
  fi

  if kubectl get namespace rook-ceph &>/dev/null; then
    log "Rook-Ceph 已安装，跳过..."
    return 0
  fi

  log "下载 Rook ${ROOK_VERSION} 配置..."
  local rook_dir="/tmp/rook-${ROOK_VERSION}"
  mkdir -p "$rook_dir"
  cd "$rook_dir"

  local base_url="https://raw.githubusercontent.com/rook/rook/${ROOK_VERSION}/deploy/examples"
  wget -q "${base_url}/crds.yaml" -O crds.yaml
  wget -q "${base_url}/common.yaml" -O common.yaml
  wget -q "${base_url}/operator.yaml" -O operator.yaml
  wget -q "${base_url}/cluster.yaml" -O cluster.yaml
  wget -q "${base_url}/csi/rbd/storageclass.yaml" -O storageclass-rbd.yaml
  wget -q "${base_url}/csi/cephfs/storageclass.yaml" -O storageclass-cephfs.yaml

  log "安装 Rook CRDs 和 Operator..."
  kubectl create -f crds.yaml
  kubectl create -f common.yaml
  kubectl create -f operator.yaml

  log "等待 Rook Operator 就绪..."
  wait_pods_ready "rook-ceph"

  log "部署 Ceph 集群..."
  kubectl create -f cluster.yaml

  log "等待 Ceph 集群就绪（可能需要 5-10 分钟）..."
  sleep 60
  wait_pods_ready "rook-ceph"

  log "创建 StorageClass..."
  kubectl create -f storageclass-rbd.yaml
  kubectl create -f storageclass-cephfs.yaml

  # 设置默认 StorageClass
  kubectl patch storageclass rook-ceph-block \
    -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

  log "Rook-Ceph 安装完成 ✓"
  kubectl get storageclass
}

# ========================== 阶段八: 安装 Ingress-Nginx ==========================

phase_install_ingress() {
  info "========== 阶段八: 安装 Ingress-Nginx =========="

  if [[ "$INGRESS_ENABLED" != true ]]; then
    warn "Ingress-Nginx 已禁用，跳过..."
    return 0
  fi

  if kubectl get pods -n ingress-nginx 2>/dev/null | grep -q "Running"; then
    log "Ingress-Nginx 已安装，跳过..."
    return 0
  fi

  log "安装 Ingress-Nginx..."
  kubectl apply -f \
    "https://raw.githubusercontent.com/kubernetes/ingress-nginx/${INGRESS_VERSION}/deploy/static/provider/baremetal/deploy.yaml" \
    2>&1 | tee -a "$LOG_FILE"

  wait_pods_ready "ingress-nginx"
  log "Ingress-Nginx 安装完成 ✓"
}

# ========================== 阶段九: 安装辅助组件 ==========================

phase_install_addons() {
  info "========== 阶段九: 安装辅助组件 =========="

  # Metrics Server
  log "安装 Metrics Server..."
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml \
    2>&1 | tee -a "$LOG_FILE" || warn "Metrics Server 安装失败，请手动安装"

  # 修复国内环境 Metrics Server TLS 问题
  kubectl patch deployment metrics-server \
    -n kube-system \
    --type='json' \
    -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]' \
    2>/dev/null || true

  log "辅助组件安装完成 ✓"
}

# ========================== 查看集群状态 ==========================

phase_status() {
  info "===== Kubernetes 集群状态 ====="
  kubectl get nodes -o wide
  echo ""
  kubectl get pods -A | head -50
  echo ""
  kubectl get storageclass
  echo ""
  kubectl top nodes 2>/dev/null || warn "Metrics Server 未就绪"
}

# ========================== 主流程 ==========================

usage() {
  echo -e "${CYAN}用法: $0 [选项]${NC}"
  echo ""
  echo "选项:"
  echo "  --all            单机/测试完整部署（推荐测试用）"
  echo "  --prepare        系统准备（所有节点）"
  echo "  --runtime        安装 containerd（所有节点）"
  echo "  --install-tools  安装 kubeadm/kubectl（所有节点）"
  echo "  --haproxy        配置 HAProxy+Keepalived（Master节点）"
  echo "  --init-master    初始化第一个 Master"
  echo "  --calico         安装 Calico CNI"
  echo "  --rook           安装 Rook-Ceph 存储"
  echo "  --ingress        安装 Ingress-Nginx"
  echo "  --addons         安装辅助组件"
  echo "  --status         查看集群状态"
  echo "  --help           显示帮助"
  echo ""
  echo "HA 部署顺序:"
  echo "  1. 所有节点: --prepare --runtime --install-tools"
  echo "  2. 所有Master节点: --haproxy"
  echo "  3. Master1: --init-master"
  echo "  4. Master2/3: 手动执行 join 命令（见 $JOIN_CMD_FILE）"
  echo "  5. Worker: 手动执行 join 命令"
  echo "  6. Master1: --calico --rook --ingress --addons"
}

main() {
  check_root
  mkdir -p "$(dirname $LOG_FILE)"
  log "日志文件: $LOG_FILE"

  local action="${1:---all}"

  case "$action" in
    --all)
      phase_system_prepare
      phase_install_containerd
      phase_install_kube_tools
      phase_setup_haproxy
      phase_init_first_master
      phase_install_calico
      phase_install_rook_ceph
      phase_install_ingress
      phase_install_addons
      log "=============================================="
      log "Kubernetes 集群部署完成! 🎉"
      log "节点状态: kubectl get nodes"
      log "kubeconfig: $HOME/.kube/config"
      log "Join 命令: $JOIN_CMD_FILE"
      log "=============================================="
      phase_status
      ;;
    --prepare)       phase_system_prepare ;;
    --runtime)       phase_install_containerd ;;
    --install-tools) phase_install_kube_tools ;;
    --haproxy)       phase_setup_haproxy ;;
    --init-master)
      phase_system_prepare
      phase_install_containerd
      phase_install_kube_tools
      phase_setup_haproxy
      phase_init_first_master
      ;;
    --calico)   phase_install_calico ;;
    --rook)     phase_install_rook_ceph ;;
    --ingress)  phase_install_ingress ;;
    --addons)   phase_install_addons ;;
    --status)   phase_status ;;
    --help)     usage ;;
    *)          error "未知选项: $action，使用 --help 查看帮助" ;;
  esac
}

main "$@"
