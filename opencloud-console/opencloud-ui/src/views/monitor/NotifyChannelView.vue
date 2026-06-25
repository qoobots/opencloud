<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">通知渠道</span>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增渠道</el-button>
        </div>
      </template>

      <el-table :data="channelList" v-loading="loading" border stripe>
        <el-table-column prop="channelName" label="渠道名称" min-width="180" />
        <el-table-column label="渠道类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.channelType]" size="small">{{ row.channelType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" @click="handleTest(row)" :loading="testingMap[row.channelId]">测试</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="formData.channelName" placeholder="请输入渠道名称" />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="formData.channelType" style="width:100%" @change="onTypeChange">
            <el-option label="邮件 (EMAIL)" value="EMAIL" />
            <el-option label="钉钉 (DINGTALK)" value="DINGTALK" />
            <el-option label="企业微信 (WECHAT_WORK)" value="WECHAT_WORK" />
            <el-option label="自定义 Webhook" value="WEBHOOK" />
          </el-select>
        </el-form-item>

        <!-- 动态配置字段 -->
        <template v-if="formData.channelType === 'EMAIL'">
          <el-form-item label="收件人">
            <el-input v-model="formData.config.to" placeholder="多个邮箱用逗号分隔" />
          </el-form-item>
          <el-form-item label="SMTP Host">
            <el-input v-model="formData.config.smtpHost" placeholder="smtp.example.com" />
          </el-form-item>
          <el-form-item label="SMTP Port">
            <el-input v-model="formData.config.smtpPort" placeholder="465" />
          </el-form-item>
          <el-form-item label="发件人">
            <el-input v-model="formData.config.from" placeholder="noreply@example.com" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="formData.config.password" type="password" show-password placeholder="SMTP 密码" />
          </el-form-item>
        </template>

        <template v-else-if="formData.channelType === 'DINGTALK'">
          <el-form-item label="Webhook URL">
            <el-input v-model="formData.config.webhookUrl" placeholder="钉钉机器人 Webhook 地址" />
          </el-form-item>
          <el-form-item label="签名密钥">
            <el-input v-model="formData.config.secret" type="password" show-password placeholder="可选" />
          </el-form-item>
        </template>

        <template v-else-if="formData.channelType === 'WECHAT_WORK'">
          <el-form-item label="Webhook URL">
            <el-input v-model="formData.config.webhookUrl" placeholder="企业微信机器人 Webhook 地址" />
          </el-form-item>
        </template>

        <template v-else-if="formData.channelType === 'WEBHOOK'">
          <el-form-item label="URL">
            <el-input v-model="formData.config.url" placeholder="Webhook 地址" />
          </el-form-item>
          <el-form-item label="Header">
            <el-input v-model="formData.config.headers" placeholder='{"Authorization":"Bearer xxx"}' />
          </el-form-item>
        </template>

        <el-form-item label="启用">
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getNotifyChannelList, createNotifyChannel, updateNotifyChannel,
  deleteNotifyChannel, testNotifyChannel,
} from '@/api/monitor'
import type { NotifyChannelVO } from '@/api/monitor'

const channelList   = ref<NotifyChannelVO[]>([])
const loading       = ref(false)
const testingMap    = reactive<Record<number, boolean>>({})

const typeTagMap: Record<string, string> = {
  EMAIL: 'primary', DINGTALK: 'success', WECHAT_WORK: 'warning', WEBHOOK: 'info',
}

onMounted(fetchChannels)

async function fetchChannels() {
  loading.value = true
  try { channelList.value = await getNotifyChannelList() }
  finally { loading.value = false }
}

async function handleTest(row: NotifyChannelVO) {
  testingMap[row.channelId] = true
  try {
    await testNotifyChannel(row.channelId)
    ElMessage.success('测试消息已发送，请确认收到')
  } finally { testingMap[row.channelId] = false }
}

async function handleDelete(row: NotifyChannelVO) {
  await ElMessageBox.confirm(`确定删除渠道「${row.channelName}」？`, '警告', { type: 'warning' })
  await deleteNotifyChannel(row.channelId)
  ElMessage.success('删除成功')
  fetchChannels()
}

async function handleStatusChange(row: NotifyChannelVO) {
  await updateNotifyChannel(row.channelId, { channelName: row.channelName, channelType: row.channelType, config: row.config, enabled: row.enabled })
  ElMessage.success(`已${row.enabled ? '启用' : '停用'}`)
}

// ── Dialog ────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle   = ref('')
const editMode      = ref(false)
const submitLoading = ref(false)
const editChannelId = ref<number>()
const formRef       = ref<FormInstance>()

const formData = reactive({
  channelName: '',
  channelType: 'DINGTALK',
  config: {} as Record<string, string>,
  enabled: true,
})

const formRules: FormRules = {
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  channelType: [{ required: true }],
}

function onTypeChange() { formData.config = {} }

function openCreateDialog() {
  editMode.value = false
  dialogTitle.value = '新增通知渠道'
  Object.assign(formData, { channelName: '', channelType: 'DINGTALK', config: {}, enabled: true })
  dialogVisible.value = true
}

function openEditDialog(row: NotifyChannelVO) {
  editMode.value = true
  dialogTitle.value = '编辑通知渠道'
  editChannelId.value = row.channelId
  Object.assign(formData, {
    channelName: row.channelName,
    channelType: row.channelType,
    config: { ...row.config },
    enabled: row.enabled,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editMode.value) {
      await updateNotifyChannel(editChannelId.value!, formData)
    } else {
      await createNotifyChannel(formData)
    }
    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchChannels()
  } finally { submitLoading.value = false }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
</style>
