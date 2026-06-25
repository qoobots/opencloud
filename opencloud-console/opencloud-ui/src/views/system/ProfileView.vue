<template>
  <div class="page-container">
    <el-row :gutter="24">
      <!-- 左侧头像 + 基本信息 -->
      <el-col :span="8">
        <el-card shadow="never">
          <div class="avatar-section">
            <el-avatar :size="100" :src="form.avatar" :icon="UserFilled" class="avatar" />
            <div class="name">{{ form.nickname || form.username }}</div>
            <div class="roles">
              <el-tag
                v-for="role in userStore.userInfo?.roles"
                :key="role"
                type="primary"
                size="small"
                style="margin:2px"
              >{{ role }}</el-tag>
            </div>
          </div>
          <el-divider />
          <div class="info-item">
            <el-icon><User /></el-icon>
            <span class="label">用户名：</span>
            <span>{{ form.username }}</span>
          </div>
          <div class="info-item">
            <el-icon><Message /></el-icon>
            <span class="label">邮箱：</span>
            <span>{{ form.email || '未设置' }}</span>
          </div>
          <div class="info-item">
            <el-icon><Phone /></el-icon>
            <span class="label">手机：</span>
            <span>{{ form.phone || '未设置' }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧编辑表单 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">编辑个人信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="90px"
            style="max-width:480px"
          >
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="30" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="头像地址" prop="avatar">
              <el-input v-model="form.avatar" placeholder="请输入头像图片 URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, User, Message, Phone } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getProfile, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef   = ref<FormInstance>()
const saving    = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email:    '',
  phone:    '',
  avatar:   '',
})

const rules: FormRules = {
  nickname: [{ max: 30, message: '昵称不超过 30 个字符', trigger: 'blur' }],
  email:    [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone:    [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
}

async function loadProfile() {
  try {
    const data = await getProfile()
    form.username = data.username
    form.nickname = data.nickname
    form.email    = data.email
    form.phone    = data.phone
    form.avatar   = data.avatar
  } catch {
    ElMessage.error('获取个人信息失败')
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      email:    form.email,
      phone:    form.phone,
      avatar:   form.avatar,
    })
    ElMessage.success('个人信息保存成功')
    // 同步更新本地 userInfo
    if (userStore.userInfo) {
      userStore.userInfo.nickname = form.nickname
      userStore.userInfo.avatar   = form.avatar
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    }
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function handleReset() {
  loadProfile()
}

onMounted(loadProfile)
</script>

<style scoped>
.page-container { padding: 0; }
.card-title { font-size: 15px; font-weight: 600; }

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0 10px;
  gap: 8px;
}
.avatar { border: 3px solid #e4e7ed; }
.name   { font-size: 18px; font-weight: 600; color: #303133; }
.roles  { display: flex; flex-wrap: wrap; justify-content: center; }

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  color: #606266;
  font-size: 14px;
  border-bottom: 1px solid #f5f7fa;
}
.info-item:last-child { border-bottom: none; }
.label { color: #909399; min-width: 48px; }
</style>
