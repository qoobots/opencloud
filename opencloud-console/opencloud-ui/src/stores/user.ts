import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Router } from 'vue-router'
import request from '@/utils/request'
import { getMenuTree } from '@/api/system'
import { asyncRoutes, menuToRoutes } from '@/router/index'

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}

export const useUserStore = defineStore('user', () => {
  const accessToken  = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo     = ref<UserInfo | null>(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  // 动态路由状态
  const routesLoaded = ref<boolean>(false)
  const menus        = ref<any[]>([])

  const isLoggedIn  = computed(() => !!accessToken.value)
  const roles       = computed(() => userInfo.value?.roles ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasPermission(perm: string) {
    return permissions.value.includes(perm)
  }

  /** 登录：获取 token + 用户信息，但不加载路由（路由守卫触发时再加载） */
  async function login(username: string, password: string) {
    const res: any = await request.post('/auth/login', { username, password })
    accessToken.value  = res.accessToken
    refreshToken.value = res.refreshToken
    userInfo.value = {
      userId:      res.userId,
      username:    res.username,
      nickname:    res.nickname,
      avatar:      res.avatar,
      roles:       res.roles,
      permissions: res.permissions,
    }
    localStorage.setItem('accessToken',  res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    localStorage.setItem('userInfo',     JSON.stringify(userInfo.value))
    // 重置路由加载标志，下次守卫触发时会重新加载
    routesLoaded.value = false
  }

  /** 登出 */
  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch {}
    accessToken.value  = ''
    refreshToken.value = ''
    userInfo.value     = null
    routesLoaded.value = false
    menus.value        = []
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  async function tryRefreshToken(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const res: any = await request.post(`/auth/refresh?refreshToken=${refreshToken.value}`)
      accessToken.value = res.accessToken
      localStorage.setItem('accessToken', res.accessToken)
      return true
    } catch {
      return false
    }
  }

  /**
   * 加载动态路由：
   * 1. 从后端获取菜单树
   * 2. 将菜单树转为路由并注册到 router
   * 3. 若接口失败，降级使用完整静态路由
   */
  async function loadDynamicRoutes(router: Router) {
    if (routesLoaded.value) return

    try {
      // 尝试从后端获取菜单
      const menuTree = await getMenuTree()
      menus.value = menuTree

      if (menuTree && menuTree.length > 0) {
        // 将后端菜单转为路由，动态注册到布局路由下
        const dynamicChildren = menuToRoutes(menuTree)
        // 注册到根布局路由
        for (const child of dynamicChildren) {
          router.addRoute('/', child)
        }
      } else {
        // 菜单为空，回退到完整静态路由
        _addAsyncRoutes(router)
      }
    } catch {
      // 接口失败，降级使用内置静态路由
      _addAsyncRoutes(router)
    }

    routesLoaded.value = true
  }

  /** 降级：将 asyncRoutes 全部注册 */
  function _addAsyncRoutes(router: Router) {
    for (const route of asyncRoutes) {
      if (!router.hasRoute(route.name as string)) {
        router.addRoute(route)
      }
    }
  }

  return {
    accessToken, refreshToken, userInfo, isLoggedIn,
    roles, permissions, routesLoaded, menus,
    hasRole, hasPermission,
    login, logout, tryRefreshToken, loadDynamicRoutes,
  }
})
