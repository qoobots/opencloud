# Ceph 安装文档

## 一、概述

Ceph 是一个统一的分布式存储系统，提供对象存储（RGW）、块存储（RBD）和文件系统（CephFS）三种接口。

## 二、环境要求

### 2.1 硬件要求

| 节点类型 | 最低配置 | 推荐配置 |
|---------|---------|---------|
| MON 节点 | 2C / 4GB RAM / 50GB SSD | 4C / 8GB RAM / 100GB SSD |
| OSD 节点 | 4C / 8GB RAM / 数据盘 ≥ 1 块 | 8C / 16GB RAM / NVMe SSD |
| MDS 节点 | 4C / 8GB RAM / 50GB SSD | 8C / 16GB RAM / SSD |
| RGW 节点 | 4C / 8GB RAM / 50GB SSD | 8C / 16GB RAM / SSD |

### 2.2 操作系统要求

- OS：Ubuntu 22.04 LTS / CentOS Stream 9 / Rocky Linux 9
- 内核版本：≥ 5.10
- Python 版本：≥ 3.6

### 2.3 网络要求

- 公共网络（Public Network）：客户端访问集群
- 集群网络（Cluster Network）：OSD 之间数据复制（建议万兆）
- 所有节点间网络互通，延迟 < 1ms（推荐）

### 2.4 节点规划示例

| 主机名 | IP 地址 | 角色 |
|--------|---------|------|
| ceph-node1 | 192.168.1.11 | MON、MGR、OSD |
| ceph-node2 | 192.168.1.12 | MON、MGR、OSD |
| ceph-node3 | 192.168.1.13 | MON、OSD |
| ceph-node4 | 192.168.1.14 | OSD、MDS |
| ceph-node5 | 192.168.1.15 | OSD、MDS、RGW |

---

## 三、前置准备

### 3.1 所有节点执行

```bash
# 关闭防火墙
systemctl stop firewalld && systemctl disable firewalld

# 关闭 SELinux
setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config

# 时间同步
timedatectl set-timezone Asia/Shanghai
systemctl enable --now chronyd

# 设置主机名（各节点分别执行）
hostnamectl set-hostname ceph-node1

# 配置 /etc/hosts
cat >> /etc/hosts << EOF
192.168.1.11 ceph-node1
192.168.1.12 ceph-node2
192.168.1.13 ceph-node3
192.168.1.14 ceph-node4
192.168.1.15 ceph-node5
EOF
```

### 3.2 配置 SSH 免密登录（在 ceph-node1 执行）

```bash
ssh-keygen -t rsa -b 4096 -N "" -f ~/.ssh/id_rsa
for node in ceph-node1 ceph-node2 ceph-node3 ceph-node4 ceph-node5; do
  ssh-copy-id root@$node
done
```

---

## 四、安装 Ceph（使用 cephadm）

### 4.1 安装 cephadm

```bash
# Ubuntu 22.04
apt update && apt install -y cephadm

# Rocky Linux / CentOS Stream 9
dnf install -y cephadm
```

### 4.2 引导初始化集群（在 ceph-node1 执行）

```bash
cephadm bootstrap \
  --mon-ip 192.168.1.11 \
  --cluster-network 192.168.2.0/24 \
  --initial-dashboard-user admin \
  --initial-dashboard-password <your-password> \
  --skip-monitoring-stack
```

> 成功后会输出 Ceph Dashboard 访问地址和初始凭据。

### 4.3 安装 ceph CLI 工具

```bash
cephadm install ceph-common
# 或
cephadm shell -- ceph -s
```

### 4.4 添加其他节点

```bash
# 将公钥拷贝至其他节点
ssh-copy-id -f -i /etc/ceph/ceph.pub root@ceph-node2
ssh-copy-id -f -i /etc/ceph/ceph.pub root@ceph-node3
ssh-copy-id -f -i /etc/ceph/ceph.pub root@ceph-node4
ssh-copy-id -f -i /etc/ceph/ceph.pub root@ceph-node5

# 添加节点到集群
ceph orch host add ceph-node2 192.168.1.12
ceph orch host add ceph-node3 192.168.1.13
ceph orch host add ceph-node4 192.168.1.14
ceph orch host add ceph-node5 192.168.1.15
```

---

## 五、部署 MON 节点

```bash
# 指定 MON 节点（3 个节点满足高可用）
ceph orch apply mon "ceph-node1,ceph-node2,ceph-node3"

# 查看 MON 状态
ceph mon stat
```

---

## 六、部署 OSD 节点

### 6.1 查看可用磁盘

```bash
ceph orch device ls
```

### 6.2 添加 OSD（自动发现模式）

```bash
# 自动发现所有可用裸盘并添加为 OSD
ceph orch apply osd --all-available-devices
```

### 6.3 手动指定磁盘添加 OSD

```bash
# 在指定节点的指定磁盘上创建 OSD
ceph orch daemon add osd ceph-node2:/dev/sdb
ceph orch daemon add osd ceph-node3:/dev/sdb
```

### 6.4 查看 OSD 状态

```bash
ceph osd tree
ceph osd stat
```

---

## 七、部署 MGR 节点

```bash
ceph orch apply mgr "ceph-node1,ceph-node2"
ceph mgr stat
```

---

## 八、部署 MDS 节点（CephFS）

```bash
# 创建 CephFS 文件系统
ceph fs volume create cephfs

# 指定 MDS 节点
ceph orch apply mds cephfs "ceph-node4,ceph-node5"

# 查看 MDS 状态
ceph fs status
```

---

## 九、部署 RGW 网关（对象存储）

```bash
# 创建 RGW realm/zone
radosgw-admin realm create --rgw-realm=opencloud --default
radosgw-admin zonegroup create --rgw-zonegroup=default --master --default
radosgw-admin zone create --rgw-zonegroup=default --rgw-zone=cn-east-1 --master --default

# 部署 RGW 服务
ceph orch apply rgw opencloud cn-east-1 --placement="2 ceph-node4 ceph-node5" --port=7480

# 查看 RGW 状态
ceph orch ls rgw
```

---

## 十、创建存储池

```bash
# 创建 RBD 存储池（块存储，供 OpenStack Cinder/Glance 使用）
ceph osd pool create rbd 128
ceph osd pool application enable rbd rbd
rbd pool init rbd

# 创建 volumes 池（Cinder）
ceph osd pool create volumes 128
ceph osd pool application enable volumes rbd

# 创建 images 池（Glance）
ceph osd pool create images 128
ceph osd pool application enable images rbd

# 创建 vms 池（Nova）
ceph osd pool create vms 128
ceph osd pool application enable vms rbd
```

---

## 十一、验证集群状态

```bash
# 集群整体健康
ceph -s
ceph health detail

# 查看存储容量
ceph df

# 查看 OSD 性能
ceph osd perf

# 执行 IO 测试
rados bench -p rbd 10 write --no-cleanup
rados bench -p rbd 10 seq
rados bench -p rbd 10 rand
```

---

## 十二、常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| `HEALTH_WARN clock skew` | 节点时间不同步 | 检查并同步 NTP |
| OSD Down | 磁盘故障或网络异常 | `ceph osd tree` 查看，`journalctl -u ceph-osd@X` 看日志 |
| PG stuck | PG 长期处于非 active+clean | `ceph pg repair <pgid>` 或检查 OSD 状态 |
| 容量不均衡 | CRUSH map 权重不当 | `ceph osd reweight-by-utilization` |

---

## 十三、参考资料

- [Ceph 官方文档](https://docs.ceph.com/en/latest/)
- [cephadm 部署指南](https://docs.ceph.com/en/latest/cephadm/)
