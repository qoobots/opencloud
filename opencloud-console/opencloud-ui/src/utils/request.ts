import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 响应结构
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

// ─── 请求拦截器 ───────────────────────────────────────────────
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers['Authorization'] = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ─── 响应拦截器 ───────────────────────────────────────────────
request.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data
    if (res.code === 200) {
      return res.data as any
    }
    // 业务错误
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message))
  },
  async (error) => {
    const status = error.response?.status
    if (status === 401) {
      // Token 过期，尝试刷新
      const userStore = useUserStore()
      const refreshed = await userStore.tryRefreshToken()
      if (refreshed) {
        // 重试原请求
        error.config.headers['Authorization'] = `Bearer ${userStore.accessToken}`
        return request(error.config)
      } else {
        ElMessage.error('登录已过期，请重新登录')
        await userStore.logout()
        router.push('/login')
      }
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else if (status === 500) {
      ElMessage.error('服务器内部错误')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
