<template>
  <div class="forbidden-wrap">
    <div class="code">403</div>
    <div class="title">无访问权限</div>
    <div class="desc">您没有权限访问此页面，请联系管理员分配相应权限。</div>
    <div class="actions">
      <el-button type="primary" @click="router.back()">返回上一页</el-button>
      <el-button @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.forbidden-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 12px;
  text-align: center;
}
.code  { font-size: 96px; font-weight: 800; color: #e4e7ed; line-height: 1; }
.title { font-size: 22px; font-weight: 600; color: #303133; }
.desc  { font-size: 14px; color: #909399; max-width: 360px; }
.actions { margin-top: 8px; display: flex; gap: 12px; }
</style>
