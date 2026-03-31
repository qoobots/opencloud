# -*- coding: utf-8 -*-
"""
安装任务编排模块 - 负责协调上传脚本、执行安装、汇报进度
"""

import logging
import os
import threading
import time
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Callable, Dict, List, Optional

from config import COMPONENTS, PACKAGES_DIR, REMOTE_WORK_DIR, config_manager
from ssh_client import SSHClient


class TaskStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"
    SKIPPED = "skipped"


class InstallTask:
    """单个组件的安装任务"""

    def __init__(self, component_key: str):
        self.key = component_key
        self.info = COMPONENTS[component_key]
        self.status = TaskStatus.PENDING
        self.start_time: Optional[float] = None
        self.end_time: Optional[float] = None
        self.error_msg: str = ""

    @property
    def elapsed(self) -> str:
        if self.start_time is None:
            return "-"
        end = self.end_time or time.time()
        secs = int(end - self.start_time)
        return f"{secs // 60}m {secs % 60}s"

    @property
    def display_name(self) -> str:
        return self.info["name"]


class InstallerEngine:
    """
    安装引擎 - 在后台线程执行，通过回调向 GUI 汇报进度
    """

    def __init__(
        self,
        server: dict,
        selected_components: List[str],
        log_callback: Callable,
        progress_callback: Callable,
        status_callback: Callable,
        finished_callback: Callable,
    ):
        """
        :param server: 服务器配置
        :param selected_components: 要安装的组件 key 列表（有序）
        :param log_callback: log_callback(message, level)
        :param progress_callback: progress_callback(task_key, transferred, total, pct)
        :param status_callback: status_callback(task_key, TaskStatus)
        :param finished_callback: finished_callback(success: bool, summary: str)
        """
        self.server = server
        self.selected = selected_components
        self.log = log_callback
        self.on_progress = progress_callback
        self.on_status = status_callback
        self.on_finished = finished_callback

        self.tasks: Dict[str, InstallTask] = {
            k: InstallTask(k) for k in selected_components
        }
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None
        self._ssh: Optional[SSHClient] = None

        # 日志文件
        log_dir = Path(__file__).parent.parent / "logs"
        log_dir.mkdir(exist_ok=True)
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        self._log_file = log_dir / f"install_{ts}.log"

    # ------------------------------------------------------------------ #
    # 公开接口
    # ------------------------------------------------------------------ #

    def start(self):
        """启动安装（异步）"""
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()

    def stop(self):
        """请求停止"""
        self._stop_event.set()
        self.log("⚠️ 正在停止安装任务...", "WARN")

    @property
    def is_running(self):
        return self._thread is not None and self._thread.is_alive()

    # ------------------------------------------------------------------ #
    # 内部执行流程
    # ------------------------------------------------------------------ #

    def _run(self):
        """主执行流程（在子线程中运行）"""
        try:
            self._write_log_header()

            # 1. 建立 SSH 连接
            self._ssh = SSHClient(self.server, log_callback=self._log_and_file)
            self.log("═" * 60, "INFO")
            self.log("🚀 开始安装流程", "INFO")
            self.log(f"目标服务器: {self.server['host']}", "INFO")
            self.log(f"安装组件: {', '.join(self.selected)}", "INFO")
            self.log("═" * 60, "INFO")

            if not self._ssh.connect(timeout=30):
                self.on_finished(False, "无法连接到服务器，请检查连接配置")
                return

            # 2. 初始化远程工作目录
            self._init_remote_dir()

            # 3. 上传所有脚本
            if not self._upload_scripts():
                self._ssh.disconnect()
                self.on_finished(False, "脚本上传失败，安装中止")
                return

            # 4. 逐个安装组件
            all_success = True
            for key in self.selected:
                if self._stop_event.is_set():
                    self.log("⚠️ 用户中止安装", "WARN")
                    break

                task = self.tasks[key]
                success = self._install_component(task)
                if not success:
                    all_success = False
                    # 检查是否有依赖此组件的后续任务
                    self._skip_dependent_tasks(key)

            # 5. 完成
            self._ssh.disconnect()
            summary = self._build_summary()
            self.log("═" * 60, "INFO")
            self.log(summary, "INFO")
            self.on_finished(all_success, summary)

        except Exception as e:
            self.log(f"❌ 安装引擎异常: {e}", "ERROR")
            if self._ssh:
                self._ssh.disconnect()
            self.on_finished(False, f"安装过程中发生异常: {e}")

    def _init_remote_dir(self):
        """初始化远程目录"""
        self.log(f"初始化远程目录: {REMOTE_WORK_DIR}", "INFO")
        self._ssh.exec_command(f"mkdir -p {REMOTE_WORK_DIR}/scripts {REMOTE_WORK_DIR}/packages")

    def _upload_scripts(self) -> bool:
        """上传部署脚本到服务器"""
        self.log("📤 上传部署脚本...", "INFO")

        # 脚本目录：installer_tool 上一级（03_安装文档）
        scripts_dir = Path(__file__).parent.parent.parent

        for key in self.selected:
            if self._stop_event.is_set():
                return False

            script_name = COMPONENTS[key]["script"]
            local_script = scripts_dir / script_name

            if not local_script.exists():
                self.log(f"⚠️ 脚本不存在: {local_script}, 将跳过", "WARN")
                continue

            remote_path = f"{REMOTE_WORK_DIR}/scripts/{script_name}"
            ok = self._ssh.upload_file(
                str(local_script),
                remote_path,
                progress_callback=lambda t, total, pct, k=key: self.on_progress(k, t, total, pct),
            )
            if not ok:
                return False

        # 上传本地 packages 目录下的安装包
        packages_dir = Path(__file__).parent.parent / "packages"
        if packages_dir.exists() and any(packages_dir.iterdir()):
            self.log("📦 上传安装包...", "INFO")
            self._ssh.upload_directory(
                str(packages_dir),
                f"{REMOTE_WORK_DIR}/packages",
            )

        return True

    def _install_component(self, task: InstallTask) -> bool:
        """安装单个组件"""
        task.status = TaskStatus.RUNNING
        task.start_time = time.time()
        self.on_status(task.key, TaskStatus.RUNNING)

        self.log("─" * 50, "INFO")
        self.log(f"▶ 开始安装: {task.display_name}", "INFO")

        script_name = task.info["script"]
        remote_script = f"{REMOTE_WORK_DIR}/scripts/{script_name}"

        # 检查脚本是否存在
        exit_code, _, _ = self._ssh.exec_command(f"test -f {remote_script} && echo OK")
        if "OK" not in _:
            # 脚本不存在，尝试用内置方式安装
            self.log(f"⚠️ 未找到脚本 {script_name}，跳过", "WARN")
            task.status = TaskStatus.SKIPPED
            task.end_time = time.time()
            self.on_status(task.key, TaskStatus.SKIPPED)
            return True

        # 执行脚本
        ok = self._ssh.exec_script(
            remote_script,
            args="--all",
            output_callback=lambda line: self._log_and_file(line, "OUTPUT"),
            timeout=7200,  # 2小时超时
        )

        task.end_time = time.time()
        if ok:
            task.status = TaskStatus.SUCCESS
            self.log(f"✅ {task.display_name} 安装成功 (耗时 {task.elapsed})", "SUCCESS")
        else:
            task.status = TaskStatus.FAILED
            task.error_msg = "脚本返回非零退出码"
            self.log(f"❌ {task.display_name} 安装失败 (耗时 {task.elapsed})", "ERROR")

        self.on_status(task.key, task.status)
        return ok

    def _skip_dependent_tasks(self, failed_key: str):
        """将依赖于失败组件的后续任务标记为跳过"""
        for key, task in self.tasks.items():
            deps = COMPONENTS[key].get("requires", [])
            if failed_key in deps and task.status == TaskStatus.PENDING:
                task.status = TaskStatus.SKIPPED
                task.error_msg = f"依赖 {failed_key} 安装失败"
                self.log(f"⏭ 跳过 {task.display_name}（依赖 {failed_key} 失败）", "WARN")
                self.on_status(key, TaskStatus.SKIPPED)

    def _build_summary(self) -> str:
        lines = ["📋 安装结果汇总", "─" * 40]
        icons = {
            TaskStatus.SUCCESS: "✅",
            TaskStatus.FAILED: "❌",
            TaskStatus.SKIPPED: "⏭",
            TaskStatus.PENDING: "⏸",
            TaskStatus.RUNNING: "🔄",
        }
        for key in self.selected:
            task = self.tasks[key]
            icon = icons.get(task.status, "?")
            lines.append(f"{icon} {task.display_name}: {task.status.value} ({task.elapsed})")
            if task.error_msg:
                lines.append(f"   └ {task.error_msg}")
        return "\n".join(lines)

    def _log_and_file(self, message: str, level: str = "INFO"):
        """写日志到 GUI 和文件"""
        self.log(message, level)
        try:
            ts = datetime.now().strftime("%H:%M:%S")
            with open(self._log_file, "a", encoding="utf-8") as f:
                f.write(f"[{ts}][{level}] {message}\n")
        except Exception:
            pass

    def _write_log_header(self):
        ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        header = f"""
{'='*60}
OpenCloud 自动化安装工具 - 安装日志
开始时间: {ts}
目标服务器: {self.server.get('host', 'N/A')}
安装组件: {', '.join(self.selected)}
{'='*60}
"""
        try:
            with open(self._log_file, "w", encoding="utf-8") as f:
                f.write(header)
        except Exception:
            pass
