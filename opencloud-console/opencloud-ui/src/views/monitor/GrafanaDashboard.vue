<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-select v-model="selectedDashUid" placeholder="选择面板" style="width:300px" @change="reloadIframe">
          <el-option v-for="d in dashboards" :key="d.uid" :label="d.title" :value="d.uid" />
        </el-select>
        <div class="right">
          <el-select v-model="timeRange" style="width:130px" @change="reloadIframe">
            <el-option label="最近 15 分钟" value="now-15m" />
            <el-option label="最近 1 小时" value="now-1h" />
            <el-option label="最近 3 小时" value="now-3h" />
            <el-option label="最近 6 小时" value="now-6h" />
            <el-option label="最近 24 小时" value="now-24h" />
            <el-option label="最近 7 天" value="now-7d" />
          </el-select>
          <el-button :icon="Refresh" @click="reloadIframe">刷新</el-button>
          <el-button :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="iframe-card" v-loading="loading">
      <div ref="iframeWrapRef" class="iframe-wrap">
        <iframe
          v-if="iframeUrl"
          :src="iframeUrl"
          frameborder="0"
          class="grafana-iframe"
          @load="loading = false"
          @error="loading = false"
        />
        <el-empty v-else description="请选择 Grafana 面板" :image-size="100" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import { getGrafanaDashboards } from '@/api/monitor'
import type { GrafanaDashboardVO } from '@/api/monitor'

const dashboards      = ref<GrafanaDashboardVO[]>([])
const selectedDashUid = ref('')
const timeRange       = ref('now-1h')
const loading         = ref(false)
const iframeWrapRef   = ref<HTMLElement>()

const iframeUrl = computed(() => {
  const dash = dashboards.value.find(d => d.uid === selectedDashUid.value)
  if (!dash) return ''
  const base = dash.url.startsWith('http') ? dash.url : `/grafana${dash.url}`
  return `${base}?orgId=1&from=${timeRange.value}&to=now&kiosk=1`
})

onMounted(async () => {
  try {
    dashboards.value = await getGrafanaDashboards()
    if (dashboards.value.length) {
      selectedDashUid.value = dashboards.value[0].uid
      loading.value = true
    }
  } catch {
    // Grafana 不可用时静默处理
  }
})

function reloadIframe() {
  loading.value = true
  // 强制重新渲染 iframe
  const uid = selectedDashUid.value
  selectedDashUid.value = ''
  setTimeout(() => { selectedDashUid.value = uid }, 50)
}

function toggleFullscreen() {
  if (!iframeWrapRef.value) return
  if (!document.fullscreenElement) {
    iframeWrapRef.value.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; height: 100%; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.right { display: flex; align-items: center; gap: 10px; }
.iframe-card { flex: 1; }
.iframe-card :deep(.el-card__body) { padding: 0; height: calc(100vh - 220px); }
.iframe-wrap { width: 100%; height: 100%; }
.grafana-iframe { width: 100%; height: 100%; border: none; display: block; }
</style>
