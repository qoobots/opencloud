<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <el-select v-model="selectedClusterId" placeholder="选择 Ceph 集群" style="width:220px" @change="fetchData">
        <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
      </el-select>
    </el-card>

    <!-- 状态卡片 -->
    <div class="metrics-row" v-loading="loading">
      <MetricCard
        label="集群状态"
        :value="overview?.status ?? '—'"
        icon="CircleCheck"
        :color="overview?.status === 'HEALTH_OK' ? 'green' : 'orange'"
      />
      <MetricCard
        label="存储使用率"
        :value="usagePercent"
        unit="%"
        icon="PieChart"
        :color="usagePercent > 80 ? 'red' : usagePercent > 60 ? 'orange' : 'green'"
      />
      <MetricCard
        label="OSD 总量"
        :value="overview?.osdTotal ?? 0"
        icon="Cpu"
        color="blue"
        :desc="`在线 ${overview?.osdUp ?? 0} / 活跃 ${overview?.osdIn ?? 0}`"
      />
      <MetricCard
        label="PG 总量"
        :value="overview?.pgTotal ?? 0"
        icon="DataLine"
        color="teal"
        :desc="`活跃 ${overview?.pgActive ?? 0}`"
      />
    </div>

    <!-- 容量详情 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" header="容量分布">
          <div ref="capChartRef" style="height:260px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="OSD 状态矩阵">
          <div class="osd-grid" v-loading="loading">
            <el-tooltip
              v-for="osd in osdList"
              :key="osd.osdId"
              :content="`OSD.${osd.osdId} | ${osd.host} | ${osd.deviceClass} | ${osd.utilization.toFixed(1)}%`"
              placement="top"
            >
              <div
                class="osd-block"
                :class="osd.status === 'up' ? 'up' : 'down'"
              >{{ osd.osdId }}</div>
            </el-tooltip>
            <el-empty v-if="!loading && !osdList.length" description="无 OSD 数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 存储池 -->
    <el-card shadow="never" header="存储池">
      <el-table :data="poolList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="存储池名称" min-width="160" />
        <el-table-column prop="pgNum" label="PG 数" width="80" align="right" />
        <el-table-column prop="size" label="副本数" width="80" align="right" />
        <el-table-column label="已用容量" min-width="200">
          <template #default="{ row }">
            <div>{{ formatBytes(row.usedBytes) }}</div>
            <el-progress :percentage="Math.round(row.percentUsed)" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="compressionMode" label="压缩" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.compressionMode !== 'none' ? 'success' : 'info'">{{ row.compressionMode }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getClusterList, getCephOverview, getCephPools, getCephOsdStatus } from '@/api/cloud'
import type { ClusterVO, CephOverviewVO, CephPoolVO, CephOsdVO } from '@/api/cloud'
import MetricCard from '@/components/MetricCard.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const loading           = ref(false)
const overview          = ref<CephOverviewVO | null>(null)
const poolList          = ref<CephPoolVO[]>([])
const osdList           = ref<CephOsdVO[]>([])
const capChartRef       = ref<HTMLElement>()
let   capChart: echarts.ECharts | null = null

const usagePercent = computed(() => {
  if (!overview.value?.totalBytes) return 0
  return Math.round((overview.value.usedBytes / overview.value.totalBytes) * 100)
})

onMounted(async () => {
  clusterList.value = await getClusterList()
  const cephCluster = clusterList.value.find(c => c.clusterType === 'CEPH')
  if (cephCluster) { selectedClusterId.value = cephCluster.clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loading.value = true
  try {
    const [ov, pools, osds] = await Promise.all([
      getCephOverview(selectedClusterId.value),
      getCephPools(selectedClusterId.value),
      getCephOsdStatus(selectedClusterId.value),
    ])
    overview.value = ov
    poolList.value = pools
    osdList.value  = osds
    await nextTick()
    renderCapChart()
  } finally { loading.value = false }
}

function renderCapChart() {
  if (!capChartRef.value || !overview.value) return
  if (!capChart) capChart = echarts.init(capChartRef.value)
  const { usedBytes, availBytes } = overview.value
  capChart.setOption({
    tooltip: { trigger: 'item', formatter: (p: any) => `${p.name}: ${formatBytes(p.value)} (${p.percent}%)` },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      data: [
        { value: usedBytes,  name: '已使用', itemStyle: { color: '#409eff' } },
        { value: availBytes, name: '可用',   itemStyle: { color: '#67c23a' } },
      ],
      label: { formatter: '{b}\n{d}%' },
    }],
  })
}

function formatBytes(bytes: number) {
  if (bytes >= 1024 ** 4) return `${(bytes / 1024 ** 4).toFixed(2)} TiB`
  if (bytes >= 1024 ** 3) return `${(bytes / 1024 ** 3).toFixed(2)} GiB`
  if (bytes >= 1024 ** 2) return `${(bytes / 1024 ** 2).toFixed(2)} MiB`
  return `${bytes} B`
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.metrics-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }

.osd-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  min-height: 180px;
  align-content: flex-start;
}

.osd-block {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  cursor: default;
  transition: transform .15s;
}
.osd-block:hover { transform: scale(1.15); }
.osd-block.up   { background: #ecf5ff; color: #409eff; border: 1px solid #b3d8ff; }
.osd-block.down { background: #fef0f0; color: #f56c6c; border: 1px solid #fbc4c4; animation: pulse 1.5s infinite; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
