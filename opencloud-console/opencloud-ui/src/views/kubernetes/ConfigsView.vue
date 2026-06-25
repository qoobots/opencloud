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

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ConfigMap -->
      <el-tab-pane label="ConfigMap" name="configmap">
        <el-table :data="configMaps" v-loading="loadingCm" border stripe>
          <el-table-column prop="name" label="名称" min-width="240" />
          <el-table-column prop="namespace" label="命名空间" width="150" />
          <el-table-column label="Key 列表" min-width="300">
            <template #default="{ row }">
              <el-tag
                v-for="k in row.dataKeys"
                :key="k"
                size="small"
                style="margin-right:4px;margin-bottom:2px"
              >{{ k }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="creationTimestamp" label="创建时间" width="170" />
        </el-table>
      </el-tab-pane>

      <!-- Secret -->
      <el-tab-pane label="Secret" name="secret">
        <el-table :data="secrets" v-loading="loadingSecret" border stripe>
          <el-table-column prop="name" label="名称" min-width="240" />
          <el-table-column prop="namespace" label="命名空间" width="150" />
          <el-table-column prop="type" label="类型" width="200" show-overflow-tooltip />
          <el-table-column label="Key 列表" min-width="260">
            <template #default="{ row }">
              <el-tag
                v-for="k in row.dataKeys"
                :key="k"
                size="small"
                type="warning"
                style="margin-right:4px;margin-bottom:2px"
              >{{ k }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="creationTimestamp" label="创建时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getClusterList, getConfigMaps, getSecrets } from '@/api/cloud'
import type { ClusterVO, ConfigMapVO, SecretVO } from '@/api/cloud'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const activeTab         = ref('configmap')
const configMaps        = ref<ConfigMapVO[]>([])
const secrets           = ref<SecretVO[]>([])
const loadingCm         = ref(false)
const loadingSecret     = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  const k8s = clusterList.value.find(c => c.clusterType === 'KUBERNETES')
  if (k8s) { selectedClusterId.value = k8s.clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loadingCm.value = true
  getConfigMaps(selectedClusterId.value).then(r => { configMaps.value = r }).finally(() => { loadingCm.value = false })
  loadingSecret.value = true
  getSecrets(selectedClusterId.value).then(r => { secrets.value = r }).finally(() => { loadingSecret.value = false })
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
</style>
