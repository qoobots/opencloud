<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-select v-model="selectedClusterId" placeholder="选择 Ceph 集群" style="width:220px" @change="fetchPools">
          <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
        </el-select>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">存储池列表</span>
        </div>
      </template>

      <el-table :data="poolList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="存储池名称" min-width="160" />
        <el-table-column prop="pgNum" label="PG 数" width="90" align="right" />
        <el-table-column prop="size" label="副本数" width="80" align="right" />
        <el-table-column label="已用容量" min-width="220">
          <template #default="{ row }">
            <div class="usage-cell">
              <span class="usage-text">{{ formatBytes(row.usedBytes) }}</span>
              <el-progress :percentage="Math.round(row.percentUsed)" :stroke-width="8" :color="progressColor(row.percentUsed)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="compressionMode" label="压缩模式" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.compressionMode !== 'none' ? 'success' : 'info'" size="small">{{ row.compressionMode }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 存储池容量 ECharts 柱状图 -->
    <el-card shadow="never" header="各存储池容量对比">
      <div ref="barChartRef" style="height:300px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getClusterList, getCephPools } from '@/api/cloud'
import type { ClusterVO, CephPoolVO } from '@/api/cloud'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const poolList          = ref<CephPoolVO[]>([])
const loading           = ref(false)
const barChartRef       = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null

onMounted(async () => {
  clusterList.value = await getClusterList()
  const cephCluster = clusterList.value.find(c => c.clusterType === 'CEPH')
  if (cephCluster) { selectedClusterId.value = cephCluster.clusterId; fetchPools() }
})

async function fetchPools() {
  if (!selectedClusterId.value) return
  loading.value = true
  try {
    poolList.value = await getCephPools(selectedClusterId.value)
    await nextTick()
    renderBarChart()
  } finally { loading.value = false }
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)
  const names = poolList.value.map(p => p.name)
  const used  = poolList.value.map(p => +(p.usedBytes / 1024 ** 3).toFixed(2))
  barChart.setOption({
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}<br/>已用: ${p[0].value} GiB` },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 15 } },
    yAxis: { type: 'value', name: '容量 (GiB)' },
    series: [{
      type: 'bar',
      data: used,
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#60a5fa' },
        { offset: 1, color: '#3b82f6' },
      ])},
      barMaxWidth: 48,
      label: { show: true, position: 'top', fontSize: 12 },
    }],
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
  })
}

function formatBytes(bytes: number) {
  if (bytes >= 1024 ** 4) return `${(bytes / 1024 ** 4).toFixed(2)} TiB`
  if (bytes >= 1024 ** 3) return `${(bytes / 1024 ** 3).toFixed(2)} GiB`
  if (bytes >= 1024 ** 2) return `${(bytes / 1024 ** 2).toFixed(2)} MiB`
  return `${bytes} B`
}

function progressColor(pct: number) {
  if (pct > 80) return '#f56c6c'
  if (pct > 60) return '#e6a23c'
  return '#67c23a'
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.usage-cell { display: flex; flex-direction: column; gap: 4px; }
.usage-text { font-size: 13px; color: #606266; }
</style>
