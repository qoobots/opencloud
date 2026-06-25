<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-select v-model="selectedClusterId" placeholder="选择集群" style="width:220px" @change="fetchData">
          <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建云硬盘</el-button>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 云硬盘列表 -->
      <el-tab-pane label="云硬盘" name="volumes">
        <el-table :data="volumes" v-loading="loadingVol" border stripe>
          <el-table-column prop="name" label="卷名称" min-width="180" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <StatusBadge :status="volStatusKey(row.status)" :label="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="size" label="容量(GB)" width="100" align="right" />
          <el-table-column prop="volumeType" label="类型" width="120" />
          <el-table-column label="挂载信息" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.attachments?.length">
                {{ row.attachments.map((a: any) => `${a.serverId?.slice(0,8)}... (${a.device})`).join(', ') }}
              </span>
              <span v-else class="text-muted">未挂载</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="!row.attachments?.length" link type="primary" @click="openAttachDialog(row)">挂载</el-button>
              <el-button v-else link type="warning" @click="handleDetach(row)">卸载</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDeleteVol(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 快照列表 -->
      <el-tab-pane label="快照" name="snapshots">
        <div class="tab-toolbar">
          <el-button @click="fetchData">刷新</el-button>
        </div>
        <el-table :data="snapshots" v-loading="loadingSnap" border stripe>
          <el-table-column prop="name" label="快照名称" min-width="180" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="size" label="容量(GB)" width="100" align="right" />
          <el-table-column prop="volumeId" label="来源卷 ID" min-width="280" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建云硬盘 Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建云硬盘" width="480px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="卷名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入云硬盘名称" />
        </el-form-item>
        <el-form-item label="容量(GB)" prop="size">
          <el-input-number v-model="createForm.size" :min="1" :max="32768" style="width:100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="createForm.volumeType" placeholder="如：__DEFAULT__" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 挂载 Dialog -->
    <el-dialog v-model="showAttachDialog" title="挂载云硬盘" width="420px" destroy-on-close>
      <el-form ref="attachFormRef" :model="attachForm" label-width="90px">
        <el-form-item label="目标实例" prop="instanceId" :rules="[{ required: true, message: '请输入实例 ID' }]">
          <el-input v-model="attachForm.instanceId" placeholder="请输入云主机 Instance ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAttachDialog = false">取消</el-button>
        <el-button type="primary" :loading="attachLoading" @click="handleAttach">确定挂载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getClusterList, getVolumes, createVolume, deleteVolume, attachVolume, detachVolume, getVolumeSnapshots } from '@/api/cloud'
import type { ClusterVO, VolumeVO, VolumeSnapshotVO } from '@/api/cloud'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const activeTab         = ref('volumes')
const volumes           = ref<VolumeVO[]>([])
const snapshots         = ref<VolumeSnapshotVO[]>([])
const loadingVol        = ref(false)
const loadingSnap       = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  if (clusterList.value.length) { selectedClusterId.value = clusterList.value[0].clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loadingVol.value = true
  getVolumes(selectedClusterId.value).then(r => { volumes.value = r.records }).finally(() => { loadingVol.value = false })
  loadingSnap.value = true
  getVolumeSnapshots(selectedClusterId.value).then(r => { snapshots.value = r }).finally(() => { loadingSnap.value = false })
}

function volStatusKey(status: string) {
  const map: Record<string, string> = { available: 'active', in-use: 'in_use', error: 'error', creating: 'pending' }
  return map[status] ?? status
}

async function handleDeleteVol(row: VolumeVO) {
  await ElMessageBox.confirm(`确定删除云硬盘「${row.name}」？`, '警告', { type: 'warning' })
  await deleteVolume(selectedClusterId.value!, row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleDetach(row: VolumeVO) {
  await ElMessageBox.confirm('确定卸载该云硬盘？', '确认', { type: 'warning' })
  await detachVolume(selectedClusterId.value!, row.id)
  ElMessage.success('卸载成功')
  fetchData()
}

// ── 创建 ──────────────────────────────────────────────────
const showCreateDialog = ref(false)
const createLoading    = ref(false)
const createFormRef    = ref<FormInstance>()
const createForm       = reactive({ name: '', size: 40, volumeType: '' })
const createRules: FormRules = {
  name: [{ required: true, message: '请输入卷名称', trigger: 'blur' }],
  size: [{ required: true, type: 'number', min: 1 }],
}

function openCreateDialog() { showCreateDialog.value = true }

async function handleCreate() {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    await createVolume(selectedClusterId.value!, createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    fetchData()
  } finally { createLoading.value = false }
}

// ── 挂载 ──────────────────────────────────────────────────
const showAttachDialog = ref(false)
const attachLoading    = ref(false)
const attachFormRef    = ref<FormInstance>()
const attachForm       = reactive({ instanceId: '' })
const currentVolumeId  = ref('')

function openAttachDialog(row: VolumeVO) {
  currentVolumeId.value = row.id
  attachForm.instanceId = ''
  showAttachDialog.value = true
}

async function handleAttach() {
  await attachFormRef.value?.validate()
  attachLoading.value = true
  try {
    await attachVolume(selectedClusterId.value!, currentVolumeId.value, attachForm.instanceId)
    ElMessage.success('挂载成功')
    showAttachDialog.value = false
    fetchData()
  } finally { attachLoading.value = false }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.tab-toolbar { margin-bottom: 12px; }
.text-muted { color: #c0c4cc; }
</style>
