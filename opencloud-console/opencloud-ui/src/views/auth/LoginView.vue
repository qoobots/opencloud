<template>
  <div class="login-page">
    <!-- 背景动效粒子 -->
    <div class="bg-grid"></div>

    <div class="login-card">
      <!-- Logo -->
      <div class="login-header">
        <div class="login-logo">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <circle cx="20" cy="20" r="20" fill="url(#grad)"/>
            <path d="M12 24 L20 12 L28 24 M16 24 L24 24" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="grad" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#3b82f6"/>
                <stop offset="100%" stop-color="#6366f1"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="login-title">OpenCloud 控制台</h1>
        <p class="login-subtitle">云计算平台统一管理入口</p>
      </div>

      <!-- 表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-tips">默认账号：admin / Admin@123</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router    = useRouter()
const route     = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码',   trigger: 'blur' }],
}

async function handleLogin() {
  if (!await formRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    // 错误已在 request 拦截器弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0f172a 100%);
  position: relative;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(96,165,250,.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(96,165,250,.04) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.login-card {
  width: 420px;
  background: rgba(255,255,255,.03);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 25px 50px rgba(0,0,0,.4);
  position: relative;
  z-index: 1;
}

.login-header   { text-align: center; margin-bottom: 36px; }
.login-logo     { margin-bottom: 16px; }
.login-title    { font-size: 22px; font-weight: 700; color: #f1f5f9; margin: 0 0 8px; }
.login-subtitle { font-size: 13px; color: #64748b; margin: 0; }

:deep(.el-input__wrapper) {
  background: rgba(255,255,255,.05) !important;
  border: 1px solid rgba(255,255,255,.1) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 2px rgba(59,130,246,.2) !important;
}
:deep(.el-input__inner) { color: #f1f5f9 !important; }
:deep(.el-input__inner::placeholder) { color: #475569 !important; }

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
  letter-spacing: 2px;
  transition: all .25s ease;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(59,130,246,.4);
}

.login-tips {
  text-align: center;
  font-size: 12px;
  color: #475569;
  margin: 0;
}
</style>
