import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

export interface WsOptions {
  /** WebSocket 路径，相对路径如 /ws/alerts */
  path: string
  /** 接收消息回调 */
  onMessage?: (data: any) => void
  /** 连接成功回调 */
  onOpen?: () => void
  /** 连接关闭回调 */
  onClose?: () => void
  /** 心跳间隔（毫秒），默认 30000 */
  heartbeatInterval?: number
  /** 最大重连次数，默认 5 */
  maxRetries?: number
  /** 是否自动连接，默认 true */
  autoConnect?: boolean
}

/**
 * WebSocket 连接 Hook（带 JWT、心跳、自动重连）
 */
export function useWebSocket(options: WsOptions) {
  const {
    path,
    onMessage,
    onOpen,
    onClose,
    heartbeatInterval = 30000,
    maxRetries = 5,
    autoConnect = true,
  } = options

  const userStore = useUserStore()

  const ws        = ref<WebSocket | null>(null)
  const connected = ref(false)
  const retries   = ref(0)

  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  function buildUrl(): string {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host     = location.host
    const token    = userStore.accessToken
    return `${protocol}//${host}/api${path}?token=${token}`
  }

  function clearTimers() {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
    if (reconnectTimer) { clearTimeout(reconnectTimer);  reconnectTimer = null }
  }

  function startHeartbeat() {
    heartbeatTimer = setInterval(() => {
      if (ws.value?.readyState === WebSocket.OPEN) {
        ws.value.send(JSON.stringify({ type: 'ping' }))
      }
    }, heartbeatInterval)
  }

  function connect() {
    if (ws.value?.readyState === WebSocket.OPEN) return
    clearTimers()

    ws.value = new WebSocket(buildUrl())

    ws.value.onopen = () => {
      connected.value = true
      retries.value = 0
      startHeartbeat()
      onOpen?.()
    }

    ws.value.onmessage = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'pong') return
        onMessage?.(data)
      } catch {
        onMessage?.(event.data)
      }
    }

    ws.value.onerror = () => {
      // 静默处理，由 onclose 统一处理重连
    }

    ws.value.onclose = () => {
      connected.value = false
      clearTimers()
      onClose?.()
      // 自动重连
      if (retries.value < maxRetries) {
        retries.value++
        const delay = Math.min(1000 * 2 ** retries.value, 30000)
        reconnectTimer = setTimeout(connect, delay)
      } else {
        ElMessage.warning('WebSocket 连接已断开，请刷新页面重试')
      }
    }
  }

  function disconnect() {
    clearTimers()
    ws.value?.close()
    ws.value = null
    connected.value = false
    retries.value = maxRetries // 禁止重连
  }

  function send(data: object | string) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }

  onMounted(() => { if (autoConnect) connect() })
  onUnmounted(() => disconnect())

  return { connected, connect, disconnect, send }
}
