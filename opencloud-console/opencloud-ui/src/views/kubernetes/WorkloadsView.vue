<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="left">
          <el-select v-model="selectedClusterId" placeholder="选择 K8s 集群" style="width:220px" @change="fetchData">
            <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
          </el-select>
          <el-input v-model="nsFilter" placeholder="命名空间" clearable style="width:160px" @clear="fetchData" />
        </div>
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
      </div>
    </el-card>

    <!-- Deployment 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">Deployment（{{ deployments.length }}）</span>
        </div>
      </template>
      <el-table :data="deployments" v-loading="loading" border stripe>
        <el-table-column prop="name" label="名称" min-width="220" />
        <el-table-column prop="namespace" label="命名空间" width="150" />
        <el-table-column label="就绪状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.readyReplicas === row.replicas ? 'success' : 'warning'" size="small">
              {{ row.readyReplicas }}/{{ row.replicas }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="image" label="镜像" min-width="260" show-overflow-tooltip />
        <el-table-column prop="creationTimestamp" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewPods(row)">查看 Pod</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Pod 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">Pod 列表（{{ pods.length }}）<span v-if="filterDeployment" class="filter-badge">仅显示: {{ filterDeployment }}</span></span>
          <el-button v-if="filterDeployment" size="small" @click="filterDeployment = ''; fetchData()">显示全部</el-button>
        </div>
      </template>
      <el-table :data="filteredPods" v-loading="loading" border stripe>
        <el-table-column prop="name" label="Pod 名称" min-width="260" show-overflow-tooltip />
        <el-table-column prop="namespace" label="命名空间" width="150" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="Pod IP" width="140" />
        <el-table-column prop="nodeName" label="节点" width="160" show-overflow-tooltip />
        <el-table-column prop="creationTimestamp" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLogDialog(row)">日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Pod 日志 Dialog -->
    <el-dialog v-model="logDialogVisible" :title="`日志 — ${logPod?.name}`" width="900px" destroy-on-close>
      <div class="log-toolbar">
        <el-select v-if="logPod?.containers.length" v-model="selectedContainer" style="width:200px" @change="fetchLogs">
          <el-option v-for="c in logPod?.containers" :key="c.name" :label="c.name" :value="c.name" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchLogs">刷新</el-button>
      </div>
      <div class="log-box" v-loading="logLoading">
        <pre>{{ logContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getClusterList, getDeployments, getPods, getPodLogs } from '@/api/cloud'
import type { ClusterVO, DeploymentVO, PodVO } from '@/api/cloud'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const nsFilter          = ref('')
const loading           = ref(false)
const deployments       = ref<DeploymentVO[]>([])
const pods              = ref<PodVO[]>([])
const filterDeployment  = ref('')

const filteredPods = computed(() => {
  if (!filterDeployment.value) return pods.value
  return pods.value.filter(p => p.name.startsWith(filterDeployment.value))
})

onMounted(async () => {
  clusterList.value = await getClusterList()
  const k8s = clusterList.value.find(c => c.clusterType === 'KUBERNETES')
  if (k8s) { selectedClusterId.value = k8s.clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loading.value = true
  try {
    const [deps, podList] = await Promise.all([
      getDeployments(selectedClusterId.value, nsFilter.value || undefined),
      getPods(selectedClusterId.value, nsFilter.value || undefined),
    ])
    deployments.value = deps
    pods.value        = podList
  } finally { loading.value = false }
}

function viewPods(dep: DeploymentVO) {
  filterDeployment.value = dep.name
}

// ── 日志 ──────────────────────────────────────────────────
const logDialogVisible  = ref(false)
const logPod            = ref<PodVO | null>(null)
const selectedContainer = ref('')
const logContent        = ref('')
const logLoading        = ref(false)

async function openLogDialog(pod: PodVO) {
  logPod.value          = pod
  selectedContainer.value = pod.containers[0]?.name ?? ''
  logDialogVisible.value = true
  fetchLogs()
}

async function fetchLogs() {
  if (!logPod.value || !selectedClusterId.value) return
  logLoading.value = true
  try {
    logContent.value = await getPodLogs(
      selectedClusterId.value,
      logPod.value.namespace,
      logPod.value.name,
      selectedContainer.value,
    )
  } finally { logLoading.value = false }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.left { display: flex; align-items: center; gap: 10px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.filter-badge { font-size: 12px; color: #409eff; margin-left: 8px; font-weight: 400; }

.log-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.log-box {
  background: #1e1e2e;
  border-radius: 6px;
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}
.log-box pre {
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #a6e3a1;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
