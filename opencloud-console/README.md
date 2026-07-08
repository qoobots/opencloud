# OpenCloud Console

<div align="center">

**云计算平台统一管理控制台**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D.svg)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](../../LICENSE)

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [技术栈](#-技术栈) • [项目结构](#-项目结构) • [API 文档](#-api-文档) • [贡献指南](#-贡献指南)

</div>

---

## 📖 项目简介

OpenCloud Console 是 OpenCloud 开源云计算平台的统一管理控制台，提供用户友好的 Web 界面，实现对虚拟化、容器化、存储、网络、监控等云资源的统一管理。

### 核心特性

- 🎨 **现代化界面**：基于 Vue 3 + Element Plus，提供流畅的用户体验
- 🔐 **安全认证**：Spring Security + JWT + RBAC 权限控制
- 📊 **可视化监控**：集成 Prometheus + Grafana，实时监控平台状态
- ☁️ **多云管理**：统一管理 OpenStack、Kubernetes、Ceph 等云平台
- 🤖 **AI 辅助运维**：基于 pgvector 向量化分析，智能告警和根因分析
- 🚀 **高性能**：Spring Boot 3 + Redis 缓存，响应快速
- 📦 **容器化部署**：支持 Docker + docker-compose 快速部署

---

## ✨ 功能特性

### 已实现功能

- ✅ **系统管理**：用户管理、角色管理、菜单管理
- ✅ **认证授权**：JWT 登录、RBAC 权限控制
- ✅ **监控集成**：Prometheus 指标查询、Grafana 仪表盘
- ✅ **API 文档**：Knife4j 自动生成 API 文档
- ✅ **基础架构**：模块化设计、统一异常处理、统一响应格式

### 开发中功能

- 🚧 **资源总览**：对接 OpenStack / Ceph / Kubernetes 真实 API
- 🚧 **监控告警**：Prometheus 告警规则、WebSocket 实时推送
- 🚧 **操作日志**：AOP 切面记录操作日志
- 🚧 **AI 辅助运维**：pgvector 向量化告警日志，智能根因分析

### 计划功能

- 📋 **虚拟机管理**：创建、启停、删除虚拟机
- 📋 **容器管理**：Kubernetes 集群管理、应用部署
- 📋 **存储管理**：云盘管理、对象存储管理
- 📋 **网络管理**：VPC、安全组、负载均衡
- 📋 **计费系统**：资源计量、账单生成
- 📋 **工单系统**：运维工单、审批流程

---

## 🎯 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.2 | 应用框架 |
| **Java** | 17 | 开发语言 |
| **MyBatis-Plus** | 3.5+ | ORM 框架 |
| **PostgreSQL** | 16 | 主数据库 |
| **pgvector** | 0.5+ | 向量扩展（AI 分析） |
| **Redis** | 7+ | 缓存 |
| **Spring Security** | 6.2+ | 安全框架 |
| **JWT** | 0.12+ | Token 认证 |
| **Knife4j** | 4.3+ | API 文档 |
| **OpenStack4j** | 3.8+ | OpenStack SDK |
| **Fabric8** | 6.9+ | Kubernetes SDK |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue** | 3.4+ | 前端框架 |
| **Vite** | 5.0+ | 构建工具 |
| **Element Plus** | 2.4+ | UI 组件库 |
| **Pinia** | 2.1+ | 状态管理 |
| **Vue Router** | 4.2+ | 路由管理 |
| **Axios** | 1.6+ | HTTP 请求 |
| **ECharts** | 5.4+ | 数据可视化 |

### 基础设施

| 技术 | 版本 | 说明 |
|------|------|------|
| **Docker** | 24+ | 容器化 |
| **Docker Compose** | 2.20+ | 开发环境编排 |
| **Prometheus** | 2.48+ | 监控指标采集 |
| **Grafana** | 10.2+ | 监控可视化 |

---

## 🚀 快速开始

### 前置要求

- **Java**: JDK 17+
- **Node.js**: 18.x+
- **Maven**: 3.8+
- **Docker**: 24.0+
- **Docker Compose**: 2.20+

### 安装步骤

#### 1. 克隆仓库

```bash
git clone https://github.com/qoobots/opencloud.git
cd opencloud/opencloud-console
```

#### 2. 启动基础设施

```bash
# 启动 PostgreSQL、Redis、Prometheus、Grafana
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 3. 初始化数据库

```bash
# 数据库会在首次启动时自动初始化
# 初始化脚本位置：opencloud-web/src/main/resources/db/init.sql
```

#### 4. 配置环境变量（可选）

```bash
# 复制环境变量配置文件
cp .env.example .env

# 根据实际情况修改配置
vim .env
```

#### 5. 启动后端

```bash
# 方式一：使用 Maven
cd opencloud-web
mvn clean install -DskipTests
mvn spring-boot:run

# 方式二：使用 IDEA
# 直接运行 opencloud-web 模块的 OpenCloudWebApplication.java
```

后端启动成功后，访问：http://localhost:8080/doc.html

#### 6. 启动前端

```bash
cd opencloud-ui

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动成功后，访问：http://localhost:3000

### 访问地址

| 服务 | 地址 | 账号密码 |
|------|------|----------|
| **前端控制台** | http://localhost:3000 | admin / Admin@123 |
| **API 文档** | http://localhost:8080/doc.html | - |
| **Grafana** | http://localhost:3001 | admin / admin123 |
| **Prometheus** | http://localhost:9090 | - |
| **PostgreSQL** | localhost:5432 | opencloud / opencloud123 |
| **Redis** | localhost:6379 | - |

---

## 📂 项目结构

### 整体结构

```
opencloud-console/
├── pom.xml                           # 父 POM，版本统一管理
├── docker-compose.yml                # 开发环境基础设施
├── .env.example                      # 环境变量配置示例
├── opencloud-common/                  # 公共模块
│   ├── src/main/java/
│   │   └── com/opencloud/common/
│   │       ├── constant/             # 常量定义
│   │       ├── domain/               # 基础实体（BaseEntity、BaseDTO）
│   │       ├── enums/                # 枚举类
│   │       ├── exception/            # 全局异常
│   │       ├── result/               # 统一响应
│   │       ├── util/                 # 工具类（JWT、日期、加密等）
│   │       └── config/               # 公共配置
├── opencloud-auth/                    # 认证授权模块
│   ├── src/main/java/
│   │   └── com/opencloud/auth/
│   │       ├── config/               # Spring Security 配置
│   │       ├── filter/               # JWT 过滤器
│   │       ├── service/              # 认证服务
│   │       └── vo/                   # 登录/注册 VO
├── opencloud-system/                  # 系统管理模块
│   ├── src/main/java/
│   │   └── com/opencloud/system/
│   │       ├── controller/           # 控制器
│   │       ├── service/              # 服务层
│   │       ├── mapper/               # Mapper 层
│   │       ├── entity/               # 实体类（用户、角色、菜单）
│   │       └── dto/                  # 数据传输对象
├── opencloud-monitor/                 # 监控告警模块
│   ├── src/main/java/
│   │   └── com/opencloud/monitor/
│   │       ├── controller/           # 监控控制器
│   │       ├── service/              # 监控服务（Prometheus、告警）
│   │       ├── websocket/            # WebSocket 推送
│   │       └── dto/                  # 监控 DTO
├── opencloud-cloud/                   # 云平台对接模块
│   ├── src/main/java/
│   │   └── com/opencloud/cloud/
│   │       ├── openstack/            # OpenStack 对接
│   │       ├── kubernetes/           # Kubernetes 对接
│   │       ├── ceph/                 # Ceph 对接
│   │       └── dto/                  # 云平台 DTO
├── opencloud-web/                     # 主启动模块
│   ├── src/main/java/
│   │   └── com/opencloud/
│   │       └── OpenCloudWebApplication.java  # 启动类
│   └── src/main/resources/
│       ├── application.yml           # 主配置文件
│       ├── application-dev.yml       # 开发环境配置
│       ├── application-prod.yml      # 生产环境配置
│       ├── db/
│       │   └── init.sql              # 数据库初始化脚本
│       └── logback-spring.xml        # 日志配置
└── opencloud-ui/                      # Vue 3 前端
    ├── public/                        # 静态资源
    ├── src/
    │   ├── api/                      # API 请求封装
    │   │   ├── auth.js               # 认证 API
    │   │   ├── system.js             # 系统 API
    │   │   ├── monitor.js            # 监控 API
    │   │   └── cloud.js              # 云平台 API
    │   ├── assets/                   # 资源文件
    │   ├── components/               # 公共组件
    │   ├── layouts/                  # 布局组件
    │   │   ├── Layout.vue            # 主布局
    │   │   ├── Sidebar.vue           # 侧边栏
    │   │   ├── Header.vue            # 顶栏
    │   │   └── Breadcrumb.vue        # 面包屑
    │   ├── router/                   # 路由配置
    │   │   └── index.js
    │   ├── stores/                   # Pinia 状态管理
    │   │   ├── user.js               # 用户状态
    │   │   ├── cluster.js            # 集群状态
    │   │   └── alert.js              # 告警状态
    │   ├── utils/                    # 工具函数
    │   ├── views/                    # 页面组件
    │   │   ├── login/                # 登录页
    │   │   ├── dashboard/            # 仪表盘
    │   │   ├── system/               # 系统管理
    │   │   ├── monitor/              # 监控告警
    │   │   └── cloud/                # 云平台资源
    │   ├── App.vue                   # 根组件
    │   └── main.js                   # 入口文件
    ├── package.json                  # 依赖配置
    ├── vite.config.js                # Vite 配置
    └── .env                          # 环境变量
```

---

## ⚙️ 环境变量说明

### 后端配置

| 变量 | 默认值 | 说明 | 必填 |
|------|--------|------|------|
| `DB_HOST` | localhost | PostgreSQL 地址 | 是 |
| `DB_PORT` | 5432 | PostgreSQL 端口 | 是 |
| `DB_NAME` | opencloud | 数据库名 | 是 |
| `DB_USER` | opencloud | 数据库用户 | 是 |
| `DB_PASS` | opencloud123 | 数据库密码 | 是 |
| `REDIS_HOST` | localhost | Redis 地址 | 是 |
| `REDIS_PORT` | 6379 | Redis 端口 | 否 |
| `REDIS_PASS` | - | Redis 密码 | 否 |
| `JWT_SECRET` | - | JWT 签名密钥（生产必须修改） | 是 |
| `JWT_EXPIRATION` | 86400000 | JWT 过期时间（毫秒） | 否 |
| `OS_AUTH_URL` | - | OpenStack Keystone 地址 | 否 |
| `OS_USERNAME` | - | OpenStack 用户名 | 否 |
| `OS_PASSWORD` | - | OpenStack 密码 | 否 |
| `OS_PROJECT` | - | OpenStack 项目名 | 否 |
| `PROMETHEUS_URL` | http://prometheus:9090 | Prometheus 地址 | 否 |
| `GRAFANA_URL` | http://grafana:3000 | Grafana 地址 | 否 |

### 前端配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | http://localhost:8080 | API 基础地址 |
| `VITE_APP_TITLE` | OpenCloud Console | 应用标题 |
| `VITE_GRAFANA_URL` | http://localhost:3001 | Grafana 地址 |

---

## 📖 API 文档

启动后端服务后，访问以下地址查看 API 文档：

- **Knife4j 文档**: http://localhost:8080/doc.html
- **Swagger 原始文档**: http://localhost:8080/swagger-ui.html

### API 认证

所有需要认证的 API 都需要在请求头中携带 JWT Token：

```http
Authorization: Bearer {token}
```

获取 Token：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

---

## 🧪 测试

### 后端测试

```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl opencloud-system

# 生成测试覆盖率报告
mvn clean test jacoco:report
```

### 前端测试

```bash
# 运行单元测试
npm run test

# 运行 E2E 测试
npm run test:e2e

# 生成测试覆盖率报告
npm run test:coverage
```

---

## 📦 构建与部署

### 后端构建

```bash
# 打包为 JAR
mvn clean package -DskipTests

# 生成的 JAR 文件位置
# opencloud-web/target/opencloud-web.jar

# 运行
java -jar opencloud-web/target/opencloud-web.jar
```

### 前端构建

```bash
# 构建生产版本
npm run build

# 生成的静态文件位置
# opencloud-ui/dist/
```

### Docker 部署

```bash
# 构建后端镜像
docker build -t opencloud-console:latest -f docker/Dockerfile .

# 构建前端镜像
docker build -t opencloud-ui:latest -f opencloud-ui/Dockerfile .

# 使用 docker-compose 部署
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes 部署

```bash
# 部署到 Kubernetes
kubectl apply -f k8s/

# 查看 Pod 状态
kubectl get pods -n opencloud

# 查看服务
kubectl get svc -n opencloud
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 如何贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 开发规范

#### 后端开发规范

- 遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- 使用统一的代码格式化配置（Google Java Style）
- 编写单元测试，测试覆盖率不低于 60%
- 所有 API 必须添加 Knife4j 注解
- 提交前运行 `mvn clean checkstyle:check` 检查代码规范

#### 前端开发规范

- 遵循 [Vue 风格指南](https://vuejs.org/style-guide/)
- 使用 ESLint + Prettier 进行代码格式化
- 组件命名使用 PascalCase，文件名使用 kebab-case
- 提交前运行 `npm run lint` 检查代码规范
- 提交前运行 `npm run test` 确保测试通过

### Commit 规范

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
```

示例：

```
git commit -m "feat: 添加 Kubernetes 集群管理功能"
git commit -m "fix: 修复用户登录时的 token 过期问题"
```

---

## 🐛 问题反馈

如果您发现了 Bug 或有功能建议，请通过以下方式反馈：

- [GitHub Issues](https://github.com/qoobots/opencloud/issues)
- [GitHub Discussions](https://github.com/qoobots/opencloud/discussions)

反馈问题时，请提供以下信息：

- 问题描述
- 复现步骤
- 预期行为
- 实际行为
- 环境信息（OS、Java/Node 版本）
- 日志截图或错误信息

---

## 📜 开发路线图

### v0.1.0 - MVP 版本（开发中）

- [x] 基础架构搭建
- [x] 用户认证授权
- [x] 系统管理模块
- [x] 监控基础集成
- [ ] 虚拟机管理
- [ ] 容器管理
- [ ] 存储管理
- [ ] 网络管理

### v0.2.0 - 企业版（计划中）

- [ ] 高级监控告警
- [ ] AI 辅助运维
- [ ] 工单系统
- [ ] 计费系统
- [ ] 多租户支持

### v1.0.0 - 正式版（计划中）

- [ ] 完整的 OpenStack 集成
- [ ] 完整的 Kubernetes 集成
- [ ] 完整的 Ceph 集成
- [ ] 混合云管理
- [ ] 高可用部署
- [ ] 多语言支持

---

## 📄 开源协议

本项目采用 [Apache License 2.0](../../LICENSE) 开源协议。

---

## 🙏 致谢

OpenCloud Console 的开发和维护离不开以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MyBatis-Plus](https://baomidou.com/)
- [PostgreSQL](https://www.postgresql.org/)
- [pgvector](https://github.com/pgvector/pgvector)
- [Knife4j](https://doc.xiaominfo.com/)
- 以及所有其他开源项目

---

## 📮 联系我们

- **GitHub**: https://github.com/qoobots/opencloud
- **Issues**: https://github.com/qoobots/opencloud/issues
- **Discussions**: https://github.com/qoobots/opencloud/discussions
- **邮箱**: dev@opencloud.io

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star！**

Made with ❤️ by OpenCloud Team

</div>
