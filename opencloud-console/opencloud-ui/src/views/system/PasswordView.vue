<template>
  <div class="page-container">
    <el-row justify="center">
      <el-col :xs="24" :sm="18" :md="12" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <el-icon size="18"><Lock /></el-icon>
              <span class="card-title">修改密码</span>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            status-icon
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="form.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
                autocomplete="current-password"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                placeholder="8~20 位，包含字母和数字"
                show-password
                autocomplete="new-password"
              />
              <!-- 密码强度 -->
              <div class="strength-bar">
                <div
                  v-for="n in 4"
                  :key="n"
                  class="strength-block"
                  :class="strengthClass(n)"
                />
                <span class="strength-label">{{ strengthLabel }}</span>
              </div>
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" style="width:120px" @click="handleSubmit">
                确认修改
              </el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-alert type="warning" :closable="false" style="margin-top:16px">
            <template #title>
              密码要求：长度 8-20 位，必须包含字母和数字，不允许使用与当前密码相同的密码。
            </template>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router    = useRouter()
const userStore = useUserStore()
const formRef   = ref<FormInstance>()
const saving    = ref(false)

const form = reactive({
  oldPassword:     '',
  newPassword:     '',
  confirmPassword: '',
})

// ─── 密码强度 ──────────────────────────────────────────────────
const strengthLevel = computed(() => {
  const pw = form.newPassword
  if (!pw) return 0
  let score = 0
  if (pw.length >= 8)              score++
  if (/[a-z]/.test(pw))           score++
  if (/[A-Z]/.test(pw))           score++
  if (/\d/.test(pw))              score++
  if (/[^a-zA-Z\d]/.test(pw))     score++
  return Math.min(4, score)
})

const strengthLabel = computed(() => {
  const labels = ['', '弱', '一般', '较强', '强']
  return labels[strengthLevel.value]
})

function strengthClass(n: number) {
  if (!strengthLevel.value || n > strengthLevel.value) return ''
  if (strengthLevel.value === 1) return 'weak'
  if (strengthLevel.value === 2) return 'fair'
  if (strengthLevel.value === 3) return 'good'
  return 'strong'
}

// ─── 表单校验 ──────────────────────────────────────────────────
const validateConfirm = (_rule: any, value: string, cb: Function) => {
  if (value !== form.newPassword) {
    cb(new Error('两次输入的密码不一致'))
  } else {
    cb()
  }
}

const rules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度 8-20 位', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await changePassword({
      oldPassword:     form.oldPassword,
      newPassword:     form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } catch {
    ElMessage.error('密码修改失败，请检查当前密码是否正确')
  } finally {
    saving.value = false
  }
}

function handleReset() {
  form.oldPassword     = ''
  form.newPassword     = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.page-container { padding: 0; }
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-title { font-size: 15px; font-weight: 600; }

/* 密码强度条 */
.strength-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}
.strength-block {
  width: 48px;
  height: 4px;
  border-radius: 2px;
  background: #e4e7ed;
  transition: background 0.3s;
}
.strength-block.weak   { background: #f56c6c; }
.strength-block.fair   { background: #e6a23c; }
.strength-block.good   { background: #409eff; }
.strength-block.strong { background: #67c23a; }
.strength-label {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
</style>
