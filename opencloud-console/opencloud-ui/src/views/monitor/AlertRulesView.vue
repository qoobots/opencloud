<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="规则名称">
          <el-input v-model="queryParams.ruleName" placeholder="规则名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="queryParams.severity" clearable placeholder="全部" style="width:110px">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset({})">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">告警规则</span>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增规则</el-button>
        </div>
      </template>

      <el-table :data="state.list" v-loading="state.loading" border stripe>
        <el-table-column prop="ruleName" label="规则名称" min-width="200" />
        <el-table-column label="级别" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expr" label="PromQL 表达式" min-width="260" show-overflow-tooltip />
        <el-table-column prop="duration" label="持续时间" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="(v: boolean) => handleToggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="PromQL 表达式" prop="expr">
          <el-input v-model="formData.expr" type="textarea" :rows="3" placeholder="如：node_cpu_seconds_total{mode='idle'} < 0.1" />
        </el-form-item>
        <el-form-item label="告警级别" prop="severity">
          <el-radio-group v-model="formData.severity">
            <el-radio value="CRITICAL">严重</el-radio>
            <el-radio value="WARNING">警告</el-radio>
            <el-radio value="INFO">信息</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="持续时间">
          <el-input v-model="formData.duration" placeholder="如：5m" style="width:160px" />
          <span style="color:#909399;margin-left:8px;font-size:12px">如: 5m、1h</span>
        </el-form-item>
        <el-form-item label="告警摘要">
          <el-input v-model="formData.summary" placeholder="可使用 {{ .Labels.xxx }} 模板变量" />
        </el-form-item>
        <el-form-item label="告警描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="详细描述" />
        </el-form-item>
        <el-form-item label="通知渠道">
          <el-select v-model="formData.channelIds" multiple placeholder="选择通知渠道" style="width:100%">
            <el-option
              v-for="ch in channelList"
              :key="ch.channelId"
              :label="`${ch.channelName}（${ch.channelType}）`"
              :value="ch.channelId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAlertRulePage, createAlertRule, updateAlertRule, deleteAlertRule, updateAlertRuleStatus,
  getNotifyChannelList,
} from '@/api/monitor'
import type { AlertRuleVO, NotifyChannelVO } from '@/api/monitor'
import { useTable } from '@/hooks/useTable'

const { state, queryParams, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  useTable<{ ruleName?: string; severity?: string }, AlertRuleVO>({
    fetchFn: (p) => getAlertRulePage(p),
  })

const channelList = ref<NotifyChannelVO[]>([])
onMounted(async () => { channelList.value = await getNotifyChannelList() })

// ── Dialog ────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle   = ref('')
const editMode      = ref(false)
const submitLoading = ref(false)
const editRuleId    = ref<number>()
const formRef       = ref<FormInstance>()

const formData = reactive({
  ruleName: '', expr: '', severity: 'WARNING', duration: '5m',
  summary: '', description: '', channelIds: [] as number[], enabled: true,
})

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  expr:     [{ required: true, message: '请输入 PromQL 表达式', trigger: 'blur' }],
  severity: [{ required: true }],
}

function openCreateDialog() {
  editMode.value = false
  dialogTitle.value = '新增告警规则'
  Object.assign(formData, { ruleName: '', expr: '', severity: 'WARNING', duration: '5m', summary: '', description: '', channelIds: [], enabled: true })
  dialogVisible.value = true
}

function openEditDialog(row: AlertRuleVO) {
  editMode.value = true
  dialogTitle.value = '编辑告警规则'
  editRuleId.value = row.ruleId
  Object.assign(formData, {
    ruleName: row.ruleName, expr: row.expr, severity: row.severity,
    duration: row.duration, summary: row.summary ?? '', description: row.description ?? '',
    channelIds: row.channelIds ?? [], enabled: row.enabled,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editMode.value) {
      await updateAlertRule(editRuleId.value!, formData)
    } else {
      await createAlertRule(formData)
    }
    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    handleSearch()
  } finally { submitLoading.value = false }
}

async function handleDelete(row: AlertRuleVO) {
  await ElMessageBox.confirm(`确定删除告警规则「${row.ruleName}」？`, '警告', { type: 'warning' })
  await deleteAlertRule(row.ruleId)
  ElMessage.success('删除成功')
  handleSearch()
}

async function handleToggleStatus(row: AlertRuleVO, enabled: boolean) {
  await updateAlertRuleStatus(row.ruleId, enabled)
  ElMessage.success(`规则已${enabled ? '启用' : '停用'}`)
}

function severityType(s: string) {
  return s === 'CRITICAL' ? 'danger' : s === 'WARNING' ? 'warning' : 'info'
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 16px 16px 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
