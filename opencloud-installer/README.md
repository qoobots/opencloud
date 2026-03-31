# OpenCloud 安装工具（Electron 版）

> 基于 **Electron + Node.js** 的 Windows 桌面安装工具，零运行环境依赖，一键部署云计算平台。

---

## ✨ 功能特性

- 🎨 **现代暗色 UI** — 精心设计的暗色主题，流畅动效
- 🖥 **服务器管理** — 支持多服务器配置，密码 / SSH 密钥双认证
- 📦 **组件选择** — 卡片式开关，智能依赖处理
- 📊 **实时进度** — 每个组件独立进度条，颜色状态指示
- 📜 **实时日志** — 彩色日志滚动输出，自动保存到本地文件
- 💾 **持久配置** — 服务器和组件配置自动保存，重启不丢失
- 📦 **独立可执行** — 打包后无需安装 Node.js 或任何运行环境

---

## 🚀 快速开始

### 方式一：直接运行（需要 Node.js 18+）

```bash
# 双击运行
start.bat

# 或命令行
npm install
npm start
```

### 方式二：打包为独立 .exe

```bash
# 双击运行，自动打包
build.bat

# 或命令行
npm run build
# 输出在 dist/ 目录：
#   OpenCloud安装工具 Setup 1.0.0.exe   ← 安装包
#   OpenCloud安装工具 1.0.0.exe          ← 绿色版（免安装）
```

---

## 📂 项目结构

```
opencloud-installer/
├── package.json              # 项目配置 & 打包设置
├── start.bat                 # 一键启动（开发模式）
├── build.bat                 # 一键打包
├── src/
│   ├── main/
│   │   ├── main.js           # Electron 主进程
│   │   ├── preload.js        # 预加载脚本（IPC 桥接）
│   │   ├── config-store.js   # 配置持久化
│   │   └── ssh-service.js    # SSH/SFTP 安装引擎
│   └── renderer/
│       ├── index.html        # 主界面 HTML
│       ├── style.css         # 暗色主题样式
│       └── app.js            # 前端交互逻辑
├── packages/                 # 放置离线安装包（自动上传）
└── logs/                     # 安装日志（自动生成）
```

---

## 🖥 界面说明

### 服务器页面
- 添加、编辑、删除服务器配置
- 支持 SSH 密码认证和私钥（.pem）认证
- 内置连接测试功能

### 组件页面
- 5 个云平台组件的开关卡片
- 自动处理依赖（关闭 Ceph → 自动关闭依赖 Ceph 的组件）
- 一键全选 / 全不选

### 安装页面
- 下拉选择目标服务器
- 任务进度看板（每个组件状态一目了然）
- 彩色实时日志输出

---

## ⚙️ 工作原理

```
用户操作
  │
  ├─ 配置服务器（IP/端口/认证方式）
  ├─ 选择安装组件
  └─ 点击「开始安装」
        │
        ├─ SSH 连接目标 Linux 服务器
        ├─ 创建远程目录 /opt/opencloud_installer/
        ├─ SFTP 上传部署脚本 deploy_*.sh
        ├─ SFTP 上传 packages/ 下的离线包（如有）
        └─ 按依赖顺序执行脚本：
              bash deploy_ceph.sh --all
              bash deploy_openstack.sh --all
              bash deploy_kubernetes.sh --all
              bash deploy_prometheus.sh --all
              bash deploy_grafana.sh --all
```

---

## 🔧 离线安装包支持

将安装包放入 `packages/` 目录，工具会自动上传到服务器：
```
packages/
├── ceph-17.2.7.tar.gz
├── kolla-ansible-17.0.0.tar.gz
└── ...
```
部署脚本中通过 `$PACKAGES_DIR` 变量访问。

---

## ❓ 常见问题

**Q: 连接测试失败？**
A: 检查防火墙 22 端口是否开放；密码含特殊字符建议改用 SSH 密钥。

**Q: 如何使用 SSH 密钥认证？**
A: 编辑服务器 → 选择 `.pem` 私钥文件 → 密码留空即可。

**Q: 打包体积为什么这么大（~120MB）？**
A: Electron 内嵌了 Chromium 浏览器内核，因此体积较大，但无需用户安装任何环境。

---

## 📝 依赖说明

| 包 | 用途 |
|----|------|
| `electron` | 桌面应用框架 |
| `ssh2` | SSH 连接 + SFTP 文件传输 |
| `electron-builder` | 打包为 Windows .exe |
