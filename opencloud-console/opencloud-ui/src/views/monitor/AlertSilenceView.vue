<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="告警名称">
          <el-input
            v-model="queryParams.alertName"
            placeholder="请输入告警名称"
            clearable
            style="width:180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.expired" placeholder="全部" clearable style="width:110px">
            <el-option label="生效中" :value="false" />
            <el-option label="已过期" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 + 表格 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            告警静默规则
            <el-tooltip content="静默规则生效期间，匹配的告警不会触发通知">
              <el-icon style="margin-left:4px;color:#909399;cursor:help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </span>
          <el-button type="primary" :icon="Plus" @click="openDialog()">新增静默规则</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="silenceId" label="ID" width="70" align="center" />
        <el-table-column prop="alertName" label="告警名称" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.alertName">{{ row.alertName }}</span>
            <el-tag v-else type="warning" size="small">匹配全部</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="匹配标签" min-width="200">
          <template #default="{ row }">
            <div v-if="Object.keys(row.matchLabels || {}).length">
              <el-tag
                v-for="(v, k) in row.matchLabels"
                :key="k"
                size="small"
                type="info"
                style="margin:2px"
              >{{ k }}="{{ v }}"</el-tag>
            </div>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="备注" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.expired ? 'info' : 'success'" size="small">
              {{ row.expired ? '已过期' : '生效中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startsAt" label="开始时间" width="175" />
        <el-table-column prop="endsAt"   label="结束时间" width="175" />
        <el-table-column prop="createdBy" label="创建人" width="110" show-overflow-tooltip />
        <el-table-column label="剩余时间" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.expired" class="text-muted">—</span>
            <span v-else class="remaining">{{ remaining(row.endsAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger"  size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑静默规则' : '新增静默规则'"
      width="580px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="告警名称" prop="alertName">
          <el-input
            v-model="form.alertName"
            placeholder="留空则匹配所有告警名称"
            clearable
          />
        </el-form-item>

        <!-- 匹配标签 -->
        <el-form-item label="匹配标签">
          <div class="labels-editor">
            <div
              v-for="(label, idx) in labelPairs"
              :key="idx"
              class="label-row"
            >
              <el-input
                v-model="label.key"
                placeholder="标签名 (key)"
                style="width:140px"
              />
              <span class="eq-sign">=</span>
              <el-input
                v-model="label.value"
                placeholder="标签值 (value)"
                style="flex:1"
              />
              <el-button
                :icon="Minus"
                circle
                size="small"
                type="danger"
                plain
                @click="removeLabelPair(idx)"
              />
            </div>
            <el-button :icon="Plus" size="small" @click="addLabelPair">添加标签</el-button>
          </div>
          <div class="form-tip">多个标签之间为 AND 关系，全部匹配才生效</div>
        </el-form-item>

        <el-form-item label="静默时段" prop="timeRange" required>
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="timeShortcuts"
            style="width:100%"
          />
        </el-form-item>

        <el-form-item label="备注说明" prop="comment">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="3"
            placeholder="请描述此静默规则的用途，例如：计划维护期间暂停告警通知"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '立即创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Minus, QuestionFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAlertSilencePage,
  createAlertSilence,
  updateAlertSilence,
  deleteAlertSilence,
} from '@/api/monitor'
import type { AlertSilenceVO } from '@/api/monitor'

// ─── 列表状态 ─────────────────────────────────────────────────
const loading   = ref(false)
const tableData = ref<AlertSilenceVO[]>([])

const queryParams = reactive<{ alertName: string; expired: boolean | undefined }>({
  alertName: '',
  expired:   undefined,
})
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await getAlertSilencePage({
      alertName: queryParams.alertName || undefined,
      expired:   queryParams.expired,
      pageNum:   pagination.pageNum,
      pageSize:  pagination.pageSize,
    })
    tableData.value  = res.records
    pagination.total = res.total
  } catch {
    ElMessage.error('加载静默规则失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}
function handleReset() {
  queryParams.alertName = ''
  queryParams.expired   = undefined
  pagination.pageNum    = 1
  loadData()
}

// ─── 剩余时间 ─────────────────────────────────────────────────
function remaining(endsAt: string): string {
  const diff = new Date(endsAt).getTime() - Date.now()
  if (diff <= 0) return '已过期'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (h >= 24) return `${Math.floor(h / 24)} 天`
  if (h > 0) return `${h} 小时 ${m} 分`
  return `${m} 分钟`
}

// ─── 对话框状态 ───────────────────────────────────────────────
const dialogVisible = ref(false)
const isEdit        = ref(false)
const saving        = ref(false)
const formRef       = ref<FormInstance>()
const editId        = ref<number>(0)

interface LabelPair { key: string; value: string }
const labelPairs = ref<LabelPair[]>([])

const form = reactive<{
  alertName: string
  comment:   string
  timeRange: string[]
}>({
  alertName: '',
  comment:   '',
  timeRange: [],
})

// 时间快捷选项
const timeShortcuts = [
  {
    text: '未来 1 小时',
    value: () => {
      const now = new Date()
      return [now, new Date(now.getTime() + 3600_000)]
    },
  },
  {
    text: '未来 4 小时',
    value: () => {
      const now = new Date()
      return [now, new Date(now.getTime() + 4 * 3600_000)]
    },
  },
  {
    text: '未来 24 小时',
    value: () => {
      const now = new Date()
      return [now, new Date(now.getTime() + 86400_000)]
    },
  },
  {
    text: '未来 7 天',
    value: () => {
      const now = new Date()
      return [now, new Date(now.getTime() + 7 * 86400_000)]
    },
  },
]

const rules: FormRules = {
  timeRange: [{ required: true, message: '请选择静默时段', trigger: 'change' }],
  comment:   [{ required: true, message: '请填写备注说明', trigger: 'blur' }],
}

function addLabelPair() {
  labelPairs.value.push({ key: '', value: '' })
}
function removeLabelPair(idx: number) {
  labelPairs.value.splice(idx, 1)
}

function openDialog(row?: AlertSilenceVO) {
  formRef.value?.clearValidate()
  if (row) {
    isEdit.value      = true
    editId.value      = row.silenceId
    form.alertName    = row.alertName ?? ''
    form.comment      = row.comment
    form.timeRange    = [row.startsAt, row.endsAt]
    labelPairs.value  = Object.entries(row.matchLabels || {}).map(([key, value]) => ({ key, value }))
  } else {
    isEdit.value      = false
    editId.value      = 0
    form.alertName    = ''
    form.comment      = ''
    form.timeRange    = []
    labelPairs.value  = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 构建标签 map
  const matchLabels: Record<string, string> = {}
  for (const p of labelPairs.value) {
    if (p.key.trim()) matchLabels[p.key.trim()] = p.value.trim()
  }

  const payload = {
    alertName:   form.alertName || undefined,
    matchLabels,
    comment:     form.comment,
    startsAt:    form.timeRange[0],
    endsAt:      form.timeRange[1],
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateAlertSilence(editId.value, payload)
      ElMessage.success('静默规则已更新')
    } else {
      await createAlertSilence(payload)
      ElMessage.success('静默规则已创建')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: AlertSilenceVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除静默规则「${row.alertName || '匹配全部'}」吗？删除后将立即恢复告警通知。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteAlertSilence(row.silenceId)
    ElMessage.success('已删除')
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
.card-title { font-size: 15px; font-weight: 600; display: flex; align-items: center; }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted { color: #c0c4cc; font-size: 13px; }
.remaining  { color: #67c23a; font-size: 13px; font-weight: 500; }

/* 标签编辑器 */
.labels-editor { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.label-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.eq-sign { color: #909399; font-size: 14px; padding: 0 2px; }
.form-tip { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
