# OpenStack 安装文档

## 一、概述

OpenStack 是一套开源云计算管理平台，本文档基于 **Yoga / Zed / 2023.x** 版本，使用 **Kolla-Ansible** 进行容器化部署。

核心组件：
- **Keystone** — 认证与授权
- **Glance** — 镜像服务
- **Nova** — 计算服务
- **Neutron** — 网络服务
- **Cinder** — 块存储服务
- **Horizon** — Web 控制台
- **Placement** — 资源调度

---

## 二、环境规划

### 2.1 节点规划

| 主机名 | IP 地址 | 角色 |
|--------|---------|------|
| controller01 | 192.168.1.21 | Controller（Keystone/Glance/Nova-API/Neutron-Server/Horizon） |
| controller02 | 192.168.1.22 | Controller（高可用备节点） |
| controller03 | 192.168.1.23 | Controller（高可用备节点） |
| compute01 | 192.168.1.31 | Compute（Nova-Compute/Neutron-Agent） |
| compute02 | 192.168.1.32 | Compute |
| network01 | 192.168.1.41 | Network（Neutron L3-Agent/DHCP） |
| storage01 | 192.168.1.51 | Storage（Cinder-Volume，对接 Ceph） |
| deploy | 192.168.1.10 | 部署节点（Kolla-Ansible） |

### 2.2 网络规划

| 网络 | 网段 | 用途 |
|------|------|------|
| 管理网 | 192.168.1.0/24 | 节点管理、API 通信 |
| 存储网 | 192.168.2.0/24 | 存储流量（Ceph） |
| 隧道网 | 192.168.3.0/24 | VXLAN 隧道（Neutron） |
| 外部网 | 10.0.0.0/24 | 浮动 IP、外部访问 |

### 2.3 硬件要求

| 节点 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| Controller | ≥ 8C | ≥ 16GB | ≥ 100GB SSD |
| Compute | ≥ 16C | ≥ 64GB | ≥ 200GB SSD |
| Network | ≥ 4C | ≥ 8GB | ≥ 50GB SSD |
| Storage | ≥ 4C | ≥ 8GB | 系统盘 + 数据盘 |

---

## 三、前置准备

### 3.1 所有节点基础配置

```bash
# 关闭防火墙
systemctl stop firewalld && systemctl disable firewalld

# 关闭 SELinux
setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config

# 时间同步
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd

# 配置 /etc/hosts（所有节点）
cat >> /etc/hosts << EOF
192.168.1.10  deploy
192.168.1.21  controller01
192.168.1.22  controller02
192.168.1.23  controller03
192.168.1.31  compute01
192.168.1.32  compute02
192.168.1.41  network01
192.168.1.51  storage01
EOF
```

### 3.2 部署节点配置

```bash
# 安装 Python 和 pip
apt update && apt install -y python3 python3-pip python3-venv git

# 创建虚拟环境
python3 -m venv /opt/kolla-venv
source /opt/kolla-venv/bin/activate

# 安装 Kolla-Ansible
pip install kolla-ansible

# 创建配置目录
mkdir -p /etc/kolla
cp -r /opt/kolla-venv/share/kolla-ansible/etc_examples/kolla/* /etc/kolla/
cp /opt/kolla-venv/share/kolla-ansible/ansible/inventory/* /etc/kolla/
```

### 3.3 配置 SSH 免密（在 deploy 节点）

```bash
ssh-keygen -t rsa -b 4096 -N "" -f ~/.ssh/id_rsa
for node in controller01 controller02 controller03 compute01 compute02 network01 storage01; do
  ssh-copy-id root@$node
done
```

---

## 四、配置 Kolla-Ansible

### 4.1 配置 Inventory 文件

编辑 `/etc/kolla/multinode`：

```ini
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
```

### 4.2 配置 globals.yml

编辑 `/etc/kolla/globals.yml`：

```yaml
# 基础配置
kolla_base_distro: "ubuntu"
kolla_install_type: "source"
openstack_release: "2023.2"

# 网络配置
network_interface: "eth0"          # 管理网接口
neutron_external_interface: "eth3" # 外部网接口
kolla_internal_vip_address: "192.168.1.100"
kolla_external_vip_address: "10.0.0.100"

# 启用 HA
enable_haproxy: "yes"
enable_keepalived: "yes"

# 存储后端（Ceph）
glance_backend_ceph: "yes"
cinder_backend_ceph: "yes"
nova_backend_ceph: "yes"
ceph_glance_pool_name: "images"
ceph_cinder_pool_name: "volumes"
ceph_nova_pool_name: "vms"

# 启用的服务
enable_openstack_core: "yes"
enable_glance: "yes"
enable_nova: "yes"
enable_neutron: "yes"
enable_cinder: "yes"
enable_horizon: "yes"
enable_heat: "yes"
enable_placement: "yes"
enable_barbican: "yes"
enable_designate: "yes"
enable_octavia: "yes"

# Neutron 配置
neutron_plugin_agent: "ovn"
enable_neutron_provider_networks: "yes"

# 密码
keystone_admin_password: "<your-admin-password>"
```

### 4.3 配置 Ceph 密钥（对接 Ceph 集群）

```bash
# 从 Ceph 集群获取密钥
ssh ceph-node1 "ceph auth get client.glance" > /etc/kolla/ceph.client.glance.keyring
ssh ceph-node1 "ceph auth get client.cinder" > /etc/kolla/ceph.client.cinder.keyring
ssh ceph-node1 "ceph auth get client.nova" > /etc/kolla/ceph.client.nova.keyring

# 获取 Ceph 配置文件
ssh ceph-node1 "cat /etc/ceph/ceph.conf" > /etc/kolla/ceph.conf
```

---

## 五、执行部署

### 5.1 生成密码

```bash
kolla-genpwd
```

### 5.2 预检查

```bash
kolla-ansible -i /etc/kolla/multinode bootstrap-servers
kolla-ansible -i /etc/kolla/multinode prechecks
```

### 5.3 拉取镜像

```bash
kolla-ansible -i /etc/kolla/multinode pull
```

### 5.4 正式部署

```bash
kolla-ansible -i /etc/kolla/multinode deploy
```

> 部署时间约 30-60 分钟，取决于网络和硬件性能。

### 5.5 初始化 OpenStack 环境

```bash
kolla-ansible -i /etc/kolla/multinode post-deploy
source /etc/kolla/admin-openrc.sh
```

---

## 六、验证安装

### 6.1 验证核心服务

```bash
# 查看所有服务状态
openstack service list

# 查看计算节点
openstack compute service list

# 查看网络代理
openstack network agent list

# 查看存储服务
openstack volume service list
```

### 6.2 创建测试资源

```bash
# 上传镜像
wget http://download.cirros-cloud.net/0.6.2/cirros-0.6.2-x86_64-disk.img
openstack image create "cirros" \
  --file cirros-0.6.2-x86_64-disk.img \
  --disk-format qcow2 --container-format bare --public

# 创建 Flavor
openstack flavor create --ram 512 --disk 1 --vcpus 1 m1.tiny

# 创建网络
openstack network create --provider-network-type vxlan internal-net
openstack subnet create --network internal-net \
  --subnet-range 10.10.0.0/24 --dns-nameserver 8.8.8.8 internal-subnet

# 创建安全组规则
openstack security group rule create --protocol icmp default
openstack security group rule create --protocol tcp --dst-port 22 default

# 创建虚拟机
openstack server create \
  --image cirros \
  --flavor m1.tiny \
  --network internal-net \
  --security-group default \
  test-vm

# 查看虚拟机状态
openstack server list
```

---

## 七、Neutron OVN 网络配置

### 7.1 创建外部网络

```bash
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
```

### 7.2 创建路由器并关联浮动 IP

```bash
openstack router create main-router
openstack router set main-router --external-gateway external-net
openstack router add subnet main-router internal-subnet

# 申请浮动 IP
openstack floating ip create external-net

# 绑定到虚拟机
openstack server add floating ip test-vm <floating-ip>
```

---

## 八、访问 Horizon 控制台

- **URL**：`http://192.168.1.100/`
- **用户名**：`admin`
- **密码**：查看 `/etc/kolla/passwords.yml` 中的 `keystone_admin_password`

---

## 九、常见问题排查

| 问题 | 排查命令 |
|------|---------|
| 服务启动失败 | `docker logs <container-name>` |
| 虚拟机 Build 失败 | `openstack server show <id>`，查看 `nova-compute` 日志 |
| 网络不通 | `neutron-debug probe-create`，检查 OVN 日志 |
| Ceph 连接失败 | 检查 `ceph.conf` 和 keyring 文件权限 |
| API 504 超时 | 检查 HAProxy 和各服务健康状态 |

---

## 十、参考资料

- [Kolla-Ansible 官方文档](https://docs.openstack.org/kolla-ansible/latest/)
- [OpenStack 官方文档](https://docs.openstack.org/)
- [Neutron OVN 配置](https://docs.openstack.org/neutron/latest/admin/ovn/)
