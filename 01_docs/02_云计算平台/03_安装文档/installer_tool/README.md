# OpenCloud 自动化安装工具

> Windows GUI 工具，通过 SSH/SFTP 一键向远程 Linux 服务器自动部署云计算平台组件。

---

## 📦 支持安装的组件

| 组件 | 技术栈 | 依赖 |
|------|--------|------|
| Ceph 分布式存储 | cephadm | - |
| OpenStack 云平台 | Kolla-Ansible | Ceph |
| Kubernetes 容器编排 | kubeadm HA | Ceph |
| Prometheus 监控系统 | kube-prometheus-stack | Kubernetes |
| Grafana 可视化面板 | Helm | Prometheus |

---

## 🚀 快速开始

### 方式一：直接运行（开发模式）

**环境要求：** Python 3.10+（Windows）

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 启动工具
run.bat
# 或
python main.py
```

### 方式二：打包为独立 .exe（无需 Python 环境）

```bash
# 双击运行，自动安装依赖并打包
build.bat

# 打包完成后，可执行文件在 dist/ 目录：
# dist/OpenCloud安装工具.exe
```

---

## 🖥 界面说明

### 服务器管理（左侧 Tab）

| 操作 | 说明 |
|------|------|
| ➕ 添加 | 填写服务器名称、IP、端口、用户名、密码或 SSH 密钥 |
| ✏️ 编辑 | 修改已有服务器配置 |
| 🗑 删除 | 删除服务器记录 |
| 🔍 测试连接 | 验证 SSH 连接是否正常 |

**支持两种认证方式：**
- **密码认证**：填写用户名+密码
- **SSH 密钥认证**：选择 `.pem` 私钥文件（优先级高于密码）

### 组件选择（左侧 Tab）

- 勾选/取消要安装的组件
- **自动依赖管理**：取消某组件时，依赖它的组件自动取消；勾选时自动勾选所需依赖

### 安装进度（右侧）

- 每个组件独立进度条，实时显示安装状态
- 颜色含义：
  - 🟡 橙色旋转 = 安装中
  - 🟢 绿色满格 = 安装成功
  - 🔴 红色空格 = 安装失败
  - ⚪ 灰色 = 跳过

### 日志输出（右侧）

- 实时显示 SSH 命令输出、脚本日志
- 颜色编码：白色=普通信息，绿色=成功，红色=错误，橙色=警告，青色=命令
- 日志自动保存到 `logs/install_YYYYMMDD_HHMMSS.log`

---

## 📂 目录结构

```
installer_tool/
├── main.py                    # 主入口
├── run.bat                    # 开发模式启动脚本
├── build.bat                  # 一键打包脚本
├── requirements.txt           # Python 依赖
├── opencloud_installer.spec   # PyInstaller 打包配置
├── config.json                # 运行时保存的配置（自动生成）
├── src/
│   ├── config.py              # 配置管理模块
│   ├── ssh_client.py          # SSH/SFTP 模块（基于 paramiko）
│   ├── installer.py           # 安装任务编排引擎
│   └── gui.py                 # GUI 主界面（tkinter）
├── packages/                  # 放置离线安装包（会自动上传到服务器）
└── logs/                      # 安装日志目录
```

---

## ⚙️ 工作原理

```
Windows GUI
    │
    ├─ 1. 用户配置服务器（IP/端口/认证）
    ├─ 2. 用户选择安装组件
    └─ 3. 点击「开始安装」
            │
            ├─ SSH 连接目标服务器
            ├─ 在服务器创建工作目录 /opt/opencloud_installer/
            ├─ SFTP 上传部署脚本（deploy_*.sh）和离线包
            └─ 按依赖顺序 SSH 执行脚本：
                  deploy_ceph.sh --all
                  deploy_openstack.sh --all
                  deploy_kubernetes.sh --all
                  deploy_prometheus.sh --all
                  deploy_grafana.sh --all
```

---

## 🔧 离线安装包

如需离线安装，将安装包放入 `packages/` 目录，工具会自动通过 SFTP 上传到服务器的 `/opt/opencloud_installer/packages/`。

部署脚本中可通过 `$PACKAGES_DIR` 变量引用。

---

## ❓ 常见问题

**Q: 连接测试失败？**  
A: 检查服务器防火墙是否开放 22 端口；密码中含特殊字符时建议改用 SSH 密钥。

**Q: 脚本上传成功但执行失败？**  
A: 查看日志中的错误信息；确保服务器用户有 sudo 权限；检查服务器磁盘空间是否充足。

**Q: 打包后 exe 双击没反应？**  
A: 尝试在命令行运行 `OpenCloud安装工具.exe` 查看错误；或使用 `run.bat` 以 Python 模式运行排查。

---

## 📝 许可

MIT License
