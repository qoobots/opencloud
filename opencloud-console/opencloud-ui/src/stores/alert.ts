import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AlertRecordVO } from '@/api/monitor'

export const useAlertStore = defineStore('alert', () => {
  // 实时告警列表（WebSocket 推送）
  const alerts = ref<AlertRecordVO[]>([])
  // 未确认的 firing 告警数
  const unreadCount = computed(() =>
    alerts.value.filter(a => a.status === 'FIRING').length
  )

  function addAlert(alert: AlertRecordVO) {
    // 按 fingerprint 去重
    const idx = alerts.value.findIndex(a => a.fingerprint === alert.fingerprint)
    if (idx >= 0) {
      alerts.value.splice(idx, 1, alert)
    } else {
      alerts.value.unshift(alert)
    }
    // 最多保留 50 条
    if (alerts.value.length > 50) {
      alerts.value = alerts.value.slice(0, 50)
    }
  }

  function resolveAlert(fingerprint: string) {
    const idx = alerts.value.findIndex(a => a.fingerprint === fingerprint)
    if (idx >= 0) {
      alerts.value[idx] = { ...alerts.value[idx], status: 'RESOLVED' }
    }
  }

  function clearAlerts() {
    alerts.value = []
  }

  return { alerts, unreadCount, addAlert, resolveAlert, clearAlerts }
})
