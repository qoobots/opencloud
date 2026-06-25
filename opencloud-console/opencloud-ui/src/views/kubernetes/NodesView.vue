<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-select v-model="selectedClusterId" placeholder="选择 K8s 集群" style="width:220px" @change="fetchData">
          <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
      </div>
    </el-card>

    <el-table :data="nodes" v-loading="loading" border stripe>
      <el-table-column prop="name" label="节点名称" min-width="220" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="角色" width="140">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" :type="r.includes('control') ? 'primary' : ''" style="margin-right:4px">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="K8s 版本" width="130" />
      <el-table-column prop="osImage" label="操作系统" min-width="200" show-overflow-tooltip />
      <el-table-column label="CPU (容量/可分配)" width="180">
        <template #default="{ row }">
          <div>{{ row.cpuCapacity }} / {{ row.cpuAllocatable }}</div>
        </template>
      </el-table-column>
      <el-table-column label="内存 (容量/可分配)" width="200">
        <template #default="{ row }">
          <div>{{ formatMemory(row.memoryCapacity) }} / {{ formatMemory(row.memoryAllocatable) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="creationTimestamp" label="加入时间" width="170" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getClusterList, getNodes } from '@/api/cloud'
import type { ClusterVO, NodeVO } from '@/api/cloud'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const nodes             = ref<NodeVO[]>([])
const loading           = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  const k8s = clusterList.value.find(c => c.clusterType === 'KUBERNETES')
  if (k8s) { selectedClusterId.value = k8s.clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loading.value = true
  try { nodes.value = await getNodes(selectedClusterId.value) }
  finally { loading.value = false }
}

function formatMemory(mem: string) {
  if (!mem) return '—'
  const ki = parseInt(mem)
  if (isNaN(ki)) return mem
  return `${(ki / 1024 / 1024).toFixed(1)} GiB`
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
</style>
