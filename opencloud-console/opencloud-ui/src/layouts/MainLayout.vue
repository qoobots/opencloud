<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-wrap">
        <img src="/favicon.svg" alt="logo" class="logo-icon" />
        <span v-if="!isCollapsed" class="logo-text">OpenCloud</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :router="true"
          background-color="#0f1724"
          text-color="#a0aec0"
          active-text-color="#60a5fa"
          class="side-menu"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="route.children?.length" :index="route.path">
              <template #title>
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="`/${route.path}/${child.path}`"
              >
                <span>{{ child.meta?.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <!-- 无子菜单 -->
            <el-menu-item v-else :index="route.path">
              <el-icon><component :is="route.meta?.icon" /></el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-main">
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <Fold v-if="!isCollapsed" /><Expand v-else />
          </el-icon>
          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
              :to="item.path"
            >{{ item.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 全屏 -->
          <el-tooltip content="全屏">
            <el-icon class="header-icon" @click="toggleFullscreen"><FullScreen /></el-icon>
          </el-tooltip>
          <!-- 用户头像菜单 -->
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>

      <el-footer class="layout-footer">
        OpenCloud Console © 2026 · v1.0.0
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Fold, Expand, FullScreen, UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route      = useRoute()
const router     = useRouter()
const userStore  = useUserStore()
const isCollapsed = ref(false)

// 侧边栏路由（从路由表动态生成）
const menuRoutes = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  return mainRoute?.children?.filter(r => !r.meta?.hidden) ?? []
})

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() =>
  route.matched
    .filter(r => r.meta?.title)
    .map(r => ({ title: r.meta.title as string, path: r.path }))
)

// 全屏切换
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 用户菜单命令
async function handleUserCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/system/profile')
  } else if (cmd === 'password') {
    router.push('/system/password')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; background: #f0f2f5; }

.layout-aside {
  background: #0f1724;
  transition: width 0.25s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}
.logo-icon  { width: 28px; height: 28px; }
.logo-text  { font-size: 16px; font-weight: 700; color: #60a5fa; white-space: nowrap; }

.side-menu  { border-right: none; flex: 1; }

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  height: 56px !important;
}

.header-left  { display: flex; align-items: center; gap: 16px; }
.header-right { display: flex; align-items: center; gap: 16px; }

.collapse-btn { font-size: 20px; cursor: pointer; color: #606266; }
.collapse-btn:hover { color: #409eff; }

.header-icon  { font-size: 18px; cursor: pointer; color: #606266; }
.header-icon:hover { color: #409eff; }

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username { font-size: 14px; color: #303133; }

.layout-content { padding: 20px; overflow-y: auto; }

.layout-footer {
  text-align: center;
  font-size: 12px;
  color: #909399;
  line-height: 48px;
  height: 48px !important;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active { transition: all 0.2s ease; }
.fade-slide-enter-from   { opacity: 0; transform: translateX(10px); }
.fade-slide-leave-to     { opacity: 0; transform: translateX(-10px); }
</style>
