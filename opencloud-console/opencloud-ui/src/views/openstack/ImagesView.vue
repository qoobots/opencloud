<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-select v-model="selectedClusterId" placeholder="选择集群" style="width:200px" @change="fetchData">
            <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">上传镜像</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="imageList" v-loading="loading" border stripe>
        <el-table-column prop="name" label="镜像名称" min-width="200" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="right">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="diskFormat" label="磁盘格式" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.diskFormat }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="visibility" label="可见性" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visibility === 'public' ? 'success' : 'info'" size="small">{{ row.visibility }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传镜像 Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传镜像" width="480px" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="镜像名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="请输入镜像名称" />
        </el-form-item>
        <el-form-item label="磁盘格式" prop="diskFormat">
          <el-select v-model="uploadForm.diskFormat" style="width:100%">
            <el-option label="qcow2" value="qcow2" />
            <el-option label="raw" value="raw" />
            <el-option label="vmdk" value="vmdk" />
            <el-option label="iso" value="iso" />
          </el-select>
        </el-form-item>
        <el-form-item label="镜像文件" prop="file">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".img,.qcow2,.raw,.vmdk,.iso"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { getClusterList, getImages, uploadImage, deleteImage } from '@/api/cloud'
import type { ClusterVO, ImageVO } from '@/api/cloud'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList       = ref<ClusterVO[]>([])
const selectedClusterId = ref<number>()
const imageList         = ref<ImageVO[]>([])
const loading           = ref(false)

onMounted(async () => {
  clusterList.value = await getClusterList()
  if (clusterList.value.length) { selectedClusterId.value = clusterList.value[0].clusterId; fetchData() }
})

async function fetchData() {
  if (!selectedClusterId.value) return
  loading.value = true
  try { imageList.value = await getImages(selectedClusterId.value) }
  finally { loading.value = false }
}

async function handleDelete(row: ImageVO) {
  await ElMessageBox.confirm(`确定删除镜像「${row.name}」？`, '警告', { type: 'warning' })
  await deleteImage(selectedClusterId.value!, row.id)
  ElMessage.success('删除成功')
  fetchData()
}

function formatSize(bytes: number) {
  if (!bytes) return '—'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`
}

// ── 上传 ──────────────────────────────────────────────────
const showUploadDialog = ref(false)
const uploadLoading    = ref(false)
const uploadFormRef    = ref<FormInstance>()
const selectedFile     = ref<File | null>(null)

const uploadForm = reactive({ name: '', diskFormat: 'qcow2' })
const uploadRules: FormRules = {
  name:       [{ required: true, message: '请输入镜像名称', trigger: 'blur' }],
  diskFormat: [{ required: true }],
}

function handleFileChange(file: UploadFile) {
  selectedFile.value = file.raw ?? null
}

async function handleUpload() {
  await uploadFormRef.value?.validate()
  if (!selectedFile.value) { ElMessage.warning('请选择镜像文件'); return }
  uploadLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('name', uploadForm.name)
    fd.append('diskFormat', uploadForm.diskFormat)
    await uploadImage(selectedClusterId.value!, fd)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    fetchData()
  } finally {
    uploadLoading.value = false
  }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 12px 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.left { display: flex; align-items: center; gap: 10px; }
</style>
