# Kubernetes (K8s) 集群安装指南

## 目录
- [前置要求](#前置要求)
- [环境准备](#环境准备)
- [kubeadm 安装](#kubeadm-安装)
- [Master 节点初始化](#master-节点初始化)
- [Worker 节点加入](#worker-节点加入)
- [网络插件安装](#网络插件安装)
- [集群验证](#集群验证)
- [高可用集群](#高可用集群)
- [常见问题](#常见问题)

---

## 前置要求

### 硬件要求
- **Master 节点**：2 核 CPU，2GB 内存（推荐 4GB+）
- **Worker 节点**：2 核 CPU，2GB 内存（推荐 4GB+）
- **硬盘**：每个节点至少 20GB 可用空间

### 软件要求
- 操作系统：Ubuntu 20.04+ / CentOS 7+ / Rocky Linux 8+
- 容器运行时：Docker、containerd 或 CRI-O
- 网络：节点之间网络互通，禁用 Swap

---

## 环境准备

以下操作需要在**所有节点**上执行。

### 1. 配置主机名和 hosts
```bash
# 设置主机名（在每个节点上执行不同的主机名）
sudo hostnamectl set-hostname k8s-master  # Master 节点
sudo hostnamectl set-hostname k8s-worker1  # Worker 节点1
sudo hostnamectl set-hostname k8s-worker2  # Worker 节点2

# 编辑 /etc/hosts 文件
sudo vi /etc/hosts
```

添加以下内容（替换为实际 IP）：
```
192.168.1.100 k8s-master
192.168.1.101 k8s-worker1
192.168.1.102 k8s-worker2
```

### 2. 关闭 Swap
```bash
# 临时关闭
sudo swapoff -a

# 永久关闭（注释 /etc/fstab 中的 swap 行）
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab
```

### 3. 禁用 SELinux（CentOS/RHEL）
```bash
sudo setenforce 0
sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
```

### 4. 配置内核参数
```bash
# 创建配置文件
sudo cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

# 加载模块
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
```

### 5. 安装容器运行时（以 containerd 为例）

#### Ubuntu/Debian
```bash
# 安装依赖
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release

# 添加 Docker 仓库
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 containerd
sudo apt-get update
sudo apt-get install -y containerd.io

# 配置 containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml

# 修改配置使用 SystemdCgroup
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml

# 启动 containerd
sudo systemctl restart containerd
sudo systemctl enable containerd
```

#### CentOS/RHEL
```bash
# 安装依赖
sudo yum install -y yum-utils

# 添加 Docker 仓库
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装 containerd
sudo yum install -y containerd.io

# 配置 containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml

# 修改配置使用 SystemdCgroup
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml

# 启动 containerd
sudo systemctl restart containerd
sudo systemctl enable containerd
```

---

## kubeadm 安装

在**所有节点**上安装 kubeadm、kubelet 和 kubectl。

### Ubuntu/Debian
```bash
# 添加 Kubernetes 仓库
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

# 下载签名密钥
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

# 添加仓库
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list

# 安装 kubeadm、kubelet、kubectl
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl

# 锁定版本
sudo apt-mark hold kubelet kubeadm kubectl
```

### CentOS/RHEL
```bash
# 添加 Kubernetes 仓库
cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://pkgs.k8s.io/core:/stable:/v1.29/rpm/
enabled=1
gpgcheck=1
gpgkey=https://pkgs.k8s.io/core:/stable:/v1.29/rpm/repodata/repomd.xml.key
exclude=kubelet kubeadm kubectl
EOF

# 安装 kubeadm、kubelet、kubectl
sudo yum install -y kubelet kubeadm kubectl --disableexcludes=kubernetes

# 启动 kubelet
sudo systemctl enable --now kubelet
```

---

## Master 节点初始化

**仅在 Master 节点**上执行以下命令。

### 1. 初始化集群
```bash
# 使用 kubeadm 初始化（替换为实际 API Server IP）
sudo kubeadm init \
  --apiserver-advertise-address=192.168.1.100 \
  --pod-network-cidr=10.244.0.0/16 \
  --service-cidr=10.96.0.0/12 \
  --ignore-preflight-errors=all

# 或使用配置文件初始化
sudo kubeadm init --config=kubeadm-config.yaml
```

`kubeadm-config.yaml` 示例：
```yaml
apiVersion: kubeadm.k8s.io/v1beta3
kind: InitConfiguration
localAPIEndpoint:
  advertiseAddress: 192.168.1.100
  bindPort: 6443
---
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: "1.29.0"
networking:
  podSubnet: "10.244.0.0/16"
  serviceSubnet: "10.96.0.0/12"
---
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
cgroupDriver: systemd
```

### 2. 配置 kubectl
```bash
# 创建目录
mkdir -p $HOME/.kube

# 复制配置文件
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config

# 修改权限
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

### 3. 保存 join 命令
初始化成功后会输出类似以下的命令，**保存好该命令**：
```bash
kubeadm join 192.168.1.100:6443 --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash>
```

---

## Worker 节点加入

**在每个 Worker 节点**上执行以下命令。

### 1. 加入集群
```bash
# 使用之前保存的 join 命令
kubeadm join 192.168.1.100:6443 --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash>
```

### 2. 如果 token 过期，重新生成
在 Master 节点上：
```bash
# 创建新 token
kubeadm token create --print-join-command

# 查看 CA 证书哈希
openssl x509 -pubkey -in /etc/kubernetes/pki/ca.crt | openssl rsa -pubin -outform der 2>/dev/null | \
  openssl dgst -sha256 -hex | sed 's/^.* //'
```

---

## 网络插件安装

在 Master 节点上安装网络插件。这里以 Calico 为例。

### 安装 Calico
```bash
# 下载 Calico 清单
curl https://raw.githubusercontent.com/projectcalico/calico/v3.26.1/manifests/calico.yaml -O

# 修改配置（可选，根据 kubeadm init 时的 pod-network-cidr）
# sed -i 's/192.168.0.0\/16/10.244.0.0\/16/g' calico.yaml

# 应用配置
kubectl apply -f calico.yaml

# 查看 Pod 状态
kubectl get pods -n kube-system
```

### 其他网络插件选择

#### Flannel
```bash
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
```

#### Cilium
```bash
curl -L https://github.com/cilium/cilium-cli/releases/latest/download/cilium-linux-amd64 -o cilium
sudo mv cilium /usr/local/bin/
cilium install
```

---

## 集群验证

在 Master 节点上验证集群状态。

### 1. 查看节点状态
```bash
kubectl get nodes

# 期望输出（STATUS 应该是 Ready）
# NAME         STATUS   ROLES           AGE   VERSION
# k8s-master   Ready    control-plane   10m   v1.29.0
# k8s-worker1  Ready    <none>          5m    v1.29.0
# k8s-worker2  Ready    <none>          5m    v1.29.0
```

### 2. 查看所有 Pod
```bash
kubectl get pods --all-namespaces

# 查看 Pod 详细状态
kubectl get pods -n kube-system -o wide
```

### 3. 查看集群信息
```bash
kubectl cluster-info
```

### 4. 部署测试应用
```bash
# 创建 Deployment
kubectl create deployment nginx --image=nginx

# 暴露服务
kubectl expose deployment nginx --port=80 --type=NodePort

# 查看 Service
kubectl get svc

# 测试访问
curl http://<任意节点IP>:<NodePort>
```

---

## 高可用集群

### 1. 负载均衡配置

安装 HAProxy（在负载均衡器上）：
```bash
sudo apt-get install -y haproxy
```

配置 `/etc/haproxy/haproxy.cfg`：
```
frontend k8s-apiserver
    bind *:6443
    mode tcp
    option tcplog
    default_backend k8s-apiserver

backend k8s-apiserver
    mode tcp
    option tcplog
    option tcp-check
    balance roundrobin
    server master1 192.168.1.100:6443 check
    server master2 192.168.1.101:6443 check
    server master3 192.168.1.102:6443 check
```

重启 HAProxy：
```bash
sudo systemctl restart haproxy
```

### 2. 初始化第一个 Master 节点
```bash
kubeadm init \
  --control-plane-endpoint "load-balancer:6443" \
  --upload-certs \
  --pod-network-cidr=10.244.0.0/16
```

### 3. 加入其他 Master 节点
```bash
kubeadm join load-balancer:6443 \
  --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash> \
  --control-plane \
  --certificate-key <cert-key>
```

---

## 常见问题

### 1. 节点 NotReady
```bash
# 查看节点详情
kubectl describe node <node-name>

# 查看 kubelet 日志
sudo journalctl -u kubelet -f

# 检查网络插件
kubectl get pods -n kube-system | grep -E "calico|flannel|cilium"
```

### 2. Container runtime not ready
```bash
# 检查 containerd 状态
sudo systemctl status containerd

# 查看 CRI 配置
sudo crictl info
```

### 3. Image pull 错误
```bash
# 配置镜像加速
sudo vi /etc/containerd/config.toml
```

添加：
```toml
[plugins."io.containerd.grpc.v1.cri".registry]
  [plugins."io.containerd.grpc.v1.cri".registry.mirrors]
    [plugins."io.containerd.grpc.v1.cri".registry.mirrors."docker.io"]
      endpoint = ["https://docker.mirrors.ustc.edu.cn"]
```

重启 containerd：
```bash
sudo systemctl restart containerd
```

### 4. Swap 未关闭
```bash
# 检查 swap
free -h

# 永久关闭
sudo swapoff -a
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab
```

### 5. 端口冲突
```bash
# 检查端口占用
sudo netstat -tunlp | grep 6443

# 查看 kubeconfig
kubectl config view
```

### 6. 重置集群
```bash
# 在所有节点上执行
sudo kubeadm reset
sudo rm -rf /etc/cni/net.d
sudo iptables -F && sudo iptables -t nat -F && sudo iptables -t mangle -F && sudo iptables -X
```

---

## 集群管理

### 查看集群状态
```bash
kubectl get nodes -o wide
kubectl get pods --all-namespaces
kubectl get services --all-namespaces
```

### 查看日志
```bash
# 查看 kubelet 日志
sudo journalctl -u kubelet -f

# 查看容器日志
kubectl logs <pod-name>
```

### 集群升级
```bash
# 查看可升级版本
kubeadm upgrade plan

# 升级控制平面
sudo kubeadm upgrade apply v1.29.1

# 升级 kubelet
sudo apt-get install -y kubelet kubeadm kubectl

# 重启 kubelet
sudo systemctl daemon-reload
sudo systemctl restart kubelet
```

---

## 参考资料

- [Kubernetes 官方文档](https://kubernetes.io/docs/home/)
- [kubeadm 安装指南](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/)
- [Calico 网络插件](https://docs.projectcalico.org/)
- [Containerd 文档](https://containerd.io/docs/)

---

## 下一步

集群安装完成后，你可以：
1. 部署应用服务
2. 配置存储卷
3. 设置 Ingress
4. 配置监控（Prometheus + Grafana）
5. 配置日志收集（ELK 或 Loki）
6. 学习 Helm 包管理

祝你部署成功！
