# Kubernetes (K8s) 单机安装指南

## 目录
- [前置要求](#前置要求)
- [Minikube 安装（推荐）](#minikube-安装推荐)
- [Kind 安装](#kind-安装)
- [K3s 安装](#k3s-安装)
- [验证安装](#验证安装)
- [常用命令](#常用命令)
- [常见问题](#常见问题)

---

## 前置要求

### 系统要求
- 操作系统：Linux / macOS / Windows (支持 WSL2)
- CPU：至少 2 核
- 内存：至少 2GB（推荐 4GB+）
- 硬盘：至少 20GB 可用空间
- 容器运行时：Docker、Podman 或 containerd

### 安装 Docker
如果你还没有安装 Docker，请先安装 Docker：

#### Ubuntu/Debian
```bash
# 更新包索引
sudo apt-get update

# 安装必要的依赖
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 设置 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER
```

#### CentOS/RHEL
```bash
# 安装必要的依赖
sudo yum install -y yum-utils

# 添加 Docker 仓库
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装 Docker
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER
```

#### Windows/Mac
下载并安装 Docker Desktop：
- https://www.docker.com/products/docker-desktop

---

## Minikube 安装（推荐）

Minikube 是最流行的本地 K8s 集群解决方案，适合开发和测试。

### 安装 Minikube

#### Linux
```bash
# 下载 Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

#### macOS
使用 Homebrew：
```bash
brew install minikube
```

#### Windows
使用 Chocolatey：
```powershell
choco install minikube
```

或下载二进制文件：
https://github.com/kubernetes/minikube/releases

### 启动集群
```bash
# 启动 Minikube（使用 Docker 驱动）
minikube start --driver=docker

# 指定资源（可选）
minikube start --driver=docker --cpus=4 --memory=8192

# 查看状态
minikube status
```

### Minikube 常用命令
```bash
# 停止集群
minikube stop

# 启动集群
minikube start

# 删除集群
minikube delete

# 查看集群信息
minikube cluster-info

# 打开 Dashboard
minikube dashboard

# 进入节点
minikube ssh
```

---

## Kind 安装

Kind (Kubernetes in Docker) 专门用于在 Docker 容器中运行 K8s 集群。

### 安装 Kind

#### Linux/macOS
```bash
# 下载 Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

#### macOS (Homebrew)
```bash
brew install kind
```

#### Windows
```powershell
# 使用 Chocolatey
choco install kind
```

### 创建集群
```bash
# 创建单节点集群
kind create cluster --name my-cluster

# 查看集群
kind get clusters

# 获取 kubeconfig
kind get kubeconfig --name my-cluster

# 删除集群
kind delete cluster --name my-cluster
```

---

## K3s 安装

K3s 是轻量级的 Kubernetes 发行版，非常适合资源受限的环境。

### 安装 K3s

```bash
# 使用安装脚本
curl -sfL https://get.k3s.io | sh -

# 启动 K3s
sudo systemctl start k3s
sudo systemctl enable k3s

# 查看 K3s 状态
sudo systemctl status k3s
```

### 配置 kubectl
```bash
# 复制 K3s kubeconfig
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config
```

### 卸载 K3s
```bash
/usr/local/bin/k3s-uninstall.sh
```

---

## 验证安装

### 检查集群状态
```bash
# 查看节点
kubectl get nodes

# 查看命名空间
kubectl get namespaces

# 查看所有 Pod
kubectl get pods --all-namespaces

# 查看集群信息
kubectl cluster-info

# 查看版本
kubectl version --client && kubectl version
```

### 部署测试应用
```bash
# 部署 Nginx
kubectl create deployment nginx --image=nginx

# 暴露服务
kubectl expose deployment nginx --port=80 --type=NodePort

# 查看 Pod
kubectl get pods

# 查看 Service
kubectl get svc

# 端口转发测试
kubectl port-forward svc/nginx 8080:80

# 访问测试（另一个终端）
curl http://localhost:8080
```

---

## 常用命令

### Pod 管理
```bash
# 查看所有 Pod
kubectl get pods

# 查看 Pod 详细信息
kubectl describe pod <pod-name>

# 查看 Pod 日志
kubectl logs <pod-name>

# 进入 Pod 容器
kubectl exec -it <pod-name> -- /bin/bash

# 删除 Pod
kubectl delete pod <pod-name>
```

### Deployment 管理
```bash
# 创建 Deployment
kubectl create deployment <name> --image=<image>

# 查看 Deployment
kubectl get deployments

# 扩缩容
kubectl scale deployment <name> --replicas=3

# 更新镜像
kubectl set image deployment/<name> <container-name>=<new-image>

# 回滚
kubectl rollout undo deployment/<name>
```

### Service 管理
```bash
# 查看 Service
kubectl get svc

# 创建 Service
kubectl expose deployment <name> --port=80 --type=LoadBalancer

# 删除 Service
kubectl delete svc <service-name>
```

### 资源管理
```bash
# 查看所有资源
kubectl get all

# 查看特定命名空间的资源
kubectl get all -n <namespace>

# 删除所有 Pod
kubectl delete pods --all

# 查看资源使用情况
kubectl top nodes
kubectl top pods
```

---

## 常见问题

### 1. 权限问题
如果执行 `kubectl` 命令时报权限错误：
```bash
sudo chown -R $USER:$USER ~/.kube
```

### 2. 镜像拉取失败
如果 Pod 无法拉取镜像，可以配置镜像加速器（以 Docker 为例）：

编辑 `/etc/docker/daemon.json`：
```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}
```

重启 Docker：
```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 3. 端口被占用
如果端口冲突：
```bash
# 查看端口占用
sudo netstat -tunlp | grep <port>

# 杀掉占用进程
sudo kill -9 <pid>
```

### 4. 内存不足
如果集群因内存不足启动失败：
```bash
# Minikube 增加内存
minikube start --memory=8192

# 或者使用 swap
sudo swapon /swapfile
```

### 5. WSL2 问题（Windows 用户）
在 WSL2 中遇到网络问题：
```bash
# 重启 WSL
wsl --shutdown

# 在 PowerShell 中重启
wsl
```

### 6. 查看 Pod 状态
Pod 一直处于 Pending 或 CrashLoopBackOff 状态：
```bash
# 查看详细事件
kubectl describe pod <pod-name>

# 查看日志
kubectl logs <pod-name> --previous
```

---

## 参考资料

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Minikube 官方文档](https://minikube.sigs.k8s.io/docs/)
- [Kind 官方文档](https://kind.sigs.k8s.io/docs/user/quick-start/)
- [K3s 官方文档](https://docs.k3s.io/)

---

## 下一步

安装完成后，你可以：
1. 学习 Kubernetes 核心概念
2. 部署你的第一个应用
3. 使用 Helm 管理应用
4. 配置 Ingress 进行流量管理
5. 设置监控和日志收集

祝你使用愉快！
