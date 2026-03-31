/**
 * SSH/SFTP 服务模块 - 封装 ssh2 库，提供：
 * - 连接测试
 * - 远程命令执行（实时输出）
 * - SFTP 文件上传
 * - 安装任务编排
 */

const { Client } = require('ssh2')
const fs = require('fs')
const path = require('path')
const { app } = require('electron')

// 组件定义（顺序 = 安装顺序）
const COMPONENTS = [
  {
    key: 'ceph',
    name: 'Ceph 分布式存储',
    icon: '🗄️',
    script: 'deploy_ceph.sh',
    requires: [],
    order: 1,
  },
  {
    key: 'openstack',
    name: 'OpenStack 云平台',
    icon: '☁️',
    script: 'deploy_openstack.sh',
    requires: ['ceph'],
    order: 2,
  },
  {
    key: 'kubernetes',
    name: 'Kubernetes 容器编排',
    icon: '⎈',
    script: 'deploy_kubernetes.sh',
    requires: ['ceph'],
    order: 3,
  },
  {
    key: 'prometheus',
    name: 'Prometheus 监控系统',
    icon: '📊',
    script: 'deploy_prometheus.sh',
    requires: ['kubernetes'],
    order: 4,
  },
  {
    key: 'grafana',
    name: 'Grafana 可视化面板',
    icon: '📈',
    script: 'deploy_grafana.sh',
    requires: ['prometheus'],
    order: 5,
  },
]

const REMOTE_WORK_DIR = '/opt/opencloud_installer'

class SSHService {
  constructor() {
    this._conn = null
    this._stopped = false
    this._logFile = null
    this._taskStatus = {}  // 记录一键安装模式下各组件当前状态
    this._activeStream = null  // 当前执行的 SSH stream 引用，用于强制停止
  }

  // ────────────────────────────────────────────────
  // 安装前环境预检
  // ────────────────────────────────────────────────
  async precheck(server) {
    let conn
    try {
      conn = await this._connect(server, () => {})
    } catch (e) {
      return { ok: false, items: [{ label: 'SSH 连接', status: 'fail', detail: e.message }] }
    }

    const items = []

    // 用一条复合命令一次性拿所有信息，减少 RTT
    const checkCmd = [
      'echo "USER:$(id -u)"',
      'echo "OS:$(cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d \'\\"\' )"',
      'echo "KERNEL:$(uname -r)"',
      'echo "MEM_KB:$(grep MemTotal /proc/meminfo | awk \'{print $2}\')"',
      'echo "DISK_KB:$(df / | tail -1 | awk \'{print $4}\')"',
      'echo "CPU_CORES:$(nproc)"',
      'echo "BASH:$(bash --version | head -1)"',
      'echo "NET:$(curl -s --max-time 5 -o /dev/null -w \'%{http_code}\' https://www.baidu.com || echo FAIL)"',
    ].join(' && ')

    const [, out] = await this._exec(conn, checkCmd, null, false, 20000)
    conn.end()

    const get = (key) => {
      const m = out.match(new RegExp(`${key}:(.+)`))
      return m ? m[1].trim() : ''
    }

    // 1. SSH / root 权限
    const uid = get('USER')
    items.push({
      label: 'SSH 连接',
      status: 'pass',
      detail: `连接成功 (${server.host}:${server.port || 22})`,
    })
    items.push({
      label: '用户权限',
      status: uid === '0' ? 'pass' : 'warn',
      detail: uid === '0' ? 'root 用户 ✓' : `当前 UID=${uid}，非 root，某些步骤可能失败`,
    })

    // 2. 操作系统
    const os = get('OS')
    const kernel = get('KERNEL')
    const osOk = /ubuntu (20|22|24)|centos (8|9)|rocky|alma|debian (11|12)/i.test(os)
    items.push({
      label: '操作系统',
      status: osOk ? 'pass' : 'warn',
      detail: `${os || '未知'}  内核 ${kernel}`,
    })

    // 3. 内存
    const memKb = parseInt(get('MEM_KB')) || 0
    const memGb = (memKb / 1024 / 1024).toFixed(1)
    items.push({
      label: '内存',
      status: memKb >= 15 * 1024 * 1024 ? 'pass' : 'fail',
      detail: `${memGb} GB${memKb < 15 * 1024 * 1024 ? '  ⚠️ 低于推荐值 16 GB' : ''}`,
    })

    // 4. 磁盘（/ 可用空间）
    const diskKb = parseInt(get('DISK_KB')) || 0
    const diskGb = (diskKb / 1024 / 1024).toFixed(1)
    items.push({
      label: '磁盘 / 可用',
      status: diskKb >= 80 * 1024 * 1024 ? 'pass' : 'fail',
      detail: `${diskGb} GB 可用${diskKb < 80 * 1024 * 1024 ? '  ⚠️ 低于推荐值 100 GB' : ''}`,
    })

    // 5. CPU 核心数
    const cpuCores = parseInt(get('CPU_CORES')) || 0
    items.push({
      label: 'CPU 核心数',
      status: cpuCores >= 4 ? 'pass' : cpuCores >= 2 ? 'warn' : 'fail',
      detail: cpuCores > 0
        ? `${cpuCores} 核${cpuCores < 4 ? '  ⚠️ 低于推荐值 4 核' : ''}`
        : '无法获取 CPU 信息',
    })

    // 6. Bash 版本
    const bash = get('BASH')
    const bashVerMatch = bash.match(/version (\d+)\./)
    const bashMajor = bashVerMatch ? parseInt(bashVerMatch[1]) : 0
    items.push({
      label: 'Bash 版本',
      status: bashMajor >= 4 ? 'pass' : 'fail',
      detail: bash || '未知',
    })

    // 7. 外网连通性
    const netCode = get('NET')
    items.push({
      label: '外网连通性',
      status: netCode === '200' ? 'pass' : 'warn',
      detail: netCode === '200' ? '可访问外网 ✓' : '无法访问外网，请确保配置了离线镜像源',
    })

    const allPass = items.every(i => i.status !== 'fail')
    return { ok: allPass, items }
  }

  // ────────────────────────────────────────────────
  // 连接测试
  // ────────────────────────────────────────────────
  testConnection(server) {
    return new Promise((resolve) => {
      const conn = new Client()
      const timeout = setTimeout(() => {
        conn.destroy()
        resolve({ ok: false, msg: '连接超时（30s）' })
      }, 30000)

      conn.on('ready', () => {
        clearTimeout(timeout)
        conn.end()
        resolve({ ok: true, msg: '连接成功' })
      })

      conn.on('error', (err) => {
        clearTimeout(timeout)
        resolve({ ok: false, msg: err.message })
      })

      conn.connect(this._buildConnectConfig(server))
    })
  }

  // ────────────────────────────────────────────────
  // 安装入口
  // ────────────────────────────────────────────────
  async startInstall({ server, components, mode, scriptsDir, packagesDir, onLog, onStatus, onProgress, onFinished }) {
    this._stopped = false
    this._taskStatus = {}  // 重置组件状态记录
    this._initLogFile()

    const log = (msg, level = 'INFO') => {
      onLog(msg, level)
      this._writeLog(msg, level)
    }

    // 包装 onStatus，同步记录到 _taskStatus
    const trackStatus = (key, status) => {
      this._taskStatus[key] = status
      onStatus(key, status)
    }

    log('═'.repeat(60), 'INFO')
    log('🚀 OpenCloud 自动化安装工具启动', 'INFO')
    log(`目标服务器: ${server.host}:${server.port || 22}`, 'INFO')
    if (mode === 'all') {
      log('安装模式: 一键全部安装（deploy_all.sh）', 'INFO')
    } else {
      log(`安装组件: ${components.join(', ')}`, 'INFO')
    }
    log('═'.repeat(60), 'INFO')

    // 建立连接
    let conn
    try {
      conn = await this._connect(server, log)
    } catch (e) {
      log(`❌ 无法连接到服务器: ${e.message}`, 'ERROR')
      onFinished(false, '连接服务器失败，请检查配置')
      return
    }

    try {
      // 初始化远程目录
      await this._exec(conn, `mkdir -p ${REMOTE_WORK_DIR}/scripts ${REMOTE_WORK_DIR}/packages`, log)

      // 上传脚本
      log('📤 正在上传部署脚本...', 'INFO')
      const sftp = await this._getSftp(conn)

      if (mode === 'all') {
        // 一键模式：上传 deploy_all.sh 及所有组件脚本
        const allScripts = ['deploy_all.sh', ...COMPONENTS.map(c => c.script)]
        for (const scriptName of allScripts) {
          if (this._stopped) break
          const localScript = path.join(scriptsDir, scriptName)
          if (fs.existsSync(localScript)) {
            const remoteScript = `${REMOTE_WORK_DIR}/scripts/${scriptName}`
            await this._upload(sftp, localScript, remoteScript, null, log)
          } else {
            log(`⚠️ 脚本不存在: ${scriptName}，跳过上传`, 'WARN')
          }
        }
      } else {
        // 分组件模式：只上传选定的组件脚本
        for (const key of components) {
          if (this._stopped) break
          const comp = COMPONENTS.find(c => c.key === key)
          if (!comp) continue
          const localScript = path.join(scriptsDir, comp.script)
          if (fs.existsSync(localScript)) {
            const remoteScript = `${REMOTE_WORK_DIR}/scripts/${comp.script}`
            await this._upload(sftp, localScript, remoteScript,
              (pct) => onProgress(key, pct), log)
          } else {
            log(`⚠️ 脚本不存在: ${comp.script}，跳过上传`, 'WARN')
          }
        }
      }

      // 上传离线包（如有）
      if (fs.existsSync(packagesDir)) {
        const pkgFiles = fs.readdirSync(packagesDir)
        if (pkgFiles.length > 0) {
          log(`📦 上传 ${pkgFiles.length} 个安装包...`, 'INFO')
          for (const f of pkgFiles) {
            if (this._stopped) break
            await this._upload(
              sftp,
              path.join(packagesDir, f),
              `${REMOTE_WORK_DIR}/packages/${f}`,
              null, log
            )
          }
        }
      }
      sftp.end()

      // ── 一键全部安装模式 ──────────────────────────────
      if (mode === 'all') {
        const allScript = `${REMOTE_WORK_DIR}/scripts/deploy_all.sh`
        const [, checkOut] = await this._exec(
          conn, `test -f ${allScript} && echo EXISTS || echo MISSING`, log, false)
        if (!checkOut.includes('EXISTS')) {
          log('❌ deploy_all.sh 未找到，请检查 scripts/ 目录', 'ERROR')
          conn.end()
          onFinished(false, 'deploy_all.sh 未找到')
          return
        }

        // 通知 UI：所有组件 running
        COMPONENTS.forEach(c => trackStatus(c.key, 'running'))

        log('▶ 执行 deploy_all.sh --all --yes（非交互模式）', 'INFO')
        // 用 cd 进入脚本目录后执行，确保 BASH_SOURCE[0] / SCRIPT_DIR 计算正确
        // 传入 --yes 参数由脚本内部跳过 read -r 交互，比 echo y | 管道更可靠
        const scriptsRemoteDir = `${REMOTE_WORK_DIR}/scripts`
        const [exitCode, , execErr] = await this._exec(
          conn,
          `cd ${scriptsRemoteDir} && bash deploy_all.sh --all --yes`,
          (line) => {
            log(line, 'OUTPUT')
            // 根据输出猜测进度，更新对应组件状态
            this._inferStatusFromLog(line, trackStatus)
          },
          true,
          14400000 // 4h 超时
        )

        const allOk = exitCode === 0
        // 根据退出码收尾：只将仍处于 running 状态的组件标为最终结果
        // 已由 _inferStatusFromLog 推断为 success/skipped 的组件保持不变
        if (allOk) {
          // 全部成功：将 running 中的组件标为 success
          COMPONENTS.forEach(c => {
            if (this._taskStatus[c.key] === 'running') trackStatus(c.key, 'success')
          })
        } else {
          // 有失败：将 running 中的组件标为 failed，已 success/skipped 的保持原样
          COMPONENTS.forEach(c => {
            if (this._taskStatus[c.key] === 'running') trackStatus(c.key, 'failed')
          })
        }
        const summary = allOk ? '🎉 一键全部安装成功！' : '⚠️ 一键安装中有组件失败，请查看日志'
        log('═'.repeat(60), 'INFO')
        log(summary, allOk ? 'SUCCESS' : 'ERROR')
        conn.end()
        onFinished(allOk, summary)
        return
      }

      // ── 分组件安装模式 ────────────────────────────────
      const results = {}
      const orderedComponents = COMPONENTS
        .filter(c => components.includes(c.key))
        .sort((a, b) => a.order - b.order)

      for (const comp of orderedComponents) {
        if (this._stopped) {
          log('⚠️ 用户已停止安装', 'WARN')
          break
        }

        // 检查依赖
        const depFailed = comp.requires.find(dep => results[dep] === false)
        if (depFailed) {
          log(`⏭ 跳过 ${comp.name}（依赖 ${depFailed} 安装失败）`, 'WARN')
          trackStatus(comp.key, 'skipped')
          results[comp.key] = false
          continue
        }

        trackStatus(comp.key, 'running')
        log('─'.repeat(50), 'INFO')
        log(`▶ 开始安装: ${comp.name}`, 'INFO')

        const remoteScript = `${REMOTE_WORK_DIR}/scripts/${comp.script}`
        // 检查脚本是否存在
        const [, checkOut] = await this._exec(conn, `test -f ${remoteScript} && echo EXISTS || echo MISSING`, log, false)
        const scriptExists = checkOut.includes('EXISTS')

        let ok = false
        if (scriptExists) {
          // 使用 cd 方式执行，确保 BASH_SOURCE[0] → SCRIPT_DIR 能正确解析
          const remoteScriptsDir = `${REMOTE_WORK_DIR}/scripts`
          const [exitCode] = await this._exec(
            conn,
            `cd ${remoteScriptsDir} && bash ${comp.script} --all`,
            (line) => log(line, 'OUTPUT'),
            true,
            7200000 // 2h
          )
          ok = exitCode === 0
        } else {
          log(`⚠️ 未找到脚本 ${comp.script}，跳过`, 'WARN')
          trackStatus(comp.key, 'skipped')
          results[comp.key] = true
          continue
        }

        results[comp.key] = ok
        if (ok) {
          log(`✅ ${comp.name} 安装成功`, 'SUCCESS')
          trackStatus(comp.key, 'success')
        } else {
          log(`❌ ${comp.name} 安装失败`, 'ERROR')
          trackStatus(comp.key, 'failed')
        }
      }

      conn.end()
      const allOk = Object.values(results).every(Boolean)
      const summary = this._buildSummary(results, orderedComponents)
      log('═'.repeat(60), 'INFO')
      log(summary, allOk ? 'SUCCESS' : 'ERROR')
      onFinished(allOk, summary)

    } catch (e) {
      log(`❌ 安装异常: ${e.message}`, 'ERROR')
      conn.end()
      onFinished(false, `安装过程发生异常: ${e.message}`)
    }
  }

  // 根据日志行内容推断组件状态（用于一键安装模式）
  _inferStatusFromLog(line, onStatus) {
    const lower = line.toLowerCase()
    const compMap = [
      { keys: ['ceph'],       id: 'ceph'       },
      { keys: ['openstack'],  id: 'openstack'  },
      { keys: ['kubernetes'], id: 'kubernetes' },
      { keys: ['prometheus'], id: 'prometheus' },
      { keys: ['grafana'],    id: 'grafana'    },
    ]
    for (const { keys, id } of compMap) {
      if (keys.some(k => lower.includes(k))) {
        if (lower.includes('success') || lower.includes('成功') || lower.includes('[ok]')) {
          onStatus(id, 'success')
        } else if (lower.includes('fail') || lower.includes('失败') || lower.includes('[fail]') || lower.includes('error')) {
          onStatus(id, 'failed')
        } else if (lower.includes('skip') || lower.includes('跳过')) {
          onStatus(id, 'skipped')
        } else if (lower.includes('开始') || lower.includes('step') || lower.includes('deploying') || lower.includes('[step]')) {
          onStatus(id, 'running')
        }
        break
      }
    }
  }

  stopInstall() {
    this._stopped = true
    // 向当前执行的 stream 发送 SIGINT，真正终止远端 shell 进程
    if (this._activeStream) {
      try {
        this._activeStream.signal('INT')  // 发送 Ctrl+C
        // 给远端 500ms 响应信号后再关闭连接
        setTimeout(() => {
          try { this._activeStream?.destroy() } catch (_) {}
          try { this._conn?.end() } catch (_) {}
        }, 500)
      } catch (_) {
        try { this._conn?.end() } catch (_) {}
      }
    } else if (this._conn) {
      try { this._conn.end() } catch (_) {}
    }
  }

  // ────────────────────────────────────────────────
  // 内部工具方法
  // ────────────────────────────────────────────────
  _buildConnectConfig(server) {
    const cfg = {
      host: server.host,
      port: parseInt(server.port) || 22,
      username: server.username,
      readyTimeout: 30000,
    }
    if (server.keyFile && fs.existsSync(server.keyFile)) {
      cfg.privateKey = fs.readFileSync(server.keyFile)
      if (server.passphrase) cfg.passphrase = server.passphrase
    } else {
      cfg.password = server.password || ''
    }
    return cfg
  }

  _connect(server, log) {
    return new Promise((resolve, reject) => {
      const conn = new Client()
      conn.on('ready', () => {
        log(`✅ 已连接到 ${server.host}`, 'SUCCESS')
        this._conn = conn
        resolve(conn)
      })
      conn.on('error', reject)
      conn.connect(this._buildConnectConfig(server))
    })
  }

  _getSftp(conn) {
    return new Promise((resolve, reject) => {
      conn.sftp((err, sftp) => {
        if (err) return reject(err)
        resolve(sftp)
      })
    })
  }

  // 过滤 ANSI/VT100 转义码（颜色、光标控制等）
  _stripAnsi(str) {
    // 匹配 ESC[ ... m 颜色序列、ESC[ ... H/A/B/C/D 光标序列、单字符 ESC 序列等
    // eslint-disable-next-line no-control-regex
    return str.replace(/\x1B\[[0-9;]*[A-Za-z]/g, '')
              .replace(/\x1B[()][0-9A-Za-z]/g, '')
              .replace(/\x1B[^[]/g, '')
              .replace(/\r/g, '')       // 去掉 CR，避免行覆盖
  }

  _exec(conn, command, outputCallback, showCmd = true, timeout = 300000) {
    return new Promise((resolve) => {
      if (showCmd && typeof outputCallback === 'function') {
        outputCallback(`$ ${command.substring(0, 100)}${command.length > 100 ? '...' : ''}`)
      }

      const timer = setTimeout(() => {
        if (typeof outputCallback === 'function') outputCallback('⏰ 命令执行超时，已中止')
        resolve([-1, '', 'timeout'])
      }, timeout)

      conn.exec(command, { pty: true }, (err, stream) => {
        if (err) {
          clearTimeout(timer)
          return resolve([-1, '', err.message])
        }

        // 保存当前活跃 stream，供 stopInstall 发送 SIGINT
        this._activeStream = stream

        let stdout = ''
        stream.on('data', (data) => {
          const text = this._stripAnsi(data.toString('utf8'))
          stdout += text
          if (typeof outputCallback === 'function') {
            text.split('\n').filter(l => l.trim()).forEach(l => outputCallback(l))
          }
        })
        stream.stderr.on('data', (data) => {
          const text = this._stripAnsi(data.toString('utf8'))
          if (typeof outputCallback === 'function') {
            text.split('\n').filter(l => l.trim()).forEach(l => outputCallback(l))
          }
        })
        stream.on('close', (code) => {
          clearTimeout(timer)
          this._activeStream = null
          resolve([code, stdout, ''])
        })
      })
    })
  }

  _upload(sftp, localPath, remotePath, progressCb, log) {
    return new Promise((resolve, reject) => {
      const filename = path.basename(localPath)
      const totalSize = fs.statSync(localPath).size
      log(`  ↑ ${filename} (${this._humanSize(totalSize)})`, 'INFO')

      // 确保远程目录存在
      const remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'))
      sftp.mkdir(remoteDir, { mode: 0o755 }, () => {
        // 忽略目录已存在的错误
        const readStream = fs.createReadStream(localPath)
        const writeStream = sftp.createWriteStream(remotePath)

        let transferred = 0
        readStream.on('data', (chunk) => {
          transferred += chunk.length
          const pct = Math.round(transferred / totalSize * 100)
          if (progressCb) progressCb(pct)
        })

        writeStream.on('close', () => {
          // 设置可执行权限
          if (localPath.endsWith('.sh')) {
            sftp.chmod(remotePath, 0o755, () => resolve())
          } else {
            resolve()
          }
        })
        writeStream.on('error', reject)
        readStream.pipe(writeStream)
      })
    })
  }

  _humanSize(bytes) {
    const units = ['B', 'KB', 'MB', 'GB']
    let i = 0
    while (bytes >= 1024 && i < units.length - 1) { bytes /= 1024; i++ }
    return `${bytes.toFixed(1)} ${units[i]}`
  }

  _buildSummary(results, components) {
    const lines = ['📋 安装结果汇总', '─'.repeat(40)]
    const icons = { true: '✅', false: '❌', undefined: '⏭' }
    for (const comp of components) {
      const ok = results[comp.key]
      lines.push(`${icons[ok] ?? '⏭'} ${comp.name}: ${ok ? '成功' : '失败/跳过'}`)
    }
    return lines.join('\n')
  }

  _initLogFile() {
    // 打包后 resources/ 目录只读，日志写到 userData/logs；开发时写到项目根 logs/
    const logsDir = app.isPackaged
      ? path.join(app.getPath('userData'), 'logs')
      : path.join(__dirname, '../../logs')
    if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true })
    const ts = new Date().toISOString().replace(/[:.]/g, '-').substring(0, 19)
    this._logFile = path.join(logsDir, `install_${ts}.log`)
    fs.writeFileSync(this._logFile,
      `OpenCloud 安装日志 - ${new Date().toLocaleString()}\n${'='.repeat(60)}\n`,
      'utf-8')
  }

  _writeLog(msg, level) {
    if (!this._logFile) return
    try {
      const ts = new Date().toTimeString().substring(0, 8)
      fs.appendFileSync(this._logFile, `[${ts}][${level}] ${msg}\n`, 'utf-8')
    } catch (_) {}
  }
}

module.exports = SSHService
