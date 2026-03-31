# -*- coding: utf-8 -*-
"""
GUI 主界面 - 基于 tkinter 的安装工具界面
包含：服务器管理、组件选择、进度监控、日志输出
"""

import queue
import threading
import tkinter as tk
import tkinter.font as tkfont
from tkinter import filedialog, messagebox, scrolledtext, ttk
from typing import Optional

from config import COMPONENTS, config_manager
from installer import InstallerEngine, TaskStatus
from ssh_client import SSHClient

# ═══════════════════════════════════════════
# 颜色 & 样式常量
# ═══════════════════════════════════════════
BG_DARK = "#1e1e2e"
BG_PANEL = "#2a2a3e"
BG_CARD = "#313145"
FG_TEXT = "#cdd6f4"
FG_MUTED = "#6c7086"
FG_TITLE = "#89b4fa"
FG_SUCCESS = "#a6e3a1"
FG_ERROR = "#f38ba8"
FG_WARN = "#fab387"
FG_CMD = "#89dceb"
ACCENT = "#7287fd"
ACCENT_HOVER = "#8899ff"

LOG_COLORS = {
    "INFO": FG_TEXT,
    "SUCCESS": FG_SUCCESS,
    "ERROR": FG_ERROR,
    "WARN": FG_WARN,
    "CMD": FG_CMD,
    "OUTPUT": "#bac2de",
}


class ServerDialog(tk.Toplevel):
    """服务器配置对话框"""

    def __init__(self, parent, server: dict = None):
        super().__init__(parent)
        self.result = None
        self.server = server or {}
        self.title("添加 / 编辑服务器")
        self.resizable(False, False)
        self.configure(bg=BG_DARK)
        self.grab_set()
        self._build()
        self._center(parent)

    def _build(self):
        pad = {"padx": 12, "pady": 6}
        frm = tk.Frame(self, bg=BG_DARK)
        frm.pack(fill="both", expand=True, padx=20, pady=16)

        fields = [
            ("名称", "name", False),
            ("主机 / IP", "host", False),
            ("端口", "port", False),
            ("用户名", "username", False),
            ("密码", "password", True),
        ]
        self._vars = {}

        for row, (label, key, is_pw) in enumerate(fields):
            tk.Label(frm, text=label, bg=BG_DARK, fg=FG_TEXT, anchor="e", width=10).grid(
                row=row, column=0, **pad, sticky="e"
            )
            var = tk.StringVar(value=self.server.get(key, "22" if key == "port" else ""))
            self._vars[key] = var
            entry = tk.Entry(
                frm,
                textvariable=var,
                show="*" if is_pw else "",
                width=28,
                bg=BG_PANEL,
                fg=FG_TEXT,
                insertbackground=FG_TEXT,
                relief="flat",
                bd=4,
            )
            entry.grid(row=row, column=1, **pad, sticky="ew")

        # SSH 密钥文件
        tk.Label(frm, text="SSH 密钥", bg=BG_DARK, fg=FG_TEXT, anchor="e", width=10).grid(
            row=len(fields), column=0, **pad, sticky="e"
        )
        key_frm = tk.Frame(frm, bg=BG_DARK)
        key_frm.grid(row=len(fields), column=1, **pad, sticky="ew")
        self._vars["key_file"] = tk.StringVar(value=self.server.get("key_file", ""))
        tk.Entry(
            key_frm,
            textvariable=self._vars["key_file"],
            width=20,
            bg=BG_PANEL,
            fg=FG_TEXT,
            insertbackground=FG_TEXT,
            relief="flat",
            bd=4,
        ).pack(side="left", fill="x", expand=True)
        tk.Button(
            key_frm,
            text="浏览",
            bg=BG_CARD,
            fg=FG_TEXT,
            relief="flat",
            bd=0,
            padx=6,
            command=self._browse_key,
        ).pack(side="left", padx=(4, 0))

        # 备注
        tk.Label(frm, text="备注", bg=BG_DARK, fg=FG_TEXT, anchor="e", width=10).grid(
            row=len(fields) + 1, column=0, **pad, sticky="e"
        )
        self._vars["note"] = tk.StringVar(value=self.server.get("note", ""))
        tk.Entry(
            frm,
            textvariable=self._vars["note"],
            width=28,
            bg=BG_PANEL,
            fg=FG_TEXT,
            insertbackground=FG_TEXT,
            relief="flat",
            bd=4,
        ).grid(row=len(fields) + 1, column=1, **pad, sticky="ew")

        # 按钮
        btn_frm = tk.Frame(self, bg=BG_DARK)
        btn_frm.pack(fill="x", padx=20, pady=(0, 16))
        tk.Button(
            btn_frm, text="取消", bg=BG_CARD, fg=FG_TEXT,
            relief="flat", bd=0, padx=16, pady=6, command=self.destroy
        ).pack(side="right", padx=(6, 0))
        tk.Button(
            btn_frm, text="保存", bg=ACCENT, fg="white",
            relief="flat", bd=0, padx=16, pady=6, command=self._save
        ).pack(side="right")

    def _browse_key(self):
        path = filedialog.askopenfilename(
            title="选择 SSH 私钥文件",
            filetypes=[("PEM 文件", "*.pem"), ("所有文件", "*.*")],
        )
        if path:
            self._vars["key_file"].set(path)

    def _save(self):
        name = self._vars["name"].get().strip()
        host = self._vars["host"].get().strip()
        if not name or not host:
            messagebox.showwarning("提示", "名称和主机地址不能为空", parent=self)
            return
        self.result = {k: v.get().strip() for k, v in self._vars.items()}
        self.destroy()

    def _center(self, parent):
        self.update_idletasks()
        x = parent.winfo_x() + (parent.winfo_width() - self.winfo_width()) // 2
        y = parent.winfo_y() + (parent.winfo_height() - self.winfo_height()) // 2
        self.geometry(f"+{x}+{y}")


class InstallerApp(tk.Tk):
    """主应用窗口"""

    def __init__(self):
        super().__init__()
        self.title("OpenCloud 自动化安装工具  v1.0")
        self.geometry("1100x760")
        self.minsize(900, 640)
        self.configure(bg=BG_DARK)
        self._engine: Optional[InstallerEngine] = None
        self._log_queue: queue.Queue = queue.Queue()
        self._status_queue: queue.Queue = queue.Queue()
        self._component_vars: dict = {}
        self._task_rows: dict = {}

        self._setup_styles()
        self._build_ui()
        self._refresh_server_list()
        self._poll_queues()

    # ═══════════════════════════════════════════
    # 样式初始化
    # ═══════════════════════════════════════════

    def _setup_styles(self):
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TFrame", background=BG_DARK)
        style.configure("TLabel", background=BG_DARK, foreground=FG_TEXT)
        style.configure("TNotebook", background=BG_DARK, borderwidth=0)
        style.configure(
            "TNotebook.Tab",
            background=BG_PANEL,
            foreground=FG_MUTED,
            padding=[14, 6],
            borderwidth=0,
        )
        style.map(
            "TNotebook.Tab",
            background=[("selected", BG_CARD)],
            foreground=[("selected", FG_TITLE)],
        )
        style.configure(
            "Horizontal.TProgressbar",
            troughcolor=BG_PANEL,
            background=ACCENT,
            borderwidth=0,
            thickness=8,
        )
        style.configure(
            "Task.TProgressbar",
            troughcolor=BG_PANEL,
            background=ACCENT,
            borderwidth=0,
            thickness=6,
        )
        style.configure("Treeview", background=BG_PANEL, fieldbackground=BG_PANEL,
                        foreground=FG_TEXT, borderwidth=0, rowheight=28)
        style.configure("Treeview.Heading", background=BG_CARD, foreground=FG_TITLE,
                        relief="flat", borderwidth=0)
        style.map("Treeview", background=[("selected", ACCENT)])

    # ═══════════════════════════════════════════
    # UI 构建
    # ═══════════════════════════════════════════

    def _build_ui(self):
        # 顶部标题栏
        header = tk.Frame(self, bg=BG_PANEL, height=56)
        header.pack(fill="x")
        header.pack_propagate(False)
        tk.Label(
            header,
            text="  🚀  OpenCloud 自动化安装工具",
            bg=BG_PANEL,
            fg=FG_TITLE,
            font=("Microsoft YaHei", 16, "bold"),
            anchor="w",
        ).pack(side="left", padx=16)
        tk.Label(
            header,
            text="Ceph · OpenStack · Kubernetes · Prometheus · Grafana",
            bg=BG_PANEL,
            fg=FG_MUTED,
            font=("Microsoft YaHei", 10),
        ).pack(side="right", padx=20)

        # 主体：左侧配置 + 右侧日志
        body = tk.Frame(self, bg=BG_DARK)
        body.pack(fill="both", expand=True, padx=0, pady=0)

        # 左侧面板（配置区）
        left = tk.Frame(body, bg=BG_DARK, width=420)
        left.pack(side="left", fill="y", padx=0)
        left.pack_propagate(False)

        notebook = ttk.Notebook(left)
        notebook.pack(fill="both", expand=True, padx=8, pady=8)

        # Tab1：服务器配置
        tab_server = tk.Frame(notebook, bg=BG_DARK)
        notebook.add(tab_server, text="  🖥  服务器  ")
        self._build_server_tab(tab_server)

        # Tab2：安装组件
        tab_comp = tk.Frame(notebook, bg=BG_DARK)
        notebook.add(tab_comp, text="  📦  组件  ")
        self._build_component_tab(tab_comp)

        # 安装按钮区
        self._build_action_bar(left)

        # 右侧面板（进度+日志）
        right = tk.Frame(body, bg=BG_DARK)
        right.pack(side="left", fill="both", expand=True, padx=0)
        self._build_right_panel(right)

    def _build_server_tab(self, parent):
        """服务器列表和管理"""
        # Treeview
        cols = ("名称", "主机", "端口", "用户", "备注")
        self._server_tree = ttk.Treeview(parent, columns=cols, show="headings", height=8)
        col_widths = [80, 130, 55, 70, 80]
        for col, w in zip(cols, col_widths):
            self._server_tree.heading(col, text=col)
            self._server_tree.column(col, width=w, anchor="w")
        self._server_tree.pack(fill="both", expand=True, padx=8, pady=(8, 0))
        self._server_tree.bind("<<TreeviewSelect>>", self._on_server_select)
        self._server_tree.bind("<Double-1>", lambda e: self._edit_server())

        # 按钮行
        btn_row = tk.Frame(parent, bg=BG_DARK)
        btn_row.pack(fill="x", padx=8, pady=6)
        for text, cmd in [("➕ 添加", self._add_server), ("✏️ 编辑", self._edit_server),
                           ("🗑 删除", self._del_server), ("🔍 测试连接", self._test_connection)]:
            tk.Button(
                btn_row, text=text, bg=BG_CARD, fg=FG_TEXT, relief="flat",
                bd=0, padx=8, pady=4, command=cmd, cursor="hand2"
            ).pack(side="left", padx=(0, 4))

        # 选中服务器状态
        self._sel_server_var = tk.StringVar(value="未选择服务器")
        tk.Label(parent, textvariable=self._sel_server_var,
                 bg=BG_DARK, fg=FG_MUTED, font=("Microsoft YaHei", 9)).pack(padx=8, pady=(0, 4))

    def _build_component_tab(self, parent):
        """组件选择区"""
        tk.Label(
            parent, text="选择要安装的组件（依赖关系将自动处理）",
            bg=BG_DARK, fg=FG_MUTED, font=("Microsoft YaHei", 9)
        ).pack(padx=10, pady=(10, 4), anchor="w")

        self._comp_frame = tk.Frame(parent, bg=BG_DARK)
        self._comp_frame.pack(fill="both", expand=True, padx=8, pady=4)

        # 按 order 排序
        ordered = sorted(COMPONENTS.items(), key=lambda x: x[1]["order"])
        for key, info in ordered:
            card = tk.Frame(self._comp_frame, bg=BG_CARD, bd=0, relief="flat")
            card.pack(fill="x", pady=3)

            var = tk.BooleanVar(value=config_manager.selected_components.get(key, True))
            self._component_vars[key] = var

            # 左侧图标+名称
            left = tk.Frame(card, bg=BG_CARD)
            left.pack(side="left", fill="y", padx=(10, 0))
            tk.Label(left, text=info["icon"], bg=BG_CARD, fg=FG_TEXT,
                     font=("Segoe UI Emoji", 18)).pack(pady=8)

            # 中间文字
            mid = tk.Frame(card, bg=BG_CARD)
            mid.pack(side="left", fill="both", expand=True, padx=8, pady=6)
            tk.Label(mid, text=info["name"], bg=BG_CARD, fg=FG_TEXT,
                     font=("Microsoft YaHei", 11, "bold"), anchor="w").pack(anchor="w")
            tk.Label(mid, text=info["description"], bg=BG_CARD, fg=FG_MUTED,
                     font=("Microsoft YaHei", 9), wraplength=240, justify="left",
                     anchor="w").pack(anchor="w")
            if info.get("requires"):
                deps = "依赖: " + ", ".join(info["requires"])
                tk.Label(mid, text=deps, bg=BG_CARD, fg=FG_WARN,
                         font=("Microsoft YaHei", 8), anchor="w").pack(anchor="w")

            # 右侧开关
            cb = tk.Checkbutton(
                card, variable=var, bg=BG_CARD, fg=FG_TEXT,
                selectcolor=ACCENT, activebackground=BG_CARD,
                relief="flat", bd=0, cursor="hand2",
                command=lambda k=key, v=var: self._on_component_toggle(k, v),
            )
            cb.pack(side="right", padx=12)

    def _build_action_bar(self, parent):
        """底部操作按钮"""
        sep = tk.Frame(parent, bg=BG_PANEL, height=1)
        sep.pack(fill="x", padx=8, pady=(4, 0))

        bar = tk.Frame(parent, bg=BG_DARK)
        bar.pack(fill="x", padx=8, pady=8)

        self._install_btn = tk.Button(
            bar,
            text="▶  开始安装",
            bg=FG_SUCCESS,
            fg="#1e1e2e",
            font=("Microsoft YaHei", 11, "bold"),
            relief="flat",
            bd=0,
            padx=20,
            pady=8,
            command=self._start_install,
            cursor="hand2",
        )
        self._install_btn.pack(side="left", fill="x", expand=True)

        self._stop_btn = tk.Button(
            bar,
            text="⏹  停止",
            bg=FG_ERROR,
            fg="white",
            font=("Microsoft YaHei", 10),
            relief="flat",
            bd=0,
            padx=12,
            pady=8,
            command=self._stop_install,
            cursor="hand2",
            state="disabled",
        )
        self._stop_btn.pack(side="left", padx=(6, 0))

    def _build_right_panel(self, parent):
        """右侧进度+日志区域"""
        # 任务进度看板
        progress_lbl = tk.Label(
            parent, text="安装进度", bg=BG_DARK, fg=FG_TITLE,
            font=("Microsoft YaHei", 11, "bold"), anchor="w"
        )
        progress_lbl.pack(fill="x", padx=12, pady=(10, 4))

        self._tasks_frame = tk.Frame(parent, bg=BG_DARK)
        self._tasks_frame.pack(fill="x", padx=12)
        self._build_task_rows()

        # 日志区
        log_header = tk.Frame(parent, bg=BG_DARK)
        log_header.pack(fill="x", padx=12, pady=(10, 2))
        tk.Label(log_header, text="安装日志", bg=BG_DARK, fg=FG_TITLE,
                 font=("Microsoft YaHei", 11, "bold")).pack(side="left")
        tk.Button(log_header, text="清空", bg=BG_CARD, fg=FG_MUTED,
                  relief="flat", bd=0, padx=8, pady=2,
                  command=self._clear_log, cursor="hand2").pack(side="right")

        self._log_text = scrolledtext.ScrolledText(
            parent,
            bg=BG_PANEL,
            fg=FG_TEXT,
            font=("Consolas", 10),
            relief="flat",
            bd=0,
            wrap="word",
            state="disabled",
        )
        self._log_text.pack(fill="both", expand=True, padx=12, pady=(0, 8))

        # 为每个日志级别配置颜色 tag
        for level, color in LOG_COLORS.items():
            self._log_text.tag_configure(level, foreground=color)

    def _build_task_rows(self):
        """为每个组件创建一行进度条"""
        ordered = sorted(COMPONENTS.items(), key=lambda x: x[1]["order"])
        for key, info in ordered:
            row = tk.Frame(self._tasks_frame, bg=BG_DARK)
            row.pack(fill="x", pady=2)

            icon_lbl = tk.Label(row, text=info["icon"], bg=BG_DARK, fg=FG_TEXT,
                                font=("Segoe UI Emoji", 12), width=3)
            icon_lbl.pack(side="left")

            name_lbl = tk.Label(row, text=info["name"], bg=BG_DARK, fg=FG_TEXT,
                                font=("Microsoft YaHei", 9), width=16, anchor="w")
            name_lbl.pack(side="left")

            bar = ttk.Progressbar(row, style="Task.TProgressbar",
                                  mode="indeterminate", length=160)
            bar.pack(side="left", padx=6)

            status_lbl = tk.Label(row, text="等待中", bg=BG_DARK, fg=FG_MUTED,
                                  font=("Microsoft YaHei", 9), width=10, anchor="w")
            status_lbl.pack(side="left", padx=4)

            self._task_rows[key] = {
                "bar": bar,
                "status": status_lbl,
            }

    # ═══════════════════════════════════════════
    # 服务器管理
    # ═══════════════════════════════════════════

    def _refresh_server_list(self):
        self._server_tree.delete(*self._server_tree.get_children())
        for s in config_manager.servers:
            self._server_tree.insert(
                "", "end",
                iid=s["name"],
                values=(s["name"], s["host"], s.get("port", 22),
                        s.get("username", ""), s.get("note", ""))
            )

    def _on_server_select(self, event=None):
        sel = self._server_tree.selection()
        if sel:
            self._sel_server_var.set(f"已选择: {sel[0]}")
        else:
            self._sel_server_var.set("未选择服务器")

    def _get_selected_server_name(self) -> Optional[str]:
        sel = self._server_tree.selection()
        return sel[0] if sel else None

    def _add_server(self):
        dlg = ServerDialog(self)
        self.wait_window(dlg)
        if dlg.result:
            config_manager.add_server(dlg.result)
            self._refresh_server_list()

    def _edit_server(self):
        name = self._get_selected_server_name()
        if not name:
            messagebox.showinfo("提示", "请先选择一个服务器", parent=self)
            return
        server = config_manager.get_server(name)
        dlg = ServerDialog(self, server)
        self.wait_window(dlg)
        if dlg.result:
            config_manager.add_server(dlg.result)
            self._refresh_server_list()

    def _del_server(self):
        name = self._get_selected_server_name()
        if not name:
            messagebox.showinfo("提示", "请先选择一个服务器", parent=self)
            return
        if messagebox.askyesno("确认", f"确定要删除服务器 [{name}] 吗？", parent=self):
            config_manager.remove_server(name)
            self._refresh_server_list()
            self._sel_server_var.set("未选择服务器")

    def _test_connection(self):
        name = self._get_selected_server_name()
        if not name:
            messagebox.showinfo("提示", "请先选择一个服务器", parent=self)
            return
        server = config_manager.get_server(name)
        self._append_log(f"正在测试连接: {server['host']} ...", "INFO")

        def do_test():
            client = SSHClient(server, log_callback=lambda msg, lvl="INFO": self._log_queue.put((msg, lvl)))
            ok = client.connect()
            if ok:
                self._log_queue.put((f"✅ 服务器 {server['host']} 连接成功！", "SUCCESS"))
                client.disconnect()
            else:
                self._log_queue.put((f"❌ 服务器 {server['host']} 连接失败", "ERROR"))

        threading.Thread(target=do_test, daemon=True).start()

    # ═══════════════════════════════════════════
    # 组件选择
    # ═══════════════════════════════════════════

    def _on_component_toggle(self, key: str, var: tk.BooleanVar):
        config_manager.set_component(key, var.get())
        # 自动处理依赖
        if not var.get():
            # 关闭依赖此组件的其它组件
            for k, info in COMPONENTS.items():
                if key in info.get("requires", []) and self._component_vars.get(k):
                    self._component_vars[k].set(False)
                    config_manager.set_component(k, False)
        else:
            # 开启时，自动开启依赖
            for dep in COMPONENTS[key].get("requires", []):
                if self._component_vars.get(dep):
                    self._component_vars[dep].set(True)
                    config_manager.set_component(dep, True)

    def _get_selected_components(self):
        """按 order 返回已选中的组件 key 列表"""
        ordered = sorted(COMPONENTS.items(), key=lambda x: x[1]["order"])
        return [k for k, _ in ordered if self._component_vars.get(k, tk.BooleanVar()).get()]

    # ═══════════════════════════════════════════
    # 安装控制
    # ═══════════════════════════════════════════

    def _start_install(self):
        # 验证服务器
        name = self._get_selected_server_name()
        if not name:
            messagebox.showwarning("提示", "请先在【服务器】标签页选择目标服务器", parent=self)
            return
        server = config_manager.get_server(name)

        # 验证组件
        selected = self._get_selected_components()
        if not selected:
            messagebox.showwarning("提示", "请至少选择一个要安装的组件", parent=self)
            return

        # 确认
        comp_names = "\n".join(f"  • {COMPONENTS[k]['name']}" for k in selected)
        msg = f"即将在服务器 {server['host']} 上安装以下组件：\n\n{comp_names}\n\n确认开始安装？"
        if not messagebox.askyesno("确认安装", msg, parent=self):
            return

        # 重置任务状态
        self._reset_task_ui(selected)
        self._set_installing(True)
        self._clear_log()

        # 启动引擎
        self._engine = InstallerEngine(
            server=server,
            selected_components=selected,
            log_callback=lambda msg, lvl="INFO": self._log_queue.put((msg, lvl)),
            progress_callback=self._on_progress,
            status_callback=lambda key, status: self._status_queue.put((key, status)),
            finished_callback=self._on_finished,
        )
        self._engine.start()

    def _stop_install(self):
        if self._engine and self._engine.is_running:
            self._engine.stop()
            self._stop_btn.config(state="disabled")

    def _set_installing(self, installing: bool):
        self._install_btn.config(state="disabled" if installing else "normal")
        self._stop_btn.config(state="normal" if installing else "disabled")

    def _on_progress(self, key, transferred, total, pct):
        """SFTP 上传进度（由引擎线程回调）"""
        pass  # 进度条使用 indeterminate 模式，此处可扩展

    def _on_finished(self, success: bool, summary: str):
        """安装完成回调（引擎线程调用）"""
        self._log_queue.put(("═" * 60, "INFO"))
        level = "SUCCESS" if success else "ERROR"
        self._log_queue.put((summary, level))
        # 通过 status_queue 通知 GUI 恢复按钮状态
        self._status_queue.put(("__finished__", success))

    # ═══════════════════════════════════════════
    # 任务状态 UI 更新
    # ═══════════════════════════════════════════

    def _reset_task_ui(self, selected):
        for key, row in self._task_rows.items():
            row["bar"].stop()
            row["bar"].config(mode="indeterminate", value=0)
            if key in selected:
                row["status"].config(text="等待中", fg=FG_MUTED)
            else:
                row["status"].config(text="跳过", fg=FG_MUTED)

    def _update_task_status(self, key: str, status: TaskStatus):
        row = self._task_rows.get(key)
        if not row:
            return
        bar = row["bar"]
        lbl = row["status"]

        if status == TaskStatus.RUNNING:
            bar.config(mode="indeterminate")
            bar.start(12)
            lbl.config(text="安装中...", fg=FG_WARN)
        elif status == TaskStatus.SUCCESS:
            bar.stop()
            bar.config(mode="determinate", value=100)
            lbl.config(text="✅ 完成", fg=FG_SUCCESS)
        elif status == TaskStatus.FAILED:
            bar.stop()
            bar.config(mode="determinate", value=0)
            lbl.config(text="❌ 失败", fg=FG_ERROR)
        elif status == TaskStatus.SKIPPED:
            bar.stop()
            bar.config(mode="determinate", value=0)
            lbl.config(text="⏭ 跳过", fg=FG_MUTED)

    # ═══════════════════════════════════════════
    # 日志
    # ═══════════════════════════════════════════

    def _append_log(self, message: str, level: str = "INFO"):
        import datetime
        ts = datetime.datetime.now().strftime("%H:%M:%S")
        self._log_text.config(state="normal")
        self._log_text.insert("end", f"[{ts}] ", "INFO")
        self._log_text.insert("end", message + "\n", level)
        self._log_text.see("end")
        self._log_text.config(state="disabled")

    def _clear_log(self):
        self._log_text.config(state="normal")
        self._log_text.delete("1.0", "end")
        self._log_text.config(state="disabled")

    # ═══════════════════════════════════════════
    # 队列轮询（将子线程回调转到 GUI 主线程）
    # ═══════════════════════════════════════════

    def _poll_queues(self):
        # 处理日志队列
        try:
            while True:
                msg, level = self._log_queue.get_nowait()
                self._append_log(msg, level)
        except queue.Empty:
            pass

        # 处理状态队列
        try:
            while True:
                key, value = self._status_queue.get_nowait()
                if key == "__finished__":
                    self._set_installing(False)
                    if value:
                        messagebox.showinfo("安装完成", "🎉 所有组件安装成功！", parent=self)
                    else:
                        messagebox.showerror("安装失败", "部分组件安装失败，请查看日志", parent=self)
                else:
                    self._update_task_status(key, value)
        except queue.Empty:
            pass

        self.after(100, self._poll_queues)


def main():
    app = InstallerApp()
    app.mainloop()


if __name__ == "__main__":
    main()
