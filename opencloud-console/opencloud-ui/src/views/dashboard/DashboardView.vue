<template>
  <div class="dashboard" v-loading="pageLoading">
    <!-- 指标卡片 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="6" v-for="card in metricCards" :key="card.label">
        <div class="metric-card" :style="{ '--color': card.color }">
          <div class="metric-icon">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="metric-info">
            <div class="metric-value">
              <span v-if="card.loading" class="skeleton-text">—</span>
              <span v-else>{{ card.value }}</span>
            </div>
            <div class="metric-label">{{ card.label }}</div>
          </div>
          <div class="metric-trend" v-if="card.sub">
            <span class="sub-text">{{ card.sub }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- CPU / 内存趋势 -->
      <el-col :xs="24" :lg="16">
        <div class="page-card">
          <div class="card-header">
            <span class="card-title">CPU / 内存 使用趋势</span>
            <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
              <el-radio-button label="30m">30 分钟</el-radio-button>
              <el-radio-button label="1h">1 小时</el-radio-button>
              <el-radio-button label="3h">3 小时</el-radio-button>
              <el-radio-button label="6h">6 小时</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="trendLoading" class="chart-skeleton" />
          <v-chart v-else :option="trendOption" style="height:280px" autoresize />
        </div>
      </el-col>

      <!-- 组件在线状态 -->
      <el-col :xs="24" :lg="8">
        <div class="page-card">
          <div class="card-header">
            <span class="card-title">组件在线状态</span>
            <el-button
              :icon="Refresh"
              size="small"
              circle
              :loading="statusLoading"
              @click="loadComponentStatus"
            />
          </div>
          <div class="component-list">
            <div
              v-for="comp in componentStatus"
              :key="comp.name"
              class="comp-item"
            >
              <div class="comp-left">
                <span class="comp-dot" :class="comp.online ? 'online' : 'offline'" />
                <span class="comp-name">{{ comp.name }}</span>
              </div>
              <div class="comp-right">
                <el-tag :type="comp.online ? 'success' : 'danger'" size="small">
                  {{ comp.online ? '在线' : '离线' }}
                </el-tag>
                <span v-if="comp.version" class="comp-version">{{ comp.version }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 告警统计 + 最近告警 -->
    <el-row :gutter="16" style="margin-top:0">
      <!-- 告警统计小卡片 -->
      <el-col :xs="24" :lg="8">
        <div class="page-card alert-stat-card">
          <div class="card-title">告警统计（当前）</div>
          <div class="alert-stats">
            <div class="stat-item critical">
              <div class="stat-num">{{ alertStats.critical }}</div>
              <div class="stat-label">紧急</div>
            </div>
            <div class="stat-item warning">
              <div class="stat-num">{{ alertStats.warning }}</div>
              <div class="stat-label">警告</div>
            </div>
            <div class="stat-item info">
              <div class="stat-num">{{ alertStats.info }}</div>
              <div class="stat-label">信息</div>
            </div>
            <div class="stat-item total">
              <div class="stat-num">{{ alertStats.total }}</div>
              <div class="stat-label">总计</div>
            </div>
          </div>
          <div style="margin-top:16px">
            <v-chart :option="alertPieOption" style="height:160px" autoresize />
          </div>
        </div>
      </el-col>

      <!-- 最近告警列表 -->
      <el-col :xs="24" :lg="16">
        <div class="page-card">
          <div class="card-header">
            <span class="card-title">
              最近告警
              <el-tag v-if="alertStats.firing > 0" type="danger" size="small" effect="dark">
                {{ alertStats.firing }} 条触发中
              </el-tag>
            </span>
            <el-button text type="primary" size="small" @click="$router.push('/monitor/alerts')">
              查看全部 →
            </el-button>
          </div>
          <el-table :data="recentAlerts" stripe size="small" v-loading="alertLoading">
            <el-table-column prop="alertName" label="告警名称" show-overflow-tooltip />
            <el-table-column label="级别" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="severityTagType(row.severity)" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="摘要" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'FIRING' ? 'danger' : 'success'" size="small">
                  {{ row.status === 'FIRING' ? '触发中' : '已恢复' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startsAt" label="触发时间" width="160" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DataZoomComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { Monitor, DataBoard, SetUp, Warning, Refresh } from '@element-plus/icons-vue'
import { queryMetricRange, getAlertStats, getAlertRecordPage } from '@/api/monitor'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent,
     LegendComponent, TitleComponent, DataZoomComponent])

const router      = useRouter()
const pageLoading = ref(false)

// ─── 统计卡片 ──────────────────────────────────────────────────
const metricCards = reactive([
  { label: '活跃告警',       value: '0',  sub: '',        icon: 'Warning',   color: '#ef4444', loading: true },
  { label: '触发中告警',     value: '0',  sub: '',        icon: 'Monitor',   color: '#f59e0b', loading: true },
  { label: '今日已确认',     value: '0',  sub: '',        icon: 'DataBoard', color: '#10b981', loading: true },
  { label: '今日已恢复',     value: '0',  sub: '',        icon: 'SetUp',     color: '#3b82f6', loading: true },
])

// ─── 告警统计 ─────────────────────────────────────────────────
const alertLoading = ref(false)
const alertStats   = reactive({ total: 0, critical: 0, warning: 0, info: 0, firing: 0, resolved: 0, acknowledged: 0 })
const recentAlerts = ref<any[]>([])

async function loadAlertData() {
  alertLoading.value = true
  try {
    const [stats, page] = await Promise.all([
      getAlertStats(),
      getAlertRecordPage({ pageNum: 1, pageSize: 8 }),
    ])
    // 填充统计
    Object.assign(alertStats, stats)
    recentAlerts.value = page.records

    // 更新卡片
    metricCards[0].value   = String(stats.total)
    metricCards[1].value   = String(stats.firing)
    metricCards[2].value   = String(stats.acknowledged)
    metricCards[3].value   = String(stats.resolved)
    metricCards.forEach(c => (c.loading = false))

    // 更新告警饼图
    alertPieOption.value.series[0].data = [
      { name: '紧急',   value: stats.critical,     itemStyle: { color: '#ef4444' } },
      { name: '警告',   value: stats.warning,      itemStyle: { color: '#f59e0b' } },
      { name: '信息',   value: stats.info,         itemStyle: { color: '#3b82f6' } },
    ]
  } catch {
    metricCards.forEach(c => { c.value = '—'; c.loading = false })
  } finally {
    alertLoading.value = false
  }
}

// ─── 告警饼图 ─────────────────────────────────────────────────
const alertPieOption = ref({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie',
    radius: ['45%', '68%'],
    center: ['50%', '44%'],
    data: [
      { name: '紧急', value: 0, itemStyle: { color: '#ef4444' } },
      { name: '警告', value: 0, itemStyle: { color: '#f59e0b' } },
      { name: '信息', value: 0, itemStyle: { color: '#3b82f6' } },
    ],
    label: { show: false },
    emphasis: { itemStyle: { shadowBlur: 6, shadowColor: 'rgba(0,0,0,.2)' } },
  }],
})

// ─── 组件状态 ─────────────────────────────────────────────────
const statusLoading = ref(false)
const componentStatus = ref([
  { name: 'Prometheus',  online: false, version: '' },
  { name: 'Grafana',     online: false, version: '' },
  { name: 'AlertManager',online: false, version: '' },
  { name: 'OpenStack',   online: false, version: '' },
  { name: 'Ceph',        online: false, version: '' },
  { name: 'Kubernetes',  online: false, version: '' },
])

async function loadComponentStatus() {
  statusLoading.value = true
  try {
    // 用 up{} 指标判断 Prometheus 本身是否在线
    const now   = Math.floor(Date.now() / 1000)
    const start = now - 120
    const res   = await queryMetricRange({ query: 'up', start, end: now, step: 60 })
    // 重置
    componentStatus.value.forEach(c => { c.online = false; c.version = '' })
    // 根据 job/instance label 映射
    for (const series of res.result) {
      const job = (series.metric.job || '').toLowerCase()
      const lastVal = series.values[series.values.length - 1]?.[1]
      const online  = lastVal === '1'
      if (job.includes('prometheus'))   { componentStatus.value[0].online = online }
      if (job.includes('grafana'))      { componentStatus.value[1].online = online }
      if (job.includes('alertmanager')) { componentStatus.value[2].online = online }
      if (job.includes('openstack'))    { componentStatus.value[3].online = online }
      if (job.includes('ceph'))         { componentStatus.value[4].online = online }
      if (job.includes('kube') || job.includes('kubernetes')) { componentStatus.value[5].online = online }
    }
    // Prometheus 本身能返回数据代表在线
    if (res.result.length > 0) componentStatus.value[0].online = true
  } catch {
    // 接口失败统一标灰
  } finally {
    statusLoading.value = false
  }
}

// ─── CPU/内存趋势 ──────────────────────────────────────────────
const trendRange   = ref<'30m' | '1h' | '3h' | '6h'>('1h')
const trendLoading = ref(false)

const trendOption = ref<any>({
  tooltip: {
    trigger: 'axis',
    formatter: (params: any) => {
      const t = new Date(params[0].value[0]).toLocaleTimeString('zh-CN')
      return `<b>${t}</b><br/>` + params.map((p: any) =>
        `${p.marker}${p.seriesName}: <b>${Number(p.value[1]).toFixed(1)}%</b>`
      ).join('<br/>')
    },
  },
  legend: { data: ['CPU 使用率', '内存使用率'], bottom: 0 },
  grid: { top: 20, bottom: 50, left: 54, right: 16 },
  xAxis: { type: 'time', axisLabel: {
    formatter: (v: number) => new Date(v).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    color: '#666',
  }},
  yAxis: {
    type: 'value', max: 100,
    axisLabel: { formatter: '{value}%', color: '#666' },
    splitLine: { lineStyle: { color: '#f0f0f0' } },
  },
  series: [
    {
      name: 'CPU 使用率',
      type: 'line', smooth: true, symbol: 'none',
      data: [] as [number, number][],
      itemStyle: { color: '#3b82f6' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.2)' }, { offset: 1, color: 'rgba(59,130,246,0)' }] } },
    },
    {
      name: '内存使用率',
      type: 'line', smooth: true, symbol: 'none',
      data: [] as [number, number][],
      itemStyle: { color: '#10b981' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } },
    },
  ],
})

const rangeSeconds: Record<string, number> = { '30m': 1800, '1h': 3600, '3h': 10800, '6h': 21600 }
const stepMap:      Record<string, number> = { '30m': 30,   '1h': 60,   '3h': 180,   '6h': 300  }

async function loadTrend() {
  trendLoading.value = true
  const now   = Math.floor(Date.now() / 1000)
  const start = now - rangeSeconds[trendRange.value]
  const step  = stepMap[trendRange.value]

  try {
    const [cpuRes, memRes] = await Promise.all([
      queryMetricRange({
        query: '100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
        start, end: now, step,
      }),
      queryMetricRange({
        query: '(1 - avg(node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100',
        start, end: now, step,
      }),
    ])

    const toPoints = (result: typeof cpuRes) =>
      (result.result[0]?.values ?? []).map(([ts, v]) => [ts * 1000, parseFloat(v)] as [number, number])

    trendOption.value.series[0].data = toPoints(cpuRes)
    trendOption.value.series[1].data = toPoints(memRes)
  } catch {
    // 接口失败时生成演示数据
    _mockTrendData()
  } finally {
    trendLoading.value = false
  }
}

/** 降级 mock：用于后端未启动时也能展示图表 */
function _mockTrendData() {
  const now    = Date.now()
  const total  = rangeSeconds[trendRange.value]
  const step   = stepMap[trendRange.value] * 1000
  const cpuData: [number, number][] = []
  const memData: [number, number][] = []
  for (let t = now - total * 1000; t <= now; t += step) {
    cpuData.push([t, parseFloat((20 + Math.random() * 35).toFixed(1))])
    memData.push([t, parseFloat((45 + Math.random() * 25).toFixed(1))])
  }
  trendOption.value.series[0].data = cpuData
  trendOption.value.series[1].data = memData
}

// ─── 工具 ─────────────────────────────────────────────────────
function severityTagType(severity: string): string {
  return { CRITICAL: 'danger', WARNING: 'warning', INFO: '' }[severity] ?? ''
}

// ─── 生命周期 ─────────────────────────────────────────────────
let refreshTimer: ReturnType<typeof setInterval>

onMounted(async () => {
  pageLoading.value = true
  await Promise.all([loadAlertData(), loadTrend(), loadComponentStatus()])
  pageLoading.value = false

  // 每 60 秒自动刷新告警统计
  refreshTimer = setInterval(loadAlertData, 60_000)
})

onBeforeUnmount(() => clearInterval(refreshTimer))
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

/* 指标卡片 */
.metric-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  border-left: 4px solid var(--color);
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.metric-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.10); }
.metric-icon { color: var(--color); flex-shrink: 0; }
.metric-value { font-size: 26px; font-weight: 700; color: #1e293b; line-height: 1; }
.metric-label { font-size: 12px; color: #64748b; margin-top: 4px; }
.metric-trend { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); }
.sub-text { font-size: 12px; color: #94a3b8; }
.skeleton-text { color: #cbd5e1; }

/* 页面卡片 */
.page-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 组件状态列表 */
.component-list { display: flex; flex-direction: column; gap: 10px; }
.comp-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
}
.comp-left { display: flex; align-items: center; gap: 8px; }
.comp-dot {
  width: 8px; height: 8px; border-radius: 50%;
  flex-shrink: 0;
}
.comp-dot.online  { background: #22c55e; box-shadow: 0 0 0 3px rgba(34,197,94,.2); }
.comp-dot.offline { background: #ef4444; }
.comp-name    { font-size: 13px; color: #374151; }
.comp-right   { display: flex; align-items: center; gap: 6px; }
.comp-version { font-size: 11px; color: #94a3b8; }

/* 告警统计卡片 */
.alert-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 8px;
}
.stat-item {
  text-align: center;
  padding: 12px 4px;
  border-radius: 8px;
  background: #f8fafc;
}
.stat-item.critical { background: #fef2f2; }
.stat-item.warning  { background: #fffbeb; }
.stat-item.info     { background: #eff6ff; }
.stat-item.total    { background: #f0fdf4; }
.stat-num   { font-size: 22px; font-weight: 700; }
.stat-label { font-size: 11px; color: #64748b; margin-top: 2px; }
.critical .stat-num { color: #ef4444; }
.warning .stat-num  { color: #f59e0b; }
.info .stat-num     { color: #3b82f6; }
.total .stat-num    { color: #10b981; }

/* 趋势图骨架 */
.chart-skeleton {
  height: 280px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
