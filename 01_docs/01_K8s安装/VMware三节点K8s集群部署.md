# VMware 三节点 K8s 集群部署指南

## 目录
- [VMware 虚拟机准备](#vmware-虚拟机准备)
- [虚拟机网络配置](#虚拟机网络配置)
- [虚拟机模板创建](#虚拟机模板创建)
- [集群环境准备](#集群环境准备)
- [K8s 安装部署](#k8s-安装部署)
- [集群验证](#集群验证)
- [常见问题](#常见问题)

---

## VMware 虚拟机准备

### 1. 虚拟机规划

| 角色 | 主机名 | IP 地址 | 内存 | CPU | 硬盘 | 操作系统 |
|------|--------|---------|------|-----|------|----------|
| Master | k8s-master | 192.168.100.10 | 4GB | 2核 | 40GB | Ubuntu 22.04 LTS |
| Worker1 | k8s-worker1 | 192.168.100.11 | 4GB | 2核 | 40GB | Ubuntu 22.04 LTS |
| Worker2 | k8s-worker2 | 192.168.100.12 | 4GB | 2核 | 40GB | Ubuntu 22.04 LTS |

### 2. VMware 虚拟机设置

#### 基础配置
```
操作系统：Linux -> Ubuntu 22.04 LTS (64-bit)
处理器：2 个虚拟处理器
内存：4GB（主机内存允许的情况下可增加）
硬盘：40GB（动态分配）
网络适配器：NAT 或桥接模式
```

#### 推荐虚拟机配置
```xml
<!-- VMware .vmx 配置示例 -->
memsize = "4096"
numvcpus = "2"
scsi0.virtualDev = "lsilogic"
ethernet0.connectionType = "nat"
ethernet0.present = "TRUE"
```

---

## 虚拟机网络配置

### 方案一：NAT 网络模式（推荐用于测试）

#### 1. 配置 VMware NAT 网络

在 VMware 中：
- 编辑 -> 虚拟网络编辑器
- 选择 VMnet8 (NAT 模式)
- 子网：192.168.100.0
- 子网掩码：255.255.255.0
- NAT 设置：网关 192.168.100.2

#### 2. 配置端口转发（可选）

在 VMware NAT 设置中添加端口转发：
```
宿主机端口 6443 -> 虚拟机 192.168.100.10:6443 (API Server)
宿主机端口 30000-32767 -> 虚拟机 192.168.100.10:30000-32767 (NodePort)
```

#### 3. 虚拟机网络配置

在**所有虚拟机**中配置静态 IP：

```bash
# 编辑网络配置
sudo vi /etc/netplan/00-installer-config.yaml
```

配置内容：
```yaml
network:
  ethernets:
    ens33:
      dhcp4: no
      addresses:
        - 192.168.100.10/24  # 每个节点使用不同 IP
      routes:
        - to: default
          via: 192.168.100.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
  version: 2
```

应用配置：
```bash
sudo netplan apply
```

### 方案二：桥接网络模式（推荐用于生产）

#### 1. 配置 VMware 桥接网络

在 VMware 中：
- 编辑 -> 虚拟网络编辑器
- 选择 VMnet0 (桥接模式)
- 桥接到：你的物理网卡

#### 2. 虚拟机获取局域网 IP

确保虚拟机从路由器获取 IP，或配置静态 IP（局域网网段）。

---

## 虚拟机模板创建

### 步骤 1：创建第一台虚拟机（模板）

1. 在 VMware 中新建虚拟机
2. 安装 Ubuntu 22.04 LTS
3. 完成基础系统配置
4. 安装必要的工具：
```bash
sudo apt-get update
sudo apt-get install -y openssh-server vim wget curl net-tools
```

### 步骤 2：克隆虚拟机

1. 关闭模板虚拟机
2. 右键 -> 管理 -> 克隆
3. 克隆类型：创建完整克隆
4. 克隆两次，得到三台虚拟机

### 步骤 3：配置克隆的虚拟机

为每台虚拟机配置不同的主机名和 IP：

```bash
# Master 节点
sudo hostnamectl set-hostname k8s-master
sudo vi /etc/netplan/00-installer-config.yaml  # IP: 192.168.100.10

# Worker1 节点
sudo hostnamectl set-hostname k8s-worker1
sudo vi /etc/netplan/00-installer-config.yaml  # IP: 192.168.100.11

# Worker2 节点
sudo hostnamectl set-hostname k8s-worker2
sudo vi /etc/netplan/00-installer-config.yaml  # IP: 192.168.100.12
```

### 步骤 4：配置 hosts 文件

在**所有节点**上执行：
```bash
sudo vi /etc/hosts
```

添加以下内容：
```
192.168.100.10 k8s-master
192.168.100.11 k8s-worker1
192.168.100.12 k8s-worker2
```

### 步骤 5：配置 SSH 免密登录（可选）

```bash
# 在 Master 节点生成密钥
ssh-keygen -t rsa -b 4096

# 复制公钥到其他节点
ssh-copy-id root@k8s-worker1
ssh-copy-id root@k8s-worker2
```

---

## 集群环境准备

以下操作在**所有节点**上执行。

### 1. 关闭 Swap

```bash
# 临时关闭
sudo swapoff -a

# 永久关闭
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

# 验证
free -h
```

### 2. 配置内核参数

```bash
# 加载必要的内核模块
sudo cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

# 配置内核参数
sudo cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

# 应用参数
sudo sysctl --system

# 验证
lsmod | grep br_netfilter
sysctl net.bridge.bridge-nf-call-iptables
```

### 3. 配置时间同步

```bash
# 安装 chrony
sudo apt-get install -y chrony

# 启动 chrony
sudo systemctl start chrony
sudo systemctl enable chrony

# 验证
chronyc tracking
```

### 4. 安装 containerd

```bash
# 更新包索引
sudo apt-get update

# 安装依赖
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 设置 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 containerd
sudo apt-get update
sudo apt-get install -y containerd.io

# 生成默认配置
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml

# 修改配置使用 SystemdCgroup
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml

# 配置镜像加速（可选，国内用户推荐）
sudo sed -i 's|https://registry-1.docker.io|https://docker.mirrors.ustc.edu.cn|g' /etc/containerd/config.toml

# 重启 containerd
sudo systemctl restart containerd
sudo systemctl enable containerd

# 验证
sudo systemctl status containerd
sudo crictl info
```

### 5. 安装 kubeadm、kubelet、kubectl

```bash
# 添加 Kubernetes 仓库
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list

# 更新包索引
sudo apt-get update

# 安装 Kubernetes 组件
sudo apt-get install -y kubelet kubeadm kubectl

# 锁定版本防止自动升级
sudo apt-mark hold kubelet kubeadm kubectl

# 启动 kubelet
sudo systemctl enable kubelet

# 验证版本
kubectl version --client
kubeadm version
```

---

## K8s 安装部署

### 步骤 1：初始化 Master 节点

**仅在 Master 节点（192.168.100.10）上执行**：

```bash
# 创建 kubeadm 配置文件
sudo vi kubeadm-config.yaml
```

配置内容：
```yaml
apiVersion: kubeadm.k8s.io/v1beta3
kind: InitConfiguration
localAPIEndpoint:
  advertiseAddress: 192.168.100.10
  bindPort: 6443
nodeRegistration:
  criSocket: unix:///run/containerd/containerd.sock
  name: k8s-master
---
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: "1.29.0"
controlPlaneEndpoint: "192.168.100.10:6443"
networking:
  podSubnet: "10.244.0.0/16"
  serviceSubnet: "10.96.0.0/12"
imageRepository: registry.aliyuncs.com/google_containers
---
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
cgroupDriver: systemd
```

执行初始化：
```bash
# 拉取镜像
sudo kubeadm config images pull --config=kubeadm-config.yaml

# 初始化集群
sudo kubeadm init --config=kubeadm-config.yaml --upload-certs

# 记录输出的 join 命令，稍后在 Worker 节点使用
```

**如果初始化成功，会看到以下输出**：
```
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join 192.168.100.10:6443 --token abcdef.0123456789abcdef \
  --discovery-token-ca-cert-hash sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 步骤 2：配置 kubectl

**在 Master 节点**执行：
```bash
# 创建配置目录
mkdir -p $HOME/.kube

# 复制配置文件
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config

# 修改权限
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# 验证配置
kubectl get nodes
```

此时应该看到：
```
NAME         STATUS     ROLES           AGE   VERSION
k8s-master   NotReady   control-plane   10s   v1.29.0
```

### 步骤 3：安装网络插件（Calico）

**在 Master 节点**执行：
```bash
# 下载 Calico 清单
curl https://raw.githubusercontent.com/projectcalico/calico/v3.26.1/manifests/calico.yaml -O

# 应用配置
kubectl apply -f calico.yaml

# 等待 Pod 启动
kubectl get pods -n kube-system -w
```

等待 2-3 分钟，所有 Pod 应该变为 `Running` 状态。

### 步骤 4：加入 Worker 节点

**在每个 Worker 节点**上执行：

使用之前保存的 join 命令：
```bash
sudo kubeadm join 192.168.100.10:6443 --token abcdef.0123456789abcdef \
  --discovery-token-ca-cert-hash sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**如果 token 过期**，在 Master 节点重新生成：
```bash
# 生成新 token
kubeadm token create --print-join-command

# 输出类似：
# kubeadm join 192.168.100.10:6443 --token xxxxx.yyyyyyyyyyyyyyyy \
#   --discovery-token-ca-cert-hash sha256:zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
```

在 Master 节点查看节点加入情况：
```bash
kubectl get nodes
kubectl get pods --all-namespaces
```

---

## 集群验证

### 1. 验证节点状态

```bash
kubectl get nodes -o wide
```

期望输出（所有节点状态为 `Ready`）：
```
NAME          STATUS   ROLES           AGE   VERSION   INTERNAL-IP       EXTERNAL-IP
k8s-master    Ready    control-plane   10m   v1.29.0   192.168.100.10    <none>
k8s-worker1   Ready    <none>          5m    v1.29.0   192.168.100.11    <none>
k8s-worker2   Ready    <none>          5m    v1.29.0   192.168.100.12    <none>
```

### 2. 验证系统 Pod

```bash
kubectl get pods -n kube-system
```

期望输出（所有 Pod 状态为 `Running`）：
```
NAME                                       READY   STATUS    RESTARTS   AGE
calico-kube-controllers-xxx                1/1     Running   0          10m
calico-node-xxx                            1/1     Running   0          10m
coredns-xxx                                1/1     Running   0          10m
etcd-k8s-master                            1/1     Running   0          10m
kube-apiserver-k8s-master                  1/1     Running   0          10m
kube-controller-manager-k8s-master         1/1     Running   0          10m
kube-proxy-xxx                             1/1     Running   0          10m
kube-scheduler-k8s-master                  1/1     Running   0          10m
```

### 3. 部署测试应用

```bash
# 部署 Nginx
kubectl create deployment nginx --image=nginx:latest --replicas=3

# 查看部署状态
kubectl get deployments
kubectl get pods -o wide

# 暴露服务
kubectl expose deployment nginx --port=80 --type=NodePort

# 查看服务
kubectl get svc
```

测试访问：
```bash
# 获取 NodePort
kubectl get svc nginx

# 在任意节点上访问
curl http://192.168.100.10:<NodePort>
curl http://192.168.100.11:<NodePort>
curl http://192.168.100.12:<NodePort>
```

### 4. 验证 Pod 通信

```bash
# 进入一个 Pod
kubectl exec -it <pod-name> -- /bin/bash

# 测试 DNS 解析
nslookup kubernetes.default
ping k8s-worker1

# 测试服务访问
curl http://nginx.default.svc.cluster.local
```

### 5. 查看集群信息

```bash
# 集群信息
kubectl cluster-info

# 节点详细信息
kubectl describe nodes

# 查看资源使用
kubectl top nodes
```

---

## 常见问题

### 1. 虚拟机无法联网

**症状**：无法拉取镜像，apt 失败

**解决方案**：
```bash
# 检查网络适配器模式（建议使用 NAT）
# 检查 VMware NAT 服务是否启动
# 配置静态 IP 后检查网关是否正确

# 测试网络
ping 8.8.8.8
ping baidu.com

# 重新配置网络
sudo netplan apply
```

### 2. 节点状态 NotReady

**症状**：`kubectl get nodes` 显示 `NotReady`

**排查步骤**：
```bash
# 查看节点详情
kubectl describe node k8s-worker1

# 查看 kubelet 日志
sudo journalctl -u kubelet -f

# 检查网络插件
kubectl get pods -n kube-system | grep calico

# 重启 kubelet
sudo systemctl restart kubelet
```

### 3. 镜像拉取失败

**症状**：Pod 状态 `ImagePullBackOff`

**解决方案**：
```bash
# 配置镜像加速
sudo vi /etc/containerd/config.toml

# 修改 registry 为国内镜像
# 重启 containerd
sudo systemctl restart containerd

# 手动拉取镜像测试
sudo crictl pull nginx:alpine
```

### 4. CPU 或内存不足

**症状**：虚拟机卡顿，Pod 启动慢

**解决方案**：
```bash
# 增加虚拟机资源配置
# 关闭虚拟机 -> 设置 -> 处理器/内存 -> 调整大小

# 查看资源使用
kubectl top nodes
kubectl top pods -A

# 限制 Pod 资源使用
kubectl set resources deployment nginx --limits=cpu=500m,memory=512Mi
```

### 5. 重置集群

如果需要重新部署：
```bash
# 在所有节点执行
sudo kubeadm reset -f

# 清理 iptables
sudo iptables -F && sudo iptables -t nat -F && sudo iptables -t mangle -F && sudo iptables -X

# 清理 CNI
sudo rm -rf /etc/cni/net.d
sudo rm -rf ~/.kube/

# 重新初始化
```

### 6. 虚拟机快照管理

**建议**：在每个关键步骤创建快照
- 安装完系统后
- 配置好网络后
- 安装完 K8s 组件后
- 集群部署成功后

创建快照命令（VMware）：
```
虚拟机 -> 快照 -> 拍摄快照
```

### 7. VMware 性能优化

```xml
<!-- .vmx 文件中添加以下配置 -->

# 启用 3D 加速
mks.enable3d = "TRUE"

# 增加 SCSI 性能
scsi0.virtualDev = "pvscsi"

# 内存预留
mem.hotadd = "TRUE"
vcpu.hotadd = "TRUE"

# 增大预读
mainMem.useNamedFile = "FALSE"
```

### 8. 时区问题

```bash
# 检查时区
timedatectl

# 设置时区为上海
sudo timedatectl set-timezone Asia/Shanghai

# 验证
date
```

---

## 集群管理

### 启动/停止集群

```bash
# 在 Master 节点停止集群
sudo kubeadm reset

# 恢复（如果有快照）
# VMware -> 快照 -> 恢复到快照
```

### 升级集群

```bash
# 查看可升级版本
kubeadm upgrade plan

# 升级控制平面
sudo kubeadm upgrade apply v1.29.1

# 升级 Worker 节点
sudo apt-get install -y kubelet kubeadm kubectl
sudo systemctl restart kubelet
```

### 备份集群配置

```bash
# 备份 etcd
ETCDCTL_API=3 etcdctl snapshot save snapshot.db \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key

# 备份配置文件
sudo tar -czf k8s-backup-$(date +%Y%m%d).tar.gz /etc/kubernetes/
```

---

## 下一步

集群部署成功后，你可以：
1. 安装 Helm 包管理器
2. 部署 Dashboard（kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml）
3. 安装监控（Prometheus + Grafana）
4. 部署 Ingress 控制器（Nginx Ingress）
5. 学习存储卷配置（PV/PVC/StorageClass）

---

## 参考资料

- [VMware 虚拟机网络配置](https://docs.vmware.com/en/VMware-Workstation-Pro/)
- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Calico 安装指南](https://docs.projectcalico.org/)
- [containerd 配置文档](https://containerd.io/docs/)

祝你部署成功！🎉
