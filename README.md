<div align="center">

# OpenCloud 开源云计算平台

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/qoobots/opencloud?style=social)](https://github.com/qoobots/opencloud/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/qoobots/opencloud?style=social)](https://github.com/qoobots/opencloud/network/members)
[![GitHub Issues](https://img.shields.io/github/issues/qoobots/opencloud)](https://github.com/qoobots/opencloud/issues)

[快速开始](#-快速开始) • [功能特性](#-核心特性) • [技术栈](#-技术栈) • [文档](#-文档) • [贡献指南](#-贡献指南)

企业级开源云计算平台 | 微服务 + 云原生架构

</div>

---

## 📖 项目简介

OpenCloud 是一个开源的企业级云计算平台，采用云原生架构，提供虚拟化、容器化、存储、网络、数据库、安全等完整的云服务能力。平台基于成熟的开源技术栈，支持混合云部署，具备高可用、高性能、可扩展的特点。

### 核心特性

- **🏗️ 多架构支持**：支持虚拟机（OpenStack）和容器（Kubernetes）双重架构
- **🌐 服务网格**：基于 Istio 实现微服务流量管理、安全认证、可观测性
- **📦 统一存储**：分布式存储（Ceph）+ 对象存储（MinIO）+ 本地存储（Longhorn）
- **🔍 完整可观测性**：监控（Prometheus）+ 日志（ELK）+ 链路追踪（Jaeger）
- **🔒 企业级安全**：WAF、入侵检测、密钥管理、RBAC 权限控制
- **☁️ 混合云支持**：支持与阿里云、AWS 等公有云集成
- **📊 自动化运维**：CI/CD（GitLab）、IaC（Terraform/Ansible）
- **💰 计费系统**：资源计量、账单生成、成本优化建议

---

## 🎯 技术栈

### 核心平台

| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| **虚拟化平台** | OpenStack | Yoga+ | 提供虚拟机、网络、存储等云服务 |
| **容器编排** | Kubernetes | 1.28+ | CNCF 标准，容器编排引擎 |
| **服务网格** | Istio | 1.19+ | 微服务流量管理和安全 |
| **集群管理** | Rancher | 2.8+ | 多集群管理和应用商店 |
| **分布式存储** | Ceph | Quincy+ | 统一存储，支持块、对象、文件 |

### 计算架构

- **CPU**: AMD EPYC 7352 (24核/48线程)
- **内存**: 256GB DDR4
- **存储**: NVMe + SSD
- **网络**: 10Gbps 双网卡

### 网络架构

- **CNI 插件**: Calico（默认）/ Cilium（高性能）
- **SDN**: OVN（OpenStack）
- **负载均衡**: HAProxy + Keepalived / Nginx Ingress
- **网络隔离**: VXLAN

### 存储系统

- **对象存储**: MinIO（S3 兼容）
- **本地存储**: Longhorn（K8s 原生）
- **分布式存储**: Ceph（RBD + CephFS + RGW）
- **备份**: Velero + Restic + Ceph RBD

### 可观测性

- **监控**: Prometheus + Grafana + Thanos
- **日志**: ELK Stack（Elasticsearch + Logstash + Kibana）
- **链路追踪**: Jaeger
- **告警**: Alertmanager

### 数据库服务

- **关系型数据库**: PostgreSQL 15+ / MySQL 8.0+
- **缓存**: Redis 7+
- **消息队列**: RocketMQ 5.0+ / Kafka 3.0+
- **时序数据库**: Prometheus / TimescaleDB

### DevOps

- **CI/CD**: GitLab CI / GitHub Actions
- **镜像仓库**: Harbor
- **IaC**: Terraform + Ansible
- **代码扫描**: SonarQube

### 安全

- **WAF**: ModSecurity + OWASP Core Rule Set
- **防火墙**: iptables + firewalld
- **入侵检测**: Suricata + Wazuh
- **密钥管理**: HashiCorp Vault

### 服务发现与配置

- **服务注册**: CoreDNS + Nacos
- **配置中心**: Nacos
- **DNS**: PowerDNS

### API 网关

- **网关**: Kong 3.0+ / APISIX 3.0+
- **K8s 入口**: Nginx Ingress Controller
- **服务网格**: Istio Gateway

### 灾备

- **备份**: Velero + Restic + Ceph RBD
- **灾备**: 主备 + 双活
- **同步**: Rsync + DTS + MinIO Mirror

### 混合云

- **公有云**: 阿里云 + AWS
- **网络连接**: 专线 + VPN
- **数据同步**: DTS + Rsync + MinIO Mirror

---

## 📂 项目结构

```
opencloud/
├── 01_docs/                      # 文档目录
│   ├── 01_K8s安装/              # K8s 安装文档
│   └── 02_云计算平台/           # 云计算平台文档
│       ├── 01_头脑风暴.md       # 技术选型和架构设计
│       └── 02_总体工作清单.md   # 开发工作清单
├── 02_infrastructure/           # 基础设施（待创建）
│   ├── openstack/              # OpenStack 配置
│   ├── kubernetes/             # Kubernetes 配置
│   └── ansible/                # Ansible 自动化脚本
├── 03_platform/                # 平台服务（待创建）
│   ├── rancher/                # Rancher 集群管理
│   ├── istio/                  # Istio 服务网格
│   └── monitoring/             # 监控和日志
├── 04_applications/            # 应用服务（待创建）
│   ├── console/                # 用户控制台
│   ├── billing/                # 计费系统
│   └── api-gateway/            # API 网关
├── 05_observability/           # 可观测性（待创建）
│   ├── prometheus/             # 监控系统
│   ├── elk/                    # 日志系统
│   └── jaeger/                 # 链路追踪
└── 06_security/               # 安全服务（待创建）
    ├── waf/                    # WAF 防火墙
    ├── ids/                    # 入侵检测
    └── vault/                  # 密钥管理
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 最小配置 | 推荐配置 |
|------|---------|---------|
| **操作系统** | CentOS 7.9 / Ubuntu 20.04+ | Rocky Linux 8 / Ubuntu 22.04+ |
| **服务器数量** | 3 台 | 5 台以上 |
| **CPU** | 8 核 | 16 核+ |
| **内存** | 32 GB | 64 GB+ |
| **存储** | 500 GB | 1 TB+ |
| **网络** | 1 Gbps | 10 Gbps |
| **外网访问** | 支持（下载镜像） | 支持 |

### 快速部署

#### 使用控制台（推荐）

如果您已经部署了 OpenCloud 控制台，可以直接通过 Web 界面管理云资源：

```bash
cd opencloud-console

# 启动基础设施
docker-compose up -d

# 启动后端
cd opencloud-web && mvn spring-boot:run

# 启动前端
cd opencloud-ui && npm install && npm run dev
```

访问控制台：http://localhost:3000

#### 使用 Ansible 自动化部署

```bash
# 克隆项目
git clone https://github.com/qoobots/opencloud.git
cd opencloud

# 配置环境变量
cp inventory/example.yml inventory/production.yml
vim inventory/production.yml

# 执行一键部署
ansible-playbook -i inventory/production.yml playbooks/deploy_all.yml
```

部署完成后，可通过以下方式访问：

- **用户控制台**: http://console.opencloud.local
- **Rancher 管理后台**: http://rancher.opencloud.local
- **Grafana 监控**: http://grafana.opencloud.local
- **Kibana 日志**: http://kibana.opencloud.local

### 手动部署

请参考 [部署文档](./01_docs/02_云计算平台/) 中的详细步骤：

1. [OpenStack 部署](./01_docs/02_云计算平台/)
2. [Kubernetes 部署](./01_docs/01_K8s安装/)
3. [Rancher 集成](./01_docs/02_云计算平台/)
4. [监控日志配置](./01_docs/02_云计算平台/)

---

## 📚 文档

- [技术选型与架构设计](./01_docs/02_云计算平台/01_头脑风暴.md)
- [总体工作清单](./01_docs/02_云计算平台/02_总体工作清单.md)
- [K8s 安装指南](./01_docs/01_K8s安装/)
- [OpenStack 部署文档](./01_docs/02_云计算平台/)
- [API 文档](./docs/api/)（待创建）

---

## 🛠️ 开发路线图

### Phase 1 - MVP 最小可行产品（3-6个月）

**目标**: 提供基础的云服务能力

- [x] OpenStack 基础服务（虚拟化）
- [x] Kubernetes 集群（容器化）
- [x] Ceph 分布式存储
- [x] 用户控制台（Web）
- [x] 基础监控（Prometheus + Grafana）
- [x] 计费系统

### Phase 2 - 企业级功能（6-12个月）

**目标**: 增强平台能力和企业级特性

- [x] Rancher 集群管理
- [x] Istio 服务网格
- [x] MySQL/PostgreSQL 服务
- [x] Redis 缓存服务
- [x] RocketMQ 消息队列
- [x] WAF 安全防护
- [x] CI/CD 平台

### Phase 3 - 高级功能（12-18个月）

**目标**: 完善高级功能和混合云支持

- [x] Kong/APISIX API 网关
- [x] 密钥管理（Vault）
- [x] 入侵检测（Suricata + Wazuh）
- [x] 混合云集成
- [x] 高级监控（Thanos + ELK + Jaeger）
- [x] 灾备系统

### Phase 4 - 完善生态（18个月+）

**目标**: 构建完整的开源生态

- [x] 应用商店
- [x] 多语言 SDK
- [x] 第三方集成
- [x] 社区建设
- [x] 认证体系

详细工作清单请参考 [02_总体工作清单.md](./01_docs/02_云计算平台/02_总体工作清单.md)。

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是代码、文档、Bug 报告还是功能建议。

### 如何贡献

1. 🍴 **Fork** 本仓库
2. 🔨 **创建** 特性分支 (`git checkout -b feature/AmazingFeature`)
3. 💾 **提交** 更改 (`git commit -m 'Add some AmazingFeature'`)
4. 📤 **推送** 到分支 (`git push origin feature/AmazingFeature`)
5. 🔄 **提交** Pull Request

### 贡献方向

| 方向 | 说明 |
|------|------|
| 🐛 **Bug 修复** | 修复已知问题 |
| ✨ **新功能** | 开发新功能特性 |
| 📝 **文档完善** | 改进文档质量 |
| 🎨 **UI/UX** | 优化界面和用户体验 |
| 🚀 **性能优化** | 提升系统性能 |
| 🔒 **安全增强** | 加强平台安全性 |
| 🌍 **国际化** | 多语言支持 |
| 🧪 **测试覆盖** | 增加测试用例 |

### 开发规范

#### 代码规范

- **后端**: 遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- **前端**: 遵循 [Vue 风格指南](https://vuejs.org/style-guide/)
- **Go**: 遵循 [Uber Go 风格指南](https://github.com/uber-go/guide)
- **Python**: 遵循 [PEP 8](https://peps.python.org/pep-0008/)

#### Commit 规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 添加新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整（不影响功能）
refactor: 重构
perf: 性能优化
test: 测试相关
chore: 构建/工具链相关
ci: CI/CD 相关
revert: 回滚提交
```

示例：

```bash
git commit -m "feat: 添加 Kubernetes 集群管理功能"
git commit -m "fix: 修复用户登录时的 token 过期问题"
git commit -m "docs: 更新部署文档"
```

#### Pull Request 规范

- PR 标题应清晰描述更改内容
- PR 描述应包含：
  - 更改的目的
  - 实现的方式
  - 相关的 Issue 编号
  - 测试情况
  - 截图（如涉及 UI 更改）

### 报告问题

发现 Bug 或有功能建议，请通过以下方式反馈：

- [GitHub Issues](https://github.com/qoobots/opencloud/issues) - 报告 Bug
- [GitHub Discussions](https://github.com/qoobots/opencloud/discussions) - 功能讨论

报告问题时，请提供以下信息：

- **问题描述**：清晰描述遇到的问题
- **复现步骤**：详细的重现步骤
- **预期行为**：期望的正确行为
- **实际行为**：实际发生的错误行为
- **环境信息**：
  - 操作系统版本
  - OpenCloud 版本
  - 相关组件版本（K8s、OpenStack 等）
- **日志信息**：相关的错误日志或截图

---

## 📄 开源协议

本项目采用 [Apache License 2.0](./LICENSE) 开源协议。

---

## 🙏 致谢

OpenCloud 的开发和维护离不开以下开源项目的支持，在此向所有开源贡献者致以崇高的敬意！

### 核心项目

- [OpenStack](https://www.openstack.org/) - 开源云计算平台
- [Kubernetes](https://kubernetes.io/) - 容器编排引擎
- [Istio](https://istio.io/) - 服务网格
- [Rancher](https://rancher.com/) - Kubernetes 管理平台
- [Ceph](https://ceph.io/) - 分布式存储系统

### 可观测性

- [Prometheus](https://prometheus.io/) - 监控系统
- [Grafana](https://grafana.com/) - 数据可视化
- [Elasticsearch](https://www.elastic.co/) - 搜索引擎
- [Jaeger](https://www.jaegertracing.io/) - 分布式追踪

### DevOps

- [Harbor](https://goharbor.io/) - 镜像仓库
- [Terraform](https://www.terraform.io/) - 基础设施即代码
- [Ansible](https://www.ansible.com/) - 自动化运维
- [GitLab](https://about.gitlab.com/) - DevOps 平台

### 安全

- [HashiCorp Vault](https://www.hashicorp.com/products/vault) - 密钥管理
- [Suricata](https://suricata.io/) - 入侵检测
- [Wazuh](https://wazuh.com/) - 安全平台

以及所有其他开源项目和贡献者！

---

## 📮 联系我们

| 渠道 | 地址 |
|------|------|
| **官方网站** | https://opencloud.io（待建设）|
| **GitHub** | https://github.com/qoobots/opencloud |
| **Issues** | https://github.com/qoobots/opencloud/issues |
| **Discussions** | https://github.com/qoobots/opencloud/discussions |
| **邮箱** | hello@qoobot.com |
| **技术支持** | hello@qoobot.com |

### 社区

- 📧 **邮件列表**: hello@qoobot.com
- 💬 **Discord**: [加入我们的 Discord](https://discord.gg/your-invite)（待建设）
- 🐦 **Twitter**: [@opencloud](https://twitter.com/opencloud)（待建设）
- 📺 **YouTube**: [OpenCloud Channel](https://youtube.com/@opencloud)（待建设）

---

## ⭐ Star History

如果这个项目对你有帮助，请给我们一个 ⭐ Star！

[![Star History Chart](https://api.star-history.com/svg?repos=qoobots/opencloud&type=Date)](https://star-history.com/#qoobots/opencloud&Date)

---

## 📄 License

本项目采用 [Apache License 2.0](./LICENSE) 开源协议。

```
Copyright 2024 OpenCloud Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<div align="center">

**Made with ❤️ by [OpenCloud Team](https://github.com/qoobots/opencloud/graphs/contributors)**

[⬆ 返回顶部](#opencloud-开源云计算平台)

</div>
