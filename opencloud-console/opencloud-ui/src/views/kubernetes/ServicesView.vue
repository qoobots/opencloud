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

    <el-table :data="services" v-loading="loading" border stripe>
      <el-table-column prop="name" label="Service 名称" min-width="220" />
      <el-table-column prop="namespace" label="命名空间" width="150" />
      <el-table-column prop="type" label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="svcTypeColor(row.type)" size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="clusterIp" label="ClusterIP" width="150" />
      <el-table-column prop="externalIp" label="外部 IP" width="150" />
      <el-table-column label="端口" min-width="220">
        <template #default="{ row }">
          <el-tag
            v-for="p in row.ports"
            :key="`${p.port}`"
            size="small"
            style="margin-right:4px;margin-bottom:2px"
          >{{ p.port }}:{{ p.targetPort }}/{{ p.protocol }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creationTimestamp" label="创建时间" width="170" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getClusterList, getServices } from '@/api/cloud'
import type { ClusterVO, K8sServiceVO } from '@/api/cloud'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const services          = ref<K8sServiceVO[]>([])
const loading           = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  const k8s = clusterList.value.find(c => c.clusterType === 'KUBERNETES')
  if (k8s) { selectedClusterId.value = k8s.clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loading.value = true
  try { services.value = await getServices(selectedClusterId.value) }
  finally { loading.value = false }
}

function svcTypeColor(type: string) {
  const map: Record<string, string> = { ClusterIP: '', NodePort: 'warning', LoadBalancer: 'success', ExternalName: 'info' }
  return map[type] ?? ''
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
</style>
