<template>
  <div class="page-container">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="24">
        <el-card shadow="never">
          <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
            <!-- PromQL 输入 -->
            <el-input
              v-model="expr"
              placeholder="输入 PromQL 表达式，例如：node_cpu_seconds_total{mode='idle'}"
              clearable
              style="flex:1;min-width:300px"
              @keyup.enter="handleQuery"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <!-- 时间范围 -->
            <el-select v-model="timeRange" style="width:130px" @change="handleQuery">
              <el-option label="最近 5 分钟"  value="5m" />
              <el-option label="最近 15 分钟" value="15m" />
              <el-option label="最近 30 分钟" value="30m" />
              <el-option label="最近 1 小时"  value="1h" />
              <el-option label="最近 3 小时"  value="3h" />
              <el-option label="最近 6 小时"  value="6h" />
              <el-option label="最近 12 小时" value="12h" />
              <el-option label="最近 24 小时" value="24h" />
              <el-option label="最近 7 天"    value="7d" />
            </el-select>

            <!-- Step -->
            <el-select v-model="step" style="width:100px">
              <el-option label="Step 15s" :value="15" />
              <el-option label="Step 30s" :value="30" />
              <el-option label="Step 1m"  :value="60" />
              <el-option label="Step 5m"  :value="300" />
              <el-option label="Step 15m" :value="900" />
            </el-select>

            <el-button type="primary" :loading="loading" :icon="Search" @click="handleQuery">
              查询
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷指标卡片 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col
        v-for="tpl in queryTemplates"
        :key="tpl.label"
        :xs="12" :sm="8" :md="6" :lg="4"
      >
        <el-card
          shadow="hover"
          class="tpl-card"
          :class="{ active: expr === tpl.expr }"
          @click="applyTemplate(tpl)"
        >
          <div class="tpl-icon">{{ tpl.icon }}</div>
          <div class="tpl-label">{{ tpl.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="card-title">查询结果</span>
          <div class="header-actions">
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button label="line">折线图</el-radio-button>
              <el-radio-button label="area">面积图</el-radio-button>
            </el-radio-group>
            <el-tag v-if="series.length" type="info" size="small">
              {{ series.length }} 条时间序列
            </el-tag>
          </div>
        </div>
      </template>

      <div v-if="!queried" class="empty-hint">
        <el-empty description="请输入 PromQL 表达式并点击查询" :image-size="120" />
      </div>
      <div v-else-if="series.length === 0" class="empty-hint">
        <el-empty description="暂无数据，请检查表达式是否正确" :image-size="120" />
      </div>
      <div v-else ref="chartEl" class="chart-container" />
    </el-card>

    <!-- 原始数据表格 -->
    <el-card v-if="series.length > 0" shadow="never" style="margin-top:16px">
      <template #header>
        <span class="card-title">原始数据（前 5 条序列，各 10 个采样点）</span>
      </template>
      <el-tabs>
        <el-tab-pane
          v-for="(s, idx) in series.slice(0, 5)"
          :key="idx"
          :label="seriesLabel(s.metric)"
        >
          <el-table :data="s.values.slice(-10)" size="small" border>
            <el-table-column label="时间" width="200">
              <template #default="{ row }">{{ tsToDate(row[0]) }}</template>
            </el-table-column>
            <el-table-column label="值">
              <template #default="{ row }">{{ row[1] }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { queryMetricRange } from '@/api/monitor'
import type { MetricRangeDataVO } from '@/api/monitor'

// ─── 状态 ──────────────────────────────────────────────────────
const expr      = ref('')
const timeRange = ref('1h')
const step      = ref(60)
const loading   = ref(false)
const queried   = ref(false)
const chartType = ref<'line' | 'area'>('line')
const series    = ref<{ metric: Record<string, string>; values: [number, string][] }[]>([])
const chartEl   = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

// ─── 快捷模板 ─────────────────────────────────────────────────
const queryTemplates = [
  { label: 'CPU 使用率',    icon: '💻', expr: '100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)' },
  { label: '内存使用率',    icon: '🧠', expr: '(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100' },
  { label: '磁盘 IO',       icon: '💾', expr: 'rate(node_disk_io_time_seconds_total[5m])' },
  { label: '网络入流量',    icon: '📥', expr: 'rate(node_network_receive_bytes_total[5m])' },
  { label: '网络出流量',    icon: '📤', expr: 'rate(node_network_transmit_bytes_total[5m])' },
  { label: 'HTTP 请求率',   icon: '🌐', expr: 'rate(http_requests_total[5m])' },
  { label: 'JVM 堆内存',    icon: '☕', expr: 'jvm_memory_used_bytes{area="heap"}' },
  { label: 'GC 耗时',       icon: '🗑️',  expr: 'rate(jvm_gc_pause_seconds_sum[5m])' },
  { label: 'Pod CPU',        icon: '🐳', expr: 'rate(container_cpu_usage_seconds_total{container!=""}[5m])' },
  { label: 'Pod 内存',      icon: '📦', expr: 'container_memory_working_set_bytes{container!=""}' },
  { label: '告警触发数',    icon: '🚨', expr: 'ALERTS{alertstate="firing"}' },
  { label: 'Up/Down 状态',  icon: '🟢', expr: 'up' },
]

function applyTemplate(tpl: { label: string; icon: string; expr: string }) {
  expr.value = tpl.expr
  handleQuery()
}

// ─── 时间范围解析 ──────────────────────────────────────────────
function parseRange(range: string): number {
  const num = parseInt(range)
  if (range.endsWith('m')) return num * 60
  if (range.endsWith('h')) return num * 3600
  if (range.endsWith('d')) return num * 86400
  return 3600
}

// ─── 查询 ──────────────────────────────────────────────────────
async function handleQuery() {
  if (!expr.value.trim()) {
    ElMessage.warning('请输入 PromQL 表达式')
    return
  }
  loading.value = true
  queried.value = true
  try {
    const now   = Math.floor(Date.now() / 1000)
    const start = now - parseRange(timeRange.value)
    const res: MetricRangeDataVO = await queryMetricRange({
      query: expr.value.trim(),
      start,
      end:   now,
      step:  step.value,
    })
    series.value = res.result
    await nextTick()
    renderChart()
  } catch {
    ElMessage.error('查询失败，请检查 PromQL 语法或后端连接')
    series.value = []
  } finally {
    loading.value = false
  }
}

// ─── ECharts 渲染 ─────────────────────────────────────────────
function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value, 'dark')
  }

  const colors = ['#60a5fa','#34d399','#f59e0b','#f87171','#a78bfa','#fb923c','#38bdf8','#4ade80']
  const seriesData = series.value.map((s, i) => ({
    name: seriesLabel(s.metric),
    type: 'line' as const,
    smooth: true,
    symbol: 'none',
    areaStyle: chartType.value === 'area' ? { opacity: 0.15 } : undefined,
    data: s.values.map(([ts, val]) => [ts * 1000, parseFloat(val)]),
    lineStyle: { color: colors[i % colors.length], width: 2 },
    itemStyle: { color: colors[i % colors.length] },
  }))

  chart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 60, right: 20, top: 40, bottom: 60 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const time = new Date(params[0].value[0]).toLocaleString('zh-CN')
        let html = `<div style="font-size:12px;margin-bottom:4px;color:#ccc">${time}</div>`
        for (const p of params) {
          const val = typeof p.value[1] === 'number' ? p.value[1].toFixed(4) : p.value[1]
          html += `<div style="display:flex;align-items:center;gap:6px">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span style="flex:1;color:#ddd;font-size:11px;max-width:200px;overflow:hidden;text-overflow:ellipsis">${p.seriesName}</span>
            <b style="color:#fff">${val}</b>
          </div>`
        }
        return html
      },
    },
    legend: {
      type: 'scroll',
      bottom: 0,
      textStyle: { color: '#aaa', fontSize: 11 },
      pageTextStyle: { color: '#aaa' },
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        color: '#666',
        formatter: (val: number) =>
          new Date(val).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      },
      axisLine: { lineStyle: { color: '#333' } },
      splitLine: { lineStyle: { color: '#1e2a3a' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { color: '#1e2a3a' } },
    },
    series: seriesData,
  }, true)
}

// ─── 工具函数 ─────────────────────────────────────────────────
function seriesLabel(metric: Record<string, string>): string {
  const { __name__, ...rest } = metric
  const labels = Object.entries(rest)
    .map(([k, v]) => `${k}="${v}"`)
    .join(', ')
  return __name__ ? (labels ? `${__name__}{${labels}}` : __name__) : labels || '{}'
}

function tsToDate(ts: number): string {
  return new Date(ts * 1000).toLocaleString('zh-CN')
}

// ─── 响应式图表 ───────────────────────────────────────────────
watch(chartType, renderChart)

function onResize() { chart?.resize() }

onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.page-container { padding: 0; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.card-title { font-size: 15px; font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 12px; }

.tpl-card {
  cursor: pointer;
  text-align: center;
  padding: 12px 4px;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.tpl-card:hover { border-color: #409eff; transform: translateY(-2px); }
.tpl-card.active { border-color: #409eff; background: #ecf5ff; }
.tpl-icon  { font-size: 22px; line-height: 1.4; }
.tpl-label { font-size: 12px; color: #606266; margin-top: 4px; }

.chart-container {
  width: 100%;
  height: 420px;
}
.empty-hint { padding: 60px 0; }
</style>
