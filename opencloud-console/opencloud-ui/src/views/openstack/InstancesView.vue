<template>
  <div class="page-container">
    <!-- 工具栏 -->
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-select v-model="selectedClusterId" placeholder="选择集群" style="width:200px" @change="handleClusterChange">
            <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
          </el-select>
          <el-input v-model="queryParams.name" placeholder="云主机名称" clearable style="width:180px" />
          <el-select v-model="queryParams.status" placeholder="状态" clearable style="width:130px">
            <el-option label="运行中" value="ACTIVE" />
            <el-option label="已停止" value="SHUTOFF" />
            <el-option label="暂停" value="SUSPENDED" />
            <el-option label="错误" value="ERROR" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset({})">重置</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">创建云主机</el-button>
      </div>
    </el-card>

    <!-- 卡片/表格切换 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">云主机列表（{{ state.total }}）</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="table"><el-icon><List /></el-icon></el-radio-button>
            <el-radio-button value="card"><el-icon><Grid /></el-icon></el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 表格视图 -->
      <el-table v-if="viewMode === 'table'" :data="state.list" v-loading="state.loading" border stripe>
        <el-table-column prop="name" label="实例名称" min-width="180" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status.toLowerCase()" :label="statusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="flavorName" label="规格" width="140" />
        <el-table-column prop="imageName" label="镜像" min-width="160" show-overflow-tooltip />
        <el-table-column label="IP 地址" min-width="160">
          <template #default="{ row }">
            <template v-for="(addrs, net) in row.addresses" :key="net">
              <div v-for="a in addrs" :key="a.addr" style="font-size:13px">
                <el-tag size="small" :type="a.type === 'floating' ? 'warning' : 'info'">{{ a.type === 'floating' ? '外网' : '内网' }}</el-tag>
                <span style="margin-left:4px">{{ a.addr }}</span>
              </div>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="created" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SHUTOFF'" link type="success" :icon="VideoPlay" :loading="actionLoading[row.id]" @click="handleAction('start', row)">启动</el-button>
            <el-button v-if="row.status === 'ACTIVE'" link type="warning" :icon="VideoPause" :loading="actionLoading[row.id]" @click="handleAction('stop', row)">停止</el-button>
            <el-button link type="primary" :icon="RefreshRight" :loading="actionLoading[row.id]" @click="handleAction('reboot', row)">重启</el-button>
            <el-button link type="danger" :icon="Delete" :loading="actionLoading[row.id]" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 卡片视图 -->
      <div v-else class="card-grid" v-loading="state.loading">
        <div v-for="vm in state.list" :key="vm.id" class="vm-card">
          <div class="vm-card-header">
            <StatusBadge :status="vm.status.toLowerCase()" :label="statusLabel(vm.status)" />
            <el-dropdown @command="(cmd: string) => handleAction(cmd, vm)">
              <el-icon class="more-btn"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="vm.status === 'SHUTOFF'" command="start">启动</el-dropdown-item>
                  <el-dropdown-item v-if="vm.status === 'ACTIVE'" command="stop">停止</el-dropdown-item>
                  <el-dropdown-item command="reboot">重启</el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color:#f56c6c">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="vm-name">{{ vm.name }}</div>
          <div class="vm-info">
            <div><span class="label">规格</span>{{ vm.flavorName }}</div>
            <div><span class="label">镜像</span>{{ vm.imageName }}</div>
            <div v-for="(addrs, net) in vm.addresses" :key="net">
              <span class="label">IP</span>
              <span v-for="a in addrs" :key="a.addr" style="margin-right:6px">{{ a.addr }}</span>
            </div>
          </div>
        </div>
        <div v-if="!state.loading && state.list.length === 0" class="empty-tip">
          <el-empty description="暂无云主机" />
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="state.pageNum"
          v-model:page-size="state.pageSize"
          :total="state.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 创建云主机 Dialog（步骤表单）-->
    <el-dialog v-model="showCreateDialog" title="创建云主机" width="680px" destroy-on-close>
      <el-steps :active="createStep" align-center finish-status="success" style="margin-bottom:24px">
        <el-step title="基础配置" />
        <el-step title="网络/安全组" />
        <el-step title="确认" />
      </el-steps>

      <!-- Step 0：基础配置 -->
      <div v-if="createStep === 0">
        <el-form ref="step0Form" :model="createForm" label-width="90px">
          <el-form-item label="实例名称" prop="name" :rules="[{ required: true, message: '请输入实例名称' }]">
            <el-input v-model="createForm.name" placeholder="如：web-01" />
          </el-form-item>
          <el-form-item label="实例数量">
            <el-input-number v-model="createForm.count" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="规格" prop="flavorId" :rules="[{ required: true, message: '请选择规格' }]">
            <el-select v-model="createForm.flavorId" placeholder="选择规格" style="width:100%" filterable>
              <el-option
                v-for="f in flavorList"
                :key="f.id"
                :label="`${f.name}（${f.vcpus} vCPU / ${f.ram/1024}GB RAM / ${f.disk}GB`"
                :value="f.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="镜像" prop="imageId" :rules="[{ required: true, message: '请选择镜像' }]">
            <el-select v-model="createForm.imageId" placeholder="选择镜像" style="width:100%" filterable>
              <el-option v-for="img in imageList" :key="img.id" :label="img.name" :value="img.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 1：网络/安全组 -->
      <div v-else-if="createStep === 1">
        <el-form ref="step1Form" :model="createForm" label-width="90px">
          <el-form-item label="网络" prop="networkIds" :rules="[{ required: true, message: '请至少选择一个网络', type: 'array', min: 1 }]">
            <el-select v-model="createForm.networkIds" multiple placeholder="选择网络" style="width:100%">
              <el-option v-for="n in networkList" :key="n.id" :label="n.name" :value="n.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="安全组">
            <el-select v-model="createForm.securityGroupIds" multiple placeholder="选择安全组" style="width:100%">
              <el-option v-for="sg in securityGroups" :key="sg.id" :label="sg.name" :value="sg.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="密钥对">
            <el-input v-model="createForm.keyName" placeholder="可选" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2：确认 -->
      <div v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实例名称">{{ createForm.name }}</el-descriptions-item>
          <el-descriptions-item label="实例数量">{{ createForm.count }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ flavorList.find(f => f.id === createForm.flavorId)?.name }}</el-descriptions-item>
          <el-descriptions-item label="镜像">{{ imageList.find(i => i.id === createForm.imageId)?.name }}</el-descriptions-item>
          <el-descriptions-item label="网络" :span="2">
            {{ networkList.filter(n => createForm.networkIds.includes(n.id)).map(n => n.name).join(', ') }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button v-if="createStep > 0" @click="createStep--">上一步</el-button>
        <el-button v-if="createStep < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-else type="primary" :loading="createLoading" @click="handleCreate">立即创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, RefreshRight, VideoPlay, VideoPause, List, Grid, MoreFilled } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  getInstances, createInstance, deleteInstance, startInstance, stopInstance, rebootInstance,
  getFlavors, getImages, getNetworks, getSecurityGroups, getClusterList,
} from '@/api/cloud'
import type { InstanceVO, FlavorVO, ImageVO, NetworkVO, SecurityGroupVO, ClusterVO } from '@/api/cloud'
import { useTable } from '@/hooks/useTable'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const viewMode          = ref<'table' | 'card'>('table')
const actionLoading     = reactive<Record<string, boolean>>({})

// 创建所需数据
const flavorList      = ref<FlavorVO[]>([])
const imageList       = ref<ImageVO[]>([])
const networkList     = ref<NetworkVO[]>([])
const securityGroups  = ref<SecurityGroupVO[]>([])

const { state, queryParams, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  useTable<{ name?: string; status?: string }, InstanceVO>({
    fetchFn: (p) => {
      if (!selectedClusterId.value) return Promise.resolve({ records: [], total: 0, size: p.pageSize!, current: p.pageNum! })
      return getInstances(selectedClusterId.value, p)
    },
    immediate: false,
  })

onMounted(async () => {
  clusterList.value = await getClusterList()
  if (clusterList.value.length) {
    selectedClusterId.value = clusterList.value[0].clusterId
    handleClusterChange(selectedClusterId.value)
  }
})

async function handleClusterChange(clusterId: number) {
  handleSearch()
  const [flavors, images, networks, sgs] = await Promise.all([
    getFlavors(clusterId), getImages(clusterId), getNetworks(clusterId), getSecurityGroups(clusterId),
  ])
  flavorList.value    = flavors
  imageList.value     = images
  networkList.value   = networks
  securityGroups.value = sgs
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '运行中', SHUTOFF: '已停止', SUSPENDED: '暂停', ERROR: '错误', BUILD: '创建中', REBOOT: '重启中',
}
function statusLabel(s: string) { return STATUS_LABEL[s] ?? s }

// ── 操作 ──────────────────────────────────────────────────
async function handleAction(action: string, row: InstanceVO) {
  if (action === 'delete') { handleDelete(row); return }
  actionLoading[row.id] = true
  try {
    if (action === 'start')  await startInstance(selectedClusterId.value!, row.id)
    if (action === 'stop')   await stopInstance(selectedClusterId.value!, row.id)
    if (action === 'reboot') await rebootInstance(selectedClusterId.value!, row.id)
    ElMessage.success('操作已提交')
    setTimeout(handleSearch, 1500)
  } finally {
    actionLoading[row.id] = false
  }
}

async function handleDelete(row: InstanceVO) {
  await ElMessageBox.confirm(`确定删除云主机「${row.name}」？此操作不可恢复！`, '危险操作', { type: 'error' })
  await deleteInstance(selectedClusterId.value!, row.id)
  ElMessage.success('删除成功')
  handleSearch()
}

// ── 创建向导 ─────────────────────────────────────────────
const showCreateDialog = ref(false)
const createStep       = ref(0)
const createLoading    = ref(false)
const step0Form        = ref<FormInstance>()
const step1Form        = ref<FormInstance>()

const createForm = reactive({
  name: '',
  count: 1,
  flavorId: '',
  imageId: '',
  networkIds: [] as string[],
  securityGroupIds: [] as string[],
  keyName: '',
})

async function nextStep() {
  if (createStep.value === 0) await step0Form.value?.validate()
  if (createStep.value === 1) await step1Form.value?.validate()
  createStep.value++
}

async function handleCreate() {
  createLoading.value = true
  try {
    await createInstance(selectedClusterId.value!, createForm)
    ElMessage.success('创建请求已提交，实例即将就绪')
    showCreateDialog.value = false
    createStep.value = 0
    setTimeout(handleSearch, 2000)
  } finally {
    createLoading.value = false
  }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.left   { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title  { font-size: 15px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 100px;
}

.vm-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  transition: box-shadow .2s;
}
.vm-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.1); }

.vm-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.vm-name { font-size: 15px; font-weight: 600; color: #1d2b3a; margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.vm-info { font-size: 13px; color: #606266; display: flex; flex-direction: column; gap: 4px; }
.vm-info .label { color: #909399; width: 36px; display: inline-block; }

.more-btn { cursor: pointer; font-size: 18px; color: #909399; }
.more-btn:hover { color: #409eff; }
.empty-tip { grid-column: 1 / -1; }
</style>
