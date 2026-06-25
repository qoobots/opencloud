<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-select v-model="selectedClusterId" placeholder="选择 Ceph 集群" style="width:220px" @change="fetchBuckets">
          <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="16" style="flex:1;min-height:0">
      <!-- Bucket 列表 -->
      <el-col :span="8">
        <el-card shadow="never" style="height:100%">
          <template #header>
            <div class="card-header">
              <span class="title">Bucket 列表</span>
              <el-button type="primary" size="small" :icon="Plus" @click="showCreateBucket = true">新建</el-button>
            </div>
          </template>
          <div v-loading="loadingBuckets">
            <div
              v-for="b in buckets"
              :key="b.name"
              class="bucket-item"
              :class="{ active: currentBucket === b.name }"
              @click="selectBucket(b.name)"
            >
              <el-icon><FolderOpened /></el-icon>
              <div class="bucket-info">
                <div class="bucket-name">{{ b.name }}</div>
                <div class="bucket-meta">{{ b.creationDate }}</div>
              </div>
              <el-button
                link
                type="danger"
                size="small"
                @click.stop="handleDeleteBucket(b.name)"
              ><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-empty v-if="!loadingBuckets && !buckets.length" description="暂无 Bucket" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 文件浏览器 -->
      <el-col :span="16">
        <el-card shadow="never" style="height:100%">
          <template #header>
            <div class="card-header">
              <div class="breadcrumb-path">
                <el-icon><Folder /></el-icon>
                <span style="margin-left:6px">{{ currentBucket || '— 请选择 Bucket —' }}</span>
                <template v-if="currentPrefix">
                  <el-icon><ArrowRight /></el-icon>
                  <span>{{ currentPrefix }}</span>
                </template>
              </div>
              <div style="display:flex;gap:8px">
                <el-button size="small" @click="currentPrefix = ''; fetchObjects()">根目录</el-button>
              </div>
            </div>
          </template>

          <div v-loading="loadingObjects">
            <el-table :data="objects" border stripe style="width:100%">
              <el-table-column label="对象名称" min-width="280">
                <template #default="{ row }">
                  <el-icon style="color:#e6a23c;vertical-align:middle"><Document /></el-icon>
                  <span style="margin-left:4px;font-size:13px">{{ row.key }}</span>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatSize(row.size) }}</template>
              </el-table-column>
              <el-table-column prop="lastModified" label="修改时间" width="170" />
              <el-table-column prop="storageClass" label="存储类" width="110" align="center">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.storageClass }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loadingObjects && !objects.length && currentBucket" description="Bucket 为空" :image-size="60" />
            <el-empty v-if="!currentBucket" description="请先选择左侧 Bucket" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建 Bucket Dialog -->
    <el-dialog v-model="showCreateBucket" title="新建 Bucket" width="380px" destroy-on-close>
      <el-form ref="bucketFormRef" :model="bucketForm" :rules="bucketRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="bucketForm.name" placeholder="小写字母、数字、连字符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateBucket = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateBucket">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, FolderOpened, Folder, Document, ArrowRight } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getClusterList, getCephBuckets, createBucket, deleteBucket, getBucketObjects } from '@/api/cloud'
import type { ClusterVO, BucketVO, ObjectVO } from '@/api/cloud'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const buckets           = ref<BucketVO[]>([])
const objects           = ref<ObjectVO[]>([])
const currentBucket     = ref('')
const currentPrefix     = ref('')
const loadingBuckets    = ref(false)
const loadingObjects    = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  const ceph = clusterList.value.find(c => c.clusterType === 'CEPH')
  if (ceph) { selectedClusterId.value = ceph.clusterId; fetchBuckets() }
})

async function fetchBuckets() {
  if (!selectedClusterId.value) return
  loadingBuckets.value = true
  try { buckets.value = await getCephBuckets(selectedClusterId.value) }
  finally { loadingBuckets.value = false }
}

async function selectBucket(name: string) {
  currentBucket.value = name
  currentPrefix.value = ''
  fetchObjects()
}

async function fetchObjects() {
  if (!selectedClusterId.value || !currentBucket.value) return
  loadingObjects.value = true
  try {
    objects.value = await getBucketObjects(selectedClusterId.value, currentBucket.value, currentPrefix.value)
  } finally { loadingObjects.value = false }
}

async function handleDeleteBucket(name: string) {
  await ElMessageBox.confirm(`确定删除 Bucket「${name}」？（需为空 Bucket）`, '警告', { type: 'warning' })
  await deleteBucket(selectedClusterId.value!, name)
  ElMessage.success('删除成功')
  if (currentBucket.value === name) { currentBucket.value = ''; objects.value = [] }
  fetchBuckets()
}

// ── 新建 Bucket ────────────────────────────────────────────
const showCreateBucket = ref(false)
const createLoading    = ref(false)
const bucketFormRef    = ref<FormInstance>()
const bucketForm       = reactive({ name: '' })
const bucketRules: FormRules = {
  name: [
    { required: true, message: '请输入 Bucket 名称', trigger: 'blur' },
    { pattern: /^[a-z0-9-]{3,63}$/, message: '仅支持小写字母、数字、连字符，3~63 位', trigger: 'blur' },
  ],
}

async function handleCreateBucket() {
  await bucketFormRef.value?.validate()
  createLoading.value = true
  try {
    await createBucket(selectedClusterId.value!, bucketForm.name)
    ElMessage.success('创建成功')
    showCreateBucket.value = false
    fetchBuckets()
  } finally { createLoading.value = false }
}

function formatSize(bytes: number) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.breadcrumb-path { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #303133; }

.bucket-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background .15s;
}
.bucket-item:hover { background: #f5f7fa; }
.bucket-item.active { background: #ecf5ff; }
.bucket-info { flex: 1; min-width: 0; }
.bucket-name { font-size: 14px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bucket-meta { font-size: 12px; color: #909399; }
</style>
