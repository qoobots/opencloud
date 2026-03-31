<template>
  <div class="dashboard">
    <!-- 指标卡片 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="6" v-for="card in metricCards" :key="card.label">
        <div class="metric-card" :style="{ '--color': card.color }">
          <div class="metric-icon"><el-icon :size="28"><component :is="card.icon" /></el-icon></div>
          <div class="metric-info">
            <div class="metric-value">{{ card.value }}</div>
            <div class="metric-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <div class="page-card">
          <div class="card-title">CPU / 内存 使用趋势（近 1 小时）</div>
          <v-chart :option="trendOption" style="height:280px" autoresize />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <div class="card-title">组件状态</div>
          <v-chart :option="pieOption" style="height:280px" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- 告警列表 -->
    <div class="page-card alert-card">
      <div class="card-title">
        最近告警
        <el-tag type="danger" size="small" class="alert-count">{{ alertList.length }}</el-tag>
      </div>
      <el-table :data="alertList" stripe size="small">
        <el-table-column prop="alertName"  label="告警名称" />
        <el-table-column prop="severity"   label="级别" width="90">
          <template #default="{ row }">
            <span class="status-badge" :class="severityClass(row.severity)">{{ row.severity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary"   label="摘要" show-overflow-tooltip />
        <el-table-column prop="firedAt"   label="触发时间" width="170" />
        <el-table-column prop="status"    label="状态" width="90">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 'firing' ? 'danger' : 'success'">
              {{ row.status === 'firing' ? '触发中' : '已恢复' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { Monitor, DataBoard, SetUp, Warning } from '@element-plus/icons-vue'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

// 指标卡片（实际数据从后端接口获取）
const metricCards = ref([
  { label: '云主机总数',    value: '—', icon: Monitor,   color: '#3b82f6' },
  { label: '存储使用率',    value: '—', icon: DataBoard,  color: '#10b981' },
  { label: 'K8s 工作负载', value: '—', icon: SetUp,      color: '#f59e0b' },
  { label: '活跃告警',      value: '—', icon: Warning,    color: '#ef4444' },
])

// 趋势图配置
const trendOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: ['CPU 使用率', '内存使用率'], bottom: 0 },
  grid: { top: 20, bottom: 40, left: 50, right: 20 },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
  series: [
    { name: 'CPU 使用率',  type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#3b82f6' } },
    { name: '内存使用率',  type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#10b981' } },
  ],
})

// 组件状态饼图
const pieOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, orient: 'horizontal' },
  series: [{
    type: 'pie', radius: ['50%', '70%'], center: ['50%', '45%'],
    data: [
      { name: 'Ceph',       value: 1, itemStyle: { color: '#3b82f6' } },
      { name: 'OpenStack',  value: 1, itemStyle: { color: '#10b981' } },
      { name: 'Kubernetes', value: 1, itemStyle: { color: '#f59e0b' } },
      { name: 'Prometheus', value: 1, itemStyle: { color: '#ef4444' } },
      { name: 'Grafana',    value: 1, itemStyle: { color: '#8b5cf6' } },
    ],
    label: { show: false },
  }],
})

// 告警列表（Mock，后端对接后替换）
const alertList = ref<any[]>([])

function severityClass(severity: string) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[severity] ?? 'info'
}

onMounted(() => {
  // TODO: 调用后端接口填充真实数据
  // 生成模拟趋势数据
  const times: string[] = []
  const cpuData: number[] = []
  const memData: number[] = []
  const now = new Date()
  for (let i = 59; i >= 0; i--) {
    const t = new Date(now.getTime() - i * 60000)
    times.push(`${t.getHours().toString().padStart(2,'0')}:${t.getMinutes().toString().padStart(2,'0')}`)
    cpuData.push(Math.round(20 + Math.random() * 30))
    memData.push(Math.round(40 + Math.random() * 25))
  }
  trendOption.value.xAxis.data = times
  trendOption.value.series[0].data = cpuData
  trendOption.value.series[1].data = memData
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

.metric-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  border-left: 4px solid var(--color);
}
.metric-icon { color: var(--color); }
.metric-value { font-size: 26px; font-weight: 700; color: #1e293b; }
.metric-label { font-size: 13px; color: #64748b; margin-top: 2px; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.alert-count { margin-left: 4px; }
.alert-card  { margin-top: 0; }
</style>
