/**
 * 预加载脚本 - 在渲染进程中暴露安全的 API 桥接
 * contextIsolation=true，通过 contextBridge 暴露 window.api
 */

const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('api', {
  // ── 窗口控制 ──────────────────────────────────────
  window: {
    minimize: () => ipcRenderer.send('window:minimize'),
    maximize: () => ipcRenderer.send('window:maximize'),
    close:    () => ipcRenderer.send('window:close'),
  },

  // ── 配置管理 ──────────────────────────────────────
  config: {
    getServers:    () => ipcRenderer.invoke('config:getServers'),
    saveServer:    (server) => ipcRenderer.invoke('config:saveServer', server),
    deleteServer:  (id) => ipcRenderer.invoke('config:deleteServer', id),
    getComponents: () => ipcRenderer.invoke('config:getComponents'),
    setComponents: (c) => ipcRenderer.invoke('config:setComponents', c),
  },

  // ── 对话框 ────────────────────────────────────────
  dialog: {
    openFile: () => ipcRenderer.invoke('dialog:openFile'),
  },

  // ── SSH ───────────────────────────────────────────
  ssh: {
    test:     (server) => ipcRenderer.invoke('ssh:test', server),
    precheck: (server) => ipcRenderer.invoke('ssh:precheck', server),
  },

  // ── 安装控制 ──────────────────────────────────────
  install: {
    start:  (opts) => ipcRenderer.invoke('install:start', opts),
    stop:   () => ipcRenderer.invoke('install:stop'),

    // 监听事件（渲染进程调用）
    onLog:      (cb) => ipcRenderer.on('install:log',      (_, d) => cb(d)),
    onStatus:   (cb) => ipcRenderer.on('install:status',   (_, d) => cb(d)),
    onProgress: (cb) => ipcRenderer.on('install:progress', (_, d) => cb(d)),
    onFinished: (cb) => ipcRenderer.on('install:finished', (_, d) => cb(d)),

    // 移除监听
    removeAllListeners: () => {
      ['install:log','install:status','install:progress','install:finished']
        .forEach(ch => ipcRenderer.removeAllListeners(ch))
    },
  },

  // ── 日志 & App ────────────────────────────────────
  logs: {
    open: () => ipcRenderer.invoke('logs:open'),
  },
  app: {
    version: () => ipcRenderer.invoke('app:version'),
  },
})
