<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset({})">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span class="title">用户列表</span>
          <el-button type="primary" :icon="Plus" v-permission="'system:user:add'" @click="openCreateDialog">新增用户</el-button>
        </div>
      </template>

      <el-table :data="state.list" v-loading="state.loading" border stripe row-key="userId">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roles"
              :key="role.roleId"
              size="small"
              style="margin-right:4px"
            >{{ role.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" v-permission="'system:user:edit'" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" :icon="Key" v-permission="'system:user:edit'" @click="openResetPwdDialog(row)">重置密码</el-button>
            <el-button link type="danger" :icon="Delete" v-permission="'system:user:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="state.pageNum"
          v-model:page-size="state.pageSize"
          :total="state.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="editMode" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!editMode" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option
              v-for="role in roleList"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 Dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="380px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password placeholder="请输入新密码（8~20位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getUserPage, createUser, updateUser, deleteUser, resetPassword, updateUserStatus, getRoleList,
} from '@/api/system'
import type { UserVO, RoleVO } from '@/api/system'
import { useTable } from '@/hooks/useTable'

// ── 表格 ──────────────────────────────────────────────────
const { state, queryParams, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  useTable<{ username?: string; nickname?: string; status?: number }, UserVO>({
    fetchFn: (p) => getUserPage(p),
  })

// ── 角色列表 ────────────────────────────────────────────
const roleList = ref<RoleVO[]>([])
onMounted(async () => {
  const res = await getRoleList()
  roleList.value = res
})

// ── Dialog ────────────────────────────────────────────────
const dialogVisible  = ref(false)
const editMode       = ref(false)
const dialogTitle    = ref('新增用户')
const submitLoading  = ref(false)
const formRef        = ref<FormInstance>()
const currentUserId  = ref<number>()

const formData = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 8, max: 20, message: '密码长度 8~20 位', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email:    [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function openCreateDialog() {
  editMode.value     = false
  dialogTitle.value  = '新增用户'
  dialogVisible.value = true
  Object.assign(formData, { username: '', password: '', nickname: '', email: '', phone: '', roleIds: [] })
}

function openEditDialog(row: UserVO) {
  editMode.value     = true
  dialogTitle.value  = '编辑用户'
  currentUserId.value = row.userId
  dialogVisible.value = true
  Object.assign(formData, {
    username: row.username,
    nickname: row.nickname,
    email:    row.email,
    phone:    row.phone,
    roleIds:  row.roles?.map(r => r.roleId) ?? [],
  })
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editMode.value) {
      await updateUser(currentUserId.value!, { nickname: formData.nickname, email: formData.email, phone: formData.phone })
    } else {
      await createUser({ ...formData })
    }
    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    handleSearch()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: UserVO) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '警告', { type: 'warning' })
  await deleteUser(row.userId)
  ElMessage.success('删除成功')
  handleSearch()
}

async function handleStatusChange(row: UserVO) {
  await updateUserStatus(row.userId, row.status)
  ElMessage.success(`已${row.status === 1 ? '启用' : '禁用'}`)
}

// ── 重置密码 ─────────────────────────────────────────────
const resetPwdVisible = ref(false)
const resetFormRef    = ref<FormInstance>()
const resetUserId     = ref<number>()
const resetForm       = reactive({ password: '' })
const resetRules: FormRules = {
  password: [{ required: true, min: 8, max: 20, message: '密码长度 8~20 位', trigger: 'blur' }],
}

function openResetPwdDialog(row: UserVO) {
  resetUserId.value   = row.userId
  resetForm.password  = ''
  resetPwdVisible.value = true
}

async function handleResetPwd() {
  await resetFormRef.value?.validate()
  submitLoading.value = true
  try {
    await resetPassword(resetUserId.value!, resetForm.password)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding: 16px 16px 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
