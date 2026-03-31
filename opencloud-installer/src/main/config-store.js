/**
 * 配置持久化模块 - 使用文件存储服务器列表和组件配置
 * 密码通过 Electron safeStorage（系统密钥链）加密，存为 Base64
 */

const path = require('path')
const fs = require('fs')
const { app, safeStorage } = require('electron')

// 已加密密码的前缀标识
const ENC_PREFIX = 'enc:'

class ConfigStore {
  constructor() {
    // 存储路径：app 数据目录
    const userDataPath = app.getPath('userData')
    this._file = path.join(userDataPath, 'opencloud-config.json')
    this._data = this._load()
  }

  // ── 密码加解密 ─────────────────────────────────────
  _encryptPassword(plaintext) {
    if (!plaintext) return ''
    try {
      if (safeStorage.isEncryptionAvailable()) {
        const buf = safeStorage.encryptString(plaintext)
        return ENC_PREFIX + buf.toString('base64')
      }
    } catch (e) {
      console.warn('密码加密失败，以明文存储:', e.message)
    }
    return plaintext
  }

  _decryptPassword(stored) {
    if (!stored) return ''
    try {
      if (stored.startsWith(ENC_PREFIX) && safeStorage.isEncryptionAvailable()) {
        const buf = Buffer.from(stored.slice(ENC_PREFIX.length), 'base64')
        return safeStorage.decryptString(buf)
      }
    } catch (e) {
      console.warn('密码解密失败，返回原值:', e.message)
    }
    // 兼容旧明文数据
    return stored
  }

  _load() {
    try {
      if (fs.existsSync(this._file)) {
        return JSON.parse(fs.readFileSync(this._file, 'utf-8'))
      }
    } catch (e) {}
    return { servers: [], components: this._defaultComponents() }
  }

  _save() {
    try {
      fs.writeFileSync(this._file, JSON.stringify(this._data, null, 2), 'utf-8')
    } catch (e) {
      console.error('保存配置失败:', e)
    }
  }

  _defaultComponents() {
    return {
      ceph:       { enabled: true,  order: 1 },
      openstack:  { enabled: true,  order: 2 },
      kubernetes: { enabled: true,  order: 3 },
      prometheus: { enabled: true,  order: 4 },
      grafana:    { enabled: true,  order: 5 },
    }
  }

  // ── 服务器 ────────────────────────────────────────
  getServers() {
    // 返回时解密密码，不暴露存储格式
    return (this._data.servers || []).map(srv => ({
      ...srv,
      password: this._decryptPassword(srv.password),
    }))
  }

  saveServer(server) {
    const servers = this._data.servers || []
    // 保存时加密密码
    const toSave = {
      ...server,
      password: this._encryptPassword(server.password),
    }
    const idx = servers.findIndex(s => s.id === toSave.id)
    if (idx >= 0) {
      servers[idx] = toSave
    } else {
      toSave.id = `srv_${Date.now()}`
      servers.push(toSave)
    }
    this._data.servers = servers
    this._save()
    // 返回给前端时解密（前端 UI 回显需要明文）
    return { ...toSave, password: server.password }
  }

  deleteServer(id) {
    this._data.servers = (this._data.servers || []).filter(s => s.id !== id)
    this._save()
    return true
  }

  // ── 组件 ──────────────────────────────────────────
  getComponents() {
    return this._data.components || this._defaultComponents()
  }

  setComponents(components) {
    this._data.components = components
    this._save()
    return true
  }
}

module.exports = ConfigStore
