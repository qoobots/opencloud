/**
 * Electron 主进程
 * 负责：创建窗口、注册 IPC 处理器、管理应用生命周期
 */

const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron')
const path = require('path')
const fs = require('fs')
const SSHService = require('./ssh-service')
const ConfigStore = require('./config-store')

// ─── 全局状态 ──────────────────────────────────────────
let mainWindow = null
const configStore = new ConfigStore()
const sshService = new SSHService()
const isDev = process.argv.includes('--dev')

// ─── 创建主窗口 ────────────────────────────────────────
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 780,
    minWidth: 960,
    minHeight: 640,
    frame: false,           // 无边框，使用自定义标题栏
    backgroundColor: '#1a1b2e',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
    icon: path.join(__dirname, '../renderer/assets/icon.ico'),
  })

  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'))

  // 页面加载完成后显示，避免白屏
  mainWindow.once('ready-to-show', () => {
    mainWindow.show()
    if (isDev) mainWindow.webContents.openDevTools()
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

app.whenReady().then(createWindow)
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit() })
app.on('activate', () => { if (!mainWindow) createWindow() })

// ─── IPC：窗口控制 ────────────────────────────────────
ipcMain.on('window:minimize', () => mainWindow?.minimize())
ipcMain.on('window:maximize', () => {
  if (mainWindow?.isMaximized()) mainWindow.unmaximize()
  else mainWindow?.maximize()
})
ipcMain.on('window:close', () => mainWindow?.close())

// ─── IPC：配置管理 ────────────────────────────────────
ipcMain.handle('config:getServers', () => configStore.getServers())
ipcMain.handle('config:saveServer', (_, server) => configStore.saveServer(server))
ipcMain.handle('config:deleteServer', (_, id) => configStore.deleteServer(id))
ipcMain.handle('config:getComponents', () => configStore.getComponents())
ipcMain.handle('config:setComponents', (_, components) => configStore.setComponents(components))

// ─── IPC：选择文件（SSH 密钥） ────────────────────────
ipcMain.handle('dialog:openFile', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: '选择 SSH 私钥文件',
    filters: [
      { name: 'PEM 文件', extensions: ['pem'] },
      { name: '所有文件', extensions: ['*'] },
    ],
    properties: ['openFile'],
  })
  return result.canceled ? null : result.filePaths[0]
})

// ─── IPC：SSH 连接测试 ────────────────────────────────
ipcMain.handle('ssh:test', async (_, server) => {
  return sshService.testConnection(server)
})

// ─── IPC：安装前环境预检 ─────────────────────────────
ipcMain.handle('ssh:precheck', async (_, server) => {
  return sshService.precheck(server)
})

// ─── IPC：开始安装 ────────────────────────────────────
ipcMain.handle('install:start', async (event, { server, components, mode }) => {
  // 打包后 __dirname 为 resources/app/src/main/，scripts 在 resources/scripts/
  // 开发时 __dirname 为 src/main/，scripts 在 ../../scripts/
  const projectRoot = app.isPackaged
    ? process.resourcesPath
    : path.join(__dirname, '../../')
  const scriptsDir = path.join(projectRoot, 'scripts')
  const packagesDir = path.join(projectRoot, 'packages')

  sshService.startInstall({
    server,
    components,
    mode,      // 'all' = 一键全部安装
    scriptsDir,
    packagesDir,
    onLog: (msg, level) => {
      mainWindow?.webContents.send('install:log', { msg, level })
    },
    onStatus: (key, status) => {
      mainWindow?.webContents.send('install:status', { key, status })
    },
    onProgress: (key, pct) => {
      mainWindow?.webContents.send('install:progress', { key, pct })
    },
    onFinished: (success, summary) => {
      mainWindow?.webContents.send('install:finished', { success, summary })
    },
  })

  return { ok: true }
})

// ─── IPC：停止安装 ────────────────────────────────────
ipcMain.handle('install:stop', () => {
  sshService.stopInstall()
  return { ok: true }
})

// ─── IPC：打开日志目录 ────────────────────────────────
ipcMain.handle('logs:open', () => {
  // 打包后日志目录放到 userData 旁边，避免只读资源目录问题
  const logsDir = app.isPackaged
    ? path.join(app.getPath('userData'), 'logs')
    : path.join(__dirname, '../../logs')
  if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true })
  shell.openPath(logsDir)
})

// ─── IPC：获取平台信息 ────────────────────────────────
ipcMain.handle('app:version', () => app.getVersion())
