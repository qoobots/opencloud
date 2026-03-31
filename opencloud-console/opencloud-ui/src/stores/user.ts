import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

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

  const isLoggedIn  = computed(() => !!accessToken.value)
  const roles       = computed(() => userInfo.value?.roles ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasPermission(perm: string) {
    return permissions.value.includes(perm)
  }

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
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch {}
    accessToken.value  = ''
    refreshToken.value = ''
    userInfo.value     = null
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

  return { accessToken, refreshToken, userInfo, isLoggedIn, roles, permissions,
           hasRole, hasPermission, login, logout, tryRefreshToken }
})
