<template>
  <div class="not-found">
    <div class="content">
      <div class="code">404</div>
      <div class="title">页面不存在</div>
      <div class="desc">抱歉，您访问的页面不存在或已被移除。</div>
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #0f1724 0%, #1a2744 100%);
}

.content { text-align: center; }

.code {
  font-size: 120px;
  font-weight: 900;
  color: transparent;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
}

.title { font-size: 28px; color: #e2e8f0; font-weight: 600; margin-bottom: 12px; }
.desc  { font-size: 15px; color: #94a3b8; margin-bottom: 32px; }
</style>
