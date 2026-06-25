import request from '@/utils/request'

// ──────────────────────────────────────────────
// Auth API
// ──────────────────────────────────────────────

export function login(data: LoginReq) {
  return request.post<any, LoginRes>('/auth/login', data)
}

export function logout() {
  return request.post<any, void>('/auth/logout')
}

export function refreshToken(token: string) {
  return request.post<any, { accessToken: string }>(`/auth/refresh?refreshToken=${token}`)
}

export function getProfile() {
  return request.get<any, UserProfile>('/auth/profile')
}

export function updateProfile(data: UpdateProfileReq) {
  return request.put<any, void>('/auth/profile', data)
}

export function changePassword(data: ChangePasswordReq) {
  return request.put<any, void>('/auth/password', data)
}

// ──────────────────────────────────────────────
// Types
// ──────────────────────────────────────────────

export interface LoginReq {
  username: string
  password: string
  tenantId?: number
}

export interface LoginRes {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}

export interface UserProfile {
  userId: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  roles: string[]
}

export interface UpdateProfileReq {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

export interface ChangePasswordReq {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
