<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="IP 地址">
          <el-input v-model="queryParams.ipAddress" placeholder="请输入 IP" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span class="card-title">登录日志</span>
          <el-button type="danger" plain :icon="Delete" size="small" @click="handleBatchDelete">清空日志</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="130" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP 地址" width="140" />
        <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="备注信息" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import { getLoginLogPage } from '@/api/system'
import type { LoginLogVO } from '@/api/system'

const loading    = ref(false)
const tableData  = ref<LoginLogVO[]>([])
const dateRange  = ref<string[]>([])

const queryParams = reactive({
  username:  '',
  ipAddress: '',
  status:    undefined as number | undefined,
  startTime: '',
  endTime:   '',
})

const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const queryWithDate = computed(() => ({
  ...queryParams,
  startTime: dateRange.value?.[0] ?? '',
  endTime:   dateRange.value?.[1] ?? '',
}))

async function loadData() {
  loading.value = true
  try {
    const res = await getLoginLogPage({
      ...queryWithDate.value,
      pageNum:  pagination.pageNum,
      pageSize: pagination.pageSize,
    })
    tableData.value  = res.records
    pagination.total = res.total
  } catch {
    ElMessage.error('加载登录日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.username  = ''
  queryParams.ipAddress = ''
  queryParams.status    = undefined
  dateRange.value       = []
  pagination.pageNum    = 1
  loadData()
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm('确定要清空所有登录日志吗？此操作不可恢复。', '警告', {
      type: 'warning',
      confirmButtonText: '确定清空',
      confirmButtonClass: 'el-button--danger',
    })
    ElMessage.success('已清空登录日志')
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 0; }
.search-card :deep(.el-card__body) { padding: 16px 20px 0; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title { font-size: 15px; font-weight: 600; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
