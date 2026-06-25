<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="操作用户">
          <el-input v-model="queryParams.username" placeholder="用户名" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="queryParams.module" placeholder="业务模块" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset({})">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span class="title">操作日志</span>
        </div>
      </template>

      <el-table :data="state.list" v-loading="state.loading" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="operation" label="操作描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="请求 URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="httpMethodColor(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="right" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="state.pageNum"
          v-model:page-size="state.pageSize"
          :total="state.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 日志详情 Dialog -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="720px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作人">{{ currentLog?.username }}</el-descriptions-item>
        <el-descriptions-item label="业务模块">{{ currentLog?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog?.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ currentLog?.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ currentLog?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="请求 URL" :span="2">{{ currentLog?.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog?.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog?.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ currentLog?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-pre">{{ formatJson(currentLog?.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog?.errorMsg" label="错误信息" :span="2">
          <span class="error-msg">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOpLogPage } from '@/api/system'
import type { OpLogVO } from '@/api/system'
import { useTable } from '@/hooks/useTable'

const timeRange = ref<[string, string] | null>(null)

const { state, queryParams, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  useTable<{ username?: string; module?: string; status?: number; startTime?: string; endTime?: string }, OpLogVO>({
    fetchFn: (p) => getOpLogPage(p),
  })

// 时间范围同步到 queryParams
watch(timeRange, (val) => {
  queryParams.startTime = val?.[0]
  queryParams.endTime   = val?.[1]
})

// ── 详情 ──────────────────────────────────────────────────
const detailVisible = ref(false)
const currentLog    = ref<OpLogVO | null>(null)

function openDetail(row: OpLogVO) {
  currentLog.value    = row
  detailVisible.value = true
}

function formatJson(str?: string) {
  try { return JSON.stringify(JSON.parse(str ?? ''), null, 2) }
  catch { return str }
}

function httpMethodColor(method: string) {
  const map: Record<string, string> = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger', PATCH: 'warning' }
  return map[method] ?? 'info'
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 16px 16px 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.json-pre { margin: 0; font-size: 12px; white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow-y: auto; }
.error-msg { color: #f56c6c; font-size: 13px; }
</style>
