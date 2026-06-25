<template>
  <el-tag :type="tagType" :effect="effect" :size="size">
    <span class="status-dot" :class="`dot-${status}`"></span>
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  status: string
  label?: string
  size?: 'large' | 'default' | 'small'
  effect?: 'light' | 'dark' | 'plain'
}>(), {
  size: 'small',
  effect: 'light',
})

// 状态 → Element Plus tag type 映射
const STATUS_TYPE_MAP: Record<string, string> = {
  // 通用
  active: 'success', enabled: 'success', running: 'success', up: 'success',
  connected: 'success', ready: 'success', success: 'success', ok: 'success',
  normal: 'success', bound: 'success', in_use: 'success',
  // 警告
  warning: 'warning', pending: 'warning', paused: 'warning', unknown: 'warning',
  degraded: 'warning', slow: 'warning', warn: 'warning',
  // 错误
  error: 'danger', failed: 'danger', down: 'danger', disconnected: 'danger',
  critical: 'danger', firing: 'danger', unavailable: 'danger',
  // 中性
  inactive: 'info', disabled: 'info', stopped: 'info', terminated: 'info',
  resolved: 'info', acknowledged: 'info', info: 'info',
}

const tagType = computed(() => STATUS_TYPE_MAP[props.status?.toLowerCase()] ?? 'info')
const label = computed(() => props.label ?? props.status)
</script>

<style scoped>
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: middle;
}

.dot-running, .dot-active, .dot-enabled, .dot-up, .dot-connected, .dot-ready, .dot-success, .dot-ok, .dot-normal, .dot-bound, .dot-in_use {
  background-color: #67c23a;
  box-shadow: 0 0 4px #67c23a;
}

.dot-warning, .dot-pending, .dot-paused, .dot-unknown, .dot-degraded, .dot-warn {
  background-color: #e6a23c;
  box-shadow: 0 0 4px #e6a23c;
}

.dot-error, .dot-failed, .dot-down, .dot-disconnected, .dot-critical, .dot-firing, .dot-unavailable {
  background-color: #f56c6c;
  box-shadow: 0 0 4px #f56c6c;
  animation: blink 1.2s infinite;
}

.dot-inactive, .dot-disabled, .dot-stopped, .dot-terminated, .dot-resolved, .dot-acknowledged, .dot-info {
  background-color: #909399;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
