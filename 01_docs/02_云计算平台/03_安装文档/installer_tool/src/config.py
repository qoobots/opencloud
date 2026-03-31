# -*- coding: utf-8 -*-
"""
配置管理模块 - 负责加载、保存服务器配置和组件配置
"""

import json
import os
from pathlib import Path

# 工具根目录
BASE_DIR = Path(__file__).parent.parent

# 配置文件路径
CONFIG_FILE = BASE_DIR / "config.json"

# 日志目录
LOG_DIR = BASE_DIR / "logs"
LOG_DIR.mkdir(exist_ok=True)

# 安装包目录
PACKAGES_DIR = BASE_DIR / "packages"
PACKAGES_DIR.mkdir(exist_ok=True)

# 远程服务器工作目录
REMOTE_WORK_DIR = "/opt/opencloud_installer"

# 支持的组件定义
COMPONENTS = {
    "ceph": {
        "name": "Ceph 分布式存储",
        "description": "基于 cephadm 部署 Ceph 集群，提供块存储、对象存储和文件存储",
        "script": "deploy_ceph.sh",
        "icon": "🗄️",
        "requires": [],
        "default_enabled": True,
        "order": 1,
    },
    "openstack": {
        "name": "OpenStack 云平台",
        "description": "基于 Kolla-Ansible 容器化部署 OpenStack IaaS 平台",
        "script": "deploy_openstack.sh",
        "icon": "☁️",
        "requires": ["ceph"],
        "default_enabled": True,
        "order": 2,
    },
    "kubernetes": {
        "name": "Kubernetes 容器编排",
        "description": "基于 kubeadm 部署高可用 Kubernetes 集群",
        "script": "deploy_kubernetes.sh",
        "icon": "⎈",
        "requires": ["ceph"],
        "default_enabled": True,
        "order": 3,
    },
    "prometheus": {
        "name": "Prometheus 监控系统",
        "description": "部署 kube-prometheus-stack，提供完整监控告警能力",
        "script": "deploy_prometheus.sh",
        "icon": "📊",
        "requires": ["kubernetes"],
        "default_enabled": True,
        "order": 4,
    },
    "grafana": {
        "name": "Grafana 可视化面板",
        "description": "部署 Grafana，提供监控数据可视化和告警通知",
        "script": "deploy_grafana.sh",
        "icon": "📈",
        "requires": ["prometheus"],
        "default_enabled": True,
        "order": 5,
    },
}

# 默认配置模板
DEFAULT_CONFIG = {
    "servers": [],
    "last_used_server": None,
    "components": {k: v["default_enabled"] for k, v in COMPONENTS.items()},
    "ssh": {
        "timeout": 30,
        "banner_timeout": 60,
        "auth_timeout": 60,
    },
    "upload": {
        "chunk_size": 32768,
        "remote_work_dir": REMOTE_WORK_DIR,
    },
}


class ConfigManager:
    """配置管理器"""

    def __init__(self):
        self._config = {}
        self.load()

    def load(self):
        """从文件加载配置"""
        if CONFIG_FILE.exists():
            try:
                with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                    saved = json.load(f)
                self._config = {**DEFAULT_CONFIG, **saved}
            except Exception:
                self._config = DEFAULT_CONFIG.copy()
        else:
            self._config = DEFAULT_CONFIG.copy()

    def save(self):
        """保存配置到文件"""
        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            json.dump(self._config, f, ensure_ascii=False, indent=2)

    @property
    def servers(self):
        return self._config.get("servers", [])

    def add_server(self, server: dict):
        """添加服务器配置"""
        servers = self.servers
        # 避免重名
        for s in servers:
            if s["name"] == server["name"]:
                s.update(server)
                self.save()
                return
        servers.append(server)
        self._config["servers"] = servers
        self.save()

    def remove_server(self, name: str):
        """删除服务器配置"""
        self._config["servers"] = [s for s in self.servers if s["name"] != name]
        self.save()

    def get_server(self, name: str):
        """按名称获取服务器"""
        for s in self.servers:
            if s["name"] == name:
                return s
        return None

    @property
    def selected_components(self):
        return self._config.get("components", {})

    def set_component(self, key: str, enabled: bool):
        """设置组件启用状态"""
        self._config.setdefault("components", {})[key] = enabled
        self.save()

    @property
    def ssh_config(self):
        return self._config.get("ssh", DEFAULT_CONFIG["ssh"])

    @property
    def upload_config(self):
        return self._config.get("upload", DEFAULT_CONFIG["upload"])


# 全局配置实例
config_manager = ConfigManager()
