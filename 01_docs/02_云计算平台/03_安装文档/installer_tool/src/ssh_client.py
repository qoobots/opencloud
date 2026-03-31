# -*- coding: utf-8 -*-
"""
SSH/SFTP 模块 - 负责连接服务器、上传文件、执行远程命令
"""

import os
import stat
import threading
import time
from pathlib import Path
from typing import Callable, Optional

import paramiko


class SSHClient:
    """SSH 客户端封装"""

    def __init__(self, server: dict, log_callback: Optional[Callable] = None):
        """
        :param server: 服务器配置字典，包含 host/port/username/password/key_file
        :param log_callback: 日志回调函数 callback(message, level)
        """
        self.server = server
        self.log = log_callback or (lambda msg, lvl="INFO": print(f"[{lvl}] {msg}"))
        self._ssh: Optional[paramiko.SSHClient] = None
        self._sftp: Optional[paramiko.SFTPClient] = None
        self._connected = False

    # ------------------------------------------------------------------ #
    # 连接管理
    # ------------------------------------------------------------------ #

    def connect(self, timeout: int = 30) -> bool:
        """建立 SSH 连接"""
        try:
            self.log(f"正在连接 {self.server['host']}:{self.server.get('port', 22)} ...", "INFO")
            client = paramiko.SSHClient()
            client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

            connect_kwargs = {
                "hostname": self.server["host"],
                "port": int(self.server.get("port", 22)),
                "username": self.server["username"],
                "timeout": timeout,
                "banner_timeout": 60,
                "auth_timeout": 60,
            }

            # 优先使用密钥认证
            key_file = self.server.get("key_file")
            if key_file and os.path.exists(key_file):
                connect_kwargs["key_filename"] = key_file
                connect_kwargs["look_for_keys"] = False
                self.log("使用 SSH 密钥认证", "INFO")
            else:
                connect_kwargs["password"] = self.server.get("password", "")
                connect_kwargs["look_for_keys"] = False
                self.log("使用密码认证", "INFO")

            client.connect(**connect_kwargs)
            self._ssh = client
            self._sftp = client.open_sftp()
            self._connected = True
            self.log(f"✅ 连接成功: {self.server['host']}", "SUCCESS")
            return True

        except paramiko.AuthenticationException:
            self.log("❌ 认证失败：用户名或密码/密钥错误", "ERROR")
            return False
        except paramiko.SSHException as e:
            self.log(f"❌ SSH 错误: {e}", "ERROR")
            return False
        except Exception as e:
            self.log(f"❌ 连接失败: {e}", "ERROR")
            return False

    def disconnect(self):
        """断开连接"""
        if self._sftp:
            try:
                self._sftp.close()
            except Exception:
                pass
        if self._ssh:
            try:
                self._ssh.close()
            except Exception:
                pass
        self._connected = False
        self.log("已断开连接", "INFO")

    @property
    def connected(self):
        return self._connected

    def test_connection(self) -> bool:
        """测试连接可用性"""
        if not self._connected:
            return False
        try:
            transport = self._ssh.get_transport()
            transport.send_ignore()
            return True
        except Exception:
            self._connected = False
            return False

    # ------------------------------------------------------------------ #
    # 远程命令执行
    # ------------------------------------------------------------------ #

    def exec_command(
        self,
        command: str,
        timeout: int = 300,
        output_callback: Optional[Callable] = None,
        sudo: bool = False,
    ) -> tuple:
        """
        执行远程命令
        :return: (exit_code, stdout, stderr)
        """
        if not self._connected:
            return -1, "", "未连接到服务器"

        if sudo and self.server.get("username") != "root":
            password = self.server.get("password", "")
            command = f'echo "{password}" | sudo -S bash -c \'{command}\''

        self.log(f"$ {command[:120]}{'...' if len(command) > 120 else ''}", "CMD")

        try:
            stdin, stdout, stderr = self._ssh.exec_command(command, timeout=timeout, get_pty=True)
            stdout.channel.set_combine_stderr(False)

            output_lines = []
            error_lines = []

            # 实时读取输出
            while True:
                if stdout.channel.exit_status_ready():
                    # 读取剩余内容
                    remaining = stdout.read().decode("utf-8", errors="replace")
                    if remaining:
                        for line in remaining.splitlines():
                            if line.strip():
                                output_lines.append(line)
                                if output_callback:
                                    output_callback(line)
                    break

                line = stdout.readline()
                if line:
                    line = line.rstrip()
                    output_lines.append(line)
                    if output_callback:
                        output_callback(line)
                else:
                    time.sleep(0.05)

            exit_code = stdout.channel.recv_exit_status()
            err_output = stderr.read().decode("utf-8", errors="replace")
            if err_output:
                error_lines = err_output.splitlines()

            return exit_code, "\n".join(output_lines), "\n".join(error_lines)

        except Exception as e:
            self.log(f"命令执行异常: {e}", "ERROR")
            return -1, "", str(e)

    def exec_script(
        self,
        remote_script_path: str,
        args: str = "",
        output_callback: Optional[Callable] = None,
        timeout: int = 3600,
    ) -> bool:
        """执行远程 Shell 脚本"""
        cmd = f"bash {remote_script_path} {args}"
        exit_code, stdout, stderr = self.exec_command(
            cmd, timeout=timeout, output_callback=output_callback
        )
        if exit_code != 0:
            self.log(f"❌ 脚本执行失败 (exit={exit_code}): {stderr[:200]}", "ERROR")
            return False
        return True

    # ------------------------------------------------------------------ #
    # 文件上传（SFTP）
    # ------------------------------------------------------------------ #

    def mkdir_remote(self, remote_path: str):
        """递归创建远程目录"""
        parts = remote_path.split("/")
        current = ""
        for part in parts:
            if not part:
                continue
            current = current + "/" + part
            try:
                self._sftp.stat(current)
            except FileNotFoundError:
                try:
                    self._sftp.mkdir(current)
                except Exception:
                    pass

    def upload_file(
        self,
        local_path: str,
        remote_path: str,
        progress_callback: Optional[Callable] = None,
    ) -> bool:
        """
        上传单个文件
        :param progress_callback: callback(transferred_bytes, total_bytes)
        """
        if not self._connected:
            return False

        local_path = str(local_path)
        file_size = os.path.getsize(local_path)
        filename = os.path.basename(local_path)
        self.log(
            f"上传: {filename} ({self._human_size(file_size)}) → {remote_path}", "INFO"
        )

        try:
            # 确保远程目录存在
            remote_dir = os.path.dirname(remote_path)
            self.mkdir_remote(remote_dir)

            def _progress(transferred, total):
                pct = int(transferred / total * 100) if total > 0 else 0
                if progress_callback:
                    progress_callback(transferred, total, pct)

            self._sftp.put(local_path, remote_path, callback=_progress)
            # 设置可执行权限
            if local_path.endswith(".sh"):
                self._sftp.chmod(remote_path, stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP)
            self.log(f"✅ 上传完成: {filename}", "SUCCESS")
            return True

        except Exception as e:
            self.log(f"❌ 上传失败 {filename}: {e}", "ERROR")
            return False

    def upload_directory(
        self,
        local_dir: str,
        remote_dir: str,
        progress_callback: Optional[Callable] = None,
        file_filter: Optional[Callable] = None,
    ) -> bool:
        """
        上传整个目录
        :param file_filter: 可选，返回 True 则上传该文件
        """
        local_dir = Path(local_dir)
        files = list(local_dir.rglob("*"))
        files = [f for f in files if f.is_file()]
        if file_filter:
            files = [f for f in files if file_filter(f)]

        total = len(files)
        self.log(f"准备上传 {total} 个文件到 {remote_dir}", "INFO")

        for idx, local_file in enumerate(files, 1):
            rel = local_file.relative_to(local_dir)
            remote_file = f"{remote_dir}/{rel}".replace("\\", "/")
            ok = self.upload_file(str(local_file), remote_file)
            if not ok:
                return False
            if progress_callback:
                progress_callback(idx, total)

        return True

    # ------------------------------------------------------------------ #
    # 工具方法
    # ------------------------------------------------------------------ #

    @staticmethod
    def _human_size(size: int) -> str:
        for unit in ["B", "KB", "MB", "GB"]:
            if size < 1024:
                return f"{size:.1f} {unit}"
            size /= 1024
        return f"{size:.1f} TB"
