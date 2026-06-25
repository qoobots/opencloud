<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <MetricCard label="总告警" :value="stats?.total ?? 0" icon="Bell" color="blue" />
      <MetricCard label="严重" :value="stats?.critical ?? 0" icon="Warning" color="red" />
      <MetricCard label="警告" :value="stats?.warning ?? 0" icon="WarningFilled" color="orange" />
      <MetricCard label="触发中" :value="stats?.firing ?? 0" icon="AlarmClock" color="red" />
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="告警名称">
          <el-input v-model="queryParams.alertName" placeholder="告警名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="queryParams.severity" clearable placeholder="全部" style="width:110px">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width:120px">
            <el-option label="触发中" value="FIRING" />
            <el-option label="已恢复" value="RESOLVED" />
            <el-option label="已确认" value="ACKNOWLEDGED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset({})">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 实时推送条 -->
    <el-alert
      v-if="wsConnected"
      title="WebSocket 实时推送已连接，告警将自动刷新"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom:-8px"
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">告警记录</span>
          <el-button :icon="Refresh" size="small" @click="handleSearch">手动刷新</el-button>
        </div>
      </template>

      <el-table :data="state.list" v-loading="state.loading" border stripe row-class-name="alert-row">
        <el-table-column prop="alertName" label="告警名称" min-width="200" />
        <el-table-column label="级别" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small" effect="dark">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status.toLowerCase()" :label="statusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="startsAt" label="触发时间" width="170" />
        <el-table-column prop="endsAt" label="恢复时间" width="170">
          <template #default="{ row }">{{ row.endsAt || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'FIRING'" link type="primary" @click="openAckDialog(row)">确认</el-button>
            <el-button link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="state.pageNum"
          v-model:page-size="state.pageSize"
          :total="state.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 确认告警 Dialog -->
    <el-dialog v-model="ackDialogVisible" title="确认告警" width="420px" destroy-on-close>
      <el-form :model="ackForm" label-width="80px">
        <el-form-item label="确认意见">
          <el-input v-model="ackForm.comment" type="textarea" :rows="4" placeholder="可选：填写处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ackLoading" @click="handleAck">确认</el-button>
      </template>
    </el-dialog>

    <!-- 详情 Dialog -->
    <el-dialog v-model="detailDialogVisible" title="告警详情" width="680px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentAlert">
        <el-descriptions-item label="告警名称" :span="2">{{ currentAlert.alertName }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="severityType(currentAlert.severity)" size="small" effect="dark">{{ currentAlert.severity }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusBadge :status="currentAlert.status.toLowerCase()" :label="statusLabel(currentAlert.status)" />
        </el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ currentAlert.startsAt }}</el-descriptions-item>
        <el-descriptions-item label="恢复时间">{{ currentAlert.endsAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ currentAlert.summary }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentAlert.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <el-tag v-for="(v, k) in currentAlert.labels" :key="k" size="small" style="margin-right:4px;margin-bottom:2px">{{ k }}={{ v }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.acknowledgedBy" label="确认人">{{ currentAlert.acknowledgedBy }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.acknowledgedAt" label="确认时间">{{ currentAlert.acknowledgedAt }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.acknowledgeComment" label="确认意见" :span="2">{{ currentAlert.acknowledgeComment }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAlertRecordPage, getAlertStats, ackAlertRecord } from '@/api/monitor'
import type { AlertRecordVO, AlertStatsVO } from '@/api/monitor'
import { useTable } from '@/hooks/useTable'
import { useWebSocket } from '@/hooks/useWebSocket'
import StatusBadge from '@/components/StatusBadge.vue'
import MetricCard from '@/components/MetricCard.vue'

const stats = ref<AlertStatsVO | null>(null)

const { state, queryParams, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  useTable<{ alertName?: string; severity?: string; status?: string }, AlertRecordVO>({
    fetchFn: (p) => getAlertRecordPage(p),
  })

onMounted(async () => {
  stats.value = await getAlertStats()
})

// ── WebSocket ─────────────────────────────────────────────
const { connected: wsConnected } = useWebSocket({
  path: '/ws/alerts',
  onMessage: (data) => {
    if (data.type === 'ALERT') {
      handleSearch()
      getAlertStats().then(s => { stats.value = s })
    }
  },
})

// ── 告警确认 ─────────────────────────────────────────────
const ackDialogVisible = ref(false)
const ackLoading       = ref(false)
const ackRecordId      = ref<number>()
const ackForm          = reactive({ comment: '' })

function openAckDialog(row: AlertRecordVO) {
  ackRecordId.value    = row.recordId
  ackForm.comment      = ''
  ackDialogVisible.value = true
}

async function handleAck() {
  ackLoading.value = true
  try {
    await ackAlertRecord(ackRecordId.value!, ackForm.comment || undefined)
    ElMessage.success('告警已确认')
    ackDialogVisible.value = false
    handleSearch()
  } finally { ackLoading.value = false }
}

// ── 告警详情 ─────────────────────────────────────────────
const detailDialogVisible = ref(false)
const currentAlert        = ref<AlertRecordVO | null>(null)

function openDetail(row: AlertRecordVO) {
  currentAlert.value       = row
  detailDialogVisible.value = true
}

function severityType(s: string) {
  return s === 'CRITICAL' ? 'danger' : s === 'WARNING' ? 'warning' : 'info'
}

function statusLabel(s: string) {
  return { FIRING: '触发中', RESOLVED: '已恢复', ACKNOWLEDGED: '已确认' }[s] ?? s
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.search-card :deep(.el-card__body) { padding: 16px 16px 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
