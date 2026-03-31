# Kubernetes 安装文档

## 一、概述

本文档基于 **Kubernetes 1.29+** 版本，使用 **kubeadm** 方式部署高可用集群，包含：
- 3 个 Control Plane 节点（高可用）
- N 个 Worker 节点
- **Calico** CNI 网络插件
- **Rook-Ceph** 持久化存储

---

## 二、环境规划

### 2.1 节点规划

| 主机名 | IP 地址 | 角色 |
|--------|---------|------|
| k8s-master1 | 192.168.1.61 | Control Plane |
| k8s-master2 | 192.168.1.62 | Control Plane |
| k8s-master3 | 192.168.1.63 | Control Plane |
| k8s-worker1 | 192.168.1.71 | Worker Node |
| k8s-worker2 | 192.168.1.72 | Worker Node |
| k8s-worker3 | 192.168.1.73 | Worker Node |
| k8s-vip     | 192.168.1.60 | VIP（HAProxy + Keepalived） |

### 2.2 硬件要求

| 节点类型 | 最低配置 | 推荐配置 |
|---------|---------|---------|
| Control Plane | 2C / 4GB RAM / 50GB SSD | 4C / 8GB RAM / 100GB SSD |
| Worker Node | 4C / 8GB RAM / 100GB SSD | 8C / 32GB RAM / 200GB SSD |

### 2.3 网络规划

| 网段 | 用途 |
|------|------|
| 192.168.1.0/24 | 节点主机网络 |
| 10.96.0.0/12 | Service CIDR（ClusterIP） |
| 10.244.0.0/16 | Pod CIDR（Calico） |

---

## 三、前置准备（所有节点执行）

```bash
# 1. 关闭 Swap
swapoff -a
sed -i '/swap/d' /etc/fstab

# 2. 关闭防火墙
systemctl stop firewalld && systemctl disable firewalld

# 3. 关闭 SELinux
setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config

# 4. 时间同步
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd

# 5. 设置主机名（各节点分别执行）
hostnamectl set-hostname k8s-master1

# 6. 配置 /etc/hosts
cat >> /etc/hosts << EOF
192.168.1.60  k8s-vip
192.168.1.61  k8s-master1
192.168.1.62  k8s-master2
192.168.1.63  k8s-master3
192.168.1.71  k8s-worker1
192.168.1.72  k8s-worker2
192.168.1.73  k8s-worker3
EOF

# 7. 加载内核模块
cat > /etc/modules-load.d/k8s.conf << EOF
overlay
br_netfilter
EOF
modprobe overlay
modprobe br_netfilter

# 8. 配置内核参数
cat > /etc/sysctl.d/k8s.conf << EOF
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sysctl --system
```

---

## 四、安装容器运行时（containerd）

```bash
# 安装 containerd
apt update && apt install -y containerd

# 生成默认配置
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml

# 修改 cgroup driver 为 systemd
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

# 修改 pause 镜像（国内加速）
sed -i 's|registry.k8s.io/pause:3.8|registry.aliyuncs.com/google_containers/pause:3.9|g' /etc/containerd/config.toml

# 启动 containerd
systemctl enable --now containerd
```

---

## 五、安装 kubeadm / kubelet / kubectl

```bash
# 添加 Kubernetes 源（国内镜像）
cat > /etc/apt/sources.list.d/kubernetes.list << EOF
deb https://mirrors.aliyun.com/kubernetes/apt/ kubernetes-xenial main
EOF

curl -fsSL https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg | apt-key add -

apt update
apt install -y kubelet=1.29.0-00 kubeadm=1.29.0-00 kubectl=1.29.0-00
apt-mark hold kubelet kubeadm kubectl

systemctl enable kubelet
```

---

## 六、配置 HAProxy + Keepalived（Control Plane VIP）

### 6.1 安装（所有 Master 节点执行）

```bash
apt install -y haproxy keepalived
```

### 6.2 配置 HAProxy（所有 Master 节点）

编辑 `/etc/haproxy/haproxy.cfg`，追加：

```
frontend k8s-api
    bind *:6443
    mode tcp
    option tcplog
    default_backend k8s-masters

backend k8s-masters
    mode tcp
    balance roundrobin
    server master1 192.168.1.61:6443 check
    server master2 192.168.1.62:6443 check
    server master3 192.168.1.63:6443 check
```

### 6.3 配置 Keepalived（Master1 — MASTER）

编辑 `/etc/keepalived/keepalived.conf`：

```
vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass k8svip
    }
    virtual_ipaddress {
        192.168.1.60/24
    }
}
```

> Master2/Master3 将 `state` 改为 `BACKUP`，`priority` 改为 `90`/`80`。

```bash
systemctl enable --now haproxy keepalived
```

---

## 七、初始化 Control Plane（在 k8s-master1 执行）

### 7.1 创建 kubeadm 配置文件

```yaml
# kubeadm-config.yaml
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: v1.29.0
controlPlaneEndpoint: "192.168.1.60:6443"
imageRepository: registry.aliyuncs.com/google_containers
networking:
  serviceSubnet: "10.96.0.0/12"
  podSubnet: "10.244.0.0/16"
etcd:
  local:
    dataDir: /var/lib/etcd
---
apiVersion: kubeproxy.config.k8s.io/v1alpha1
kind: KubeProxyConfiguration
mode: ipvs
```

### 7.2 拉取镜像

```bash
kubeadm config images pull \
  --image-repository registry.aliyuncs.com/google_containers \
  --kubernetes-version v1.29.0
```

### 7.3 初始化集群

```bash
kubeadm init --config kubeadm-config.yaml --upload-certs | tee /root/kubeadm-init.log
```

### 7.4 配置 kubectl

```bash
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config
```

---

## 八、加入其他 Control Plane 节点

从 `kubeadm-init.log` 中找到 `--control-plane` 命令，在 master2/master3 执行：

```bash
kubeadm join 192.168.1.60:6443 \
  --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash> \
  --control-plane \
  --certificate-key <cert-key>
```

在 master2/master3 执行：
```bash
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
```

---

## 九、加入 Worker 节点

```bash
# 从 kubeadm-init.log 找到 worker join 命令
kubeadm join 192.168.1.60:6443 \
  --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash>
```

---

## 十、安装 Calico CNI

```bash
# 下载 Calico 配置
curl -O https://raw.githubusercontent.com/projectcalico/calico/v3.27.0/manifests/tigera-operator.yaml
curl -O https://raw.githubusercontent.com/projectcalico/calico/v3.27.0/manifests/custom-resources.yaml

# 修改 Pod CIDR
sed -i 's|192.168.0.0/16|10.244.0.0/16|g' custom-resources.yaml

# 安装
kubectl create -f tigera-operator.yaml
kubectl create -f custom-resources.yaml

# 等待就绪
kubectl get pods -n calico-system -w
```

---

## 十一、安装 Rook-Ceph CSI 存储

```bash
# 克隆 Rook 仓库
git clone --single-branch --branch v1.13.0 https://github.com/rook/rook.git
cd rook/deploy/examples

# 安装 CRD 和 Operator
kubectl create -f crds.yaml
kubectl create -f common.yaml
kubectl create -f operator.yaml

# 部署 Ceph 集群（连接已有 Ceph 集群）
kubectl create -f cluster.yaml

# 创建 StorageClass
kubectl create -f csi/rbd/storageclass.yaml
kubectl create -f csi/cephfs/storageclass.yaml

# 查看状态
kubectl get pods -n rook-ceph -w
```

---

## 十二、验证集群

```bash
# 查看节点状态
kubectl get nodes -o wide

# 查看所有 Pod
kubectl get pods -A

# 查看 StorageClass
kubectl get storageclass

# 创建测试 PVC
cat << EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: test-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  storageClassName: rook-ceph-block
EOF

kubectl get pvc test-pvc
```

---

## 十三、安装常用组件

### 13.1 安装 Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl top nodes
```

### 13.2 安装 Ingress-Nginx

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.5/deploy/static/provider/baremetal/deploy.yaml
kubectl get pods -n ingress-nginx
```

### 13.3 安装 cert-manager

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
kubectl get pods -n cert-manager
```

---

## 十四、常见问题排查

| 问题 | 排查方法 |
|------|---------|
| 节点 NotReady | `kubectl describe node <name>`，检查 CNI 是否就绪 |
| Pod Pending | `kubectl describe pod <name>`，查看 Event 中的调度原因 |
| PVC Pending | 检查 `storageclass` 和 CSI driver 是否正常 |
| etcd 故障 | `etcdctl endpoint health`，检查各 etcd 成员状态 |
| API Server 不可达 | 检查 VIP / HAProxy 状态 |

---

## 十五、参考资料

- [Kubernetes 官方文档](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/)
- [Calico 安装文档](https://docs.tigera.io/calico/latest/getting-started/kubernetes/quickstart)
- [Rook-Ceph 文档](https://rook.io/docs/rook/latest-release/Getting-Started/quickstart/)
