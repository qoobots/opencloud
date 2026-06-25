import request from '@/utils/request'

// ──────────────────────────────────────────────
// 用户管理
// ──────────────────────────────────────────────

export function getUserPage(params: UserPageReq) {
  return request.get<any, PageResult<UserVO>>('/system/users', { params })
}

export function getUserDetail(userId: number) {
  return request.get<any, UserVO>(`/system/users/${userId}`)
}

export function createUser(data: CreateUserReq) {
  return request.post<any, void>('/system/users', data)
}

export function updateUser(userId: number, data: UpdateUserReq) {
  return request.put<any, void>(`/system/users/${userId}`, data)
}

export function deleteUser(userId: number) {
  return request.delete<any, void>(`/system/users/${userId}`)
}

export function resetPassword(userId: number, password: string) {
  return request.put<any, void>(`/system/users/${userId}/password`, { password })
}

export function assignRoles(userId: number, roleIds: number[]) {
  return request.put<any, void>(`/system/users/${userId}/roles`, { roleIds })
}

export function updateUserStatus(userId: number, status: number) {
  return request.put<any, void>(`/system/users/${userId}/status`, { status })
}

// ──────────────────────────────────────────────
// 角色管理
// ──────────────────────────────────────────────

export function getRolePage(params: RolePageReq) {
  return request.get<any, PageResult<RoleVO>>('/system/roles', { params })
}

export function getRoleList() {
  return request.get<any, RoleVO[]>('/system/roles/all')
}

export function createRole(data: RoleFormReq) {
  return request.post<any, void>('/system/roles', data)
}

export function updateRole(roleId: number, data: RoleFormReq) {
  return request.put<any, void>(`/system/roles/${roleId}`, data)
}

export function deleteRole(roleId: number) {
  return request.delete<any, void>(`/system/roles/${roleId}`)
}

export function getRolePermissions(roleId: number) {
  return request.get<any, number[]>(`/system/roles/${roleId}/permissions`)
}

export function assignPermissions(roleId: number, menuIds: number[]) {
  return request.put<any, void>(`/system/roles/${roleId}/permissions`, { menuIds })
}

// ──────────────────────────────────────────────
// 菜单管理
// ──────────────────────────────────────────────

export function getMenuTree() {
  return request.get<any, MenuVO[]>('/system/menus/tree')
}

export function createMenu(data: MenuFormReq) {
  return request.post<any, void>('/system/menus', data)
}

export function updateMenu(menuId: number, data: MenuFormReq) {
  return request.put<any, void>(`/system/menus/${menuId}`, data)
}

export function deleteMenu(menuId: number) {
  return request.delete<any, void>(`/system/menus/${menuId}`)
}

// ──────────────────────────────────────────────
// 操作日志
// ──────────────────────────────────────────────

export function getOpLogPage(params: OpLogPageReq) {
  return request.get<any, PageResult<OpLogVO>>('/system/logs/operation', { params })
}

export function getOpLogDetail(logId: number) {
  return request.get<any, OpLogVO>(`/system/logs/operation/${logId}`)
}

export function deleteOpLog(logId: number) {
  return request.delete<any, void>(`/system/logs/operation/${logId}`)
}

export function getLoginLogPage(params: LoginLogPageReq) {
  return request.get<any, PageResult<LoginLogVO>>('/system/logs/login', { params })
}

// ──────────────────────────────────────────────
// Types
// ──────────────────────────────────────────────

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserVO {
  userId: number
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  avatar: string
  roles: RoleVO[]
  createTime: string
  updateTime: string
}

export interface CreateUserReq {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
  roleIds?: number[]
}

export interface UpdateUserReq {
  nickname?: string
  email?: string
  phone?: string
  status?: number
}

export interface UserPageReq {
  username?: string
  nickname?: string
  status?: number
  pageNum: number
  pageSize: number
}

export interface RoleVO {
  roleId: number
  roleName: string
  roleCode: string
  description: string
  status: number
  createTime: string
}

export interface RoleFormReq {
  roleName: string
  roleCode: string
  description?: string
  status?: number
}

export interface RolePageReq {
  roleName?: string
  roleCode?: string
  pageNum: number
  pageSize: number
}

export interface MenuVO {
  menuId: number
  parentId: number
  menuName: string
  menuType: string   // DIRECTORY | MENU | BUTTON
  path?: string
  component?: string
  icon?: string
  permission?: string
  sortOrder: number
  visible: number
  status: number
  children?: MenuVO[]
}

export interface MenuFormReq {
  parentId: number
  menuName: string
  menuType: string
  path?: string
  component?: string
  icon?: string
  permission?: string
  sortOrder?: number
  visible?: number
}

export interface OpLogVO {
  logId: number
  username: string
  module: string
  operation: string
  method: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  responseData: string
  ipAddress: string
  status: number
  errorMsg?: string
  costTime: number
  createTime: string
}

export interface OpLogPageReq {
  username?: string
  module?: string
  status?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export interface LoginLogVO {
  logId: number
  username: string
  ipAddress: string
  browser: string
  os: string
  status: number
  message: string
  loginTime: string
}

export interface LoginLogPageReq {
  username?: string
  ipAddress?: string
  status?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}
