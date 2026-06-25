<template>
  <div class="page-container">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span class="title">角色管理</span>
          <el-button type="primary" :icon="Plus" v-permission="'system:role:add'" @click="openCreateDialog">新增角色</el-button>
        </div>
      </template>

      <el-table :data="roleList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" v-permission="'system:role:edit'" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" :icon="Menu" v-permission="'system:role:edit'" @click="openPermDialog(row)">权限</el-button>
            <el-button link type="danger" :icon="Delete" v-permission="'system:role:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="如：ROLE_OPERATOR" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配 Dialog -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="520px" destroy-on-close>
      <div v-loading="permLoading">
        <el-tree
          ref="permTreeRef"
          :data="menuTree"
          :props="{ children: 'children', label: 'menuName' }"
          node-key="menuId"
          show-checkbox
          default-expand-all
          :default-checked-keys="checkedMenuIds"
          class="perm-tree"
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSavePerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Menu } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getRoleList, createRole, updateRole, deleteRole,
  getRolePermissions, assignPermissions, getMenuTree,
} from '@/api/system'
import type { RoleVO, MenuVO } from '@/api/system'

const loading = ref(false)
const roleList = ref<RoleVO[]>([])

async function fetchRoles() {
  loading.value = true
  try {
    roleList.value = await getRoleList()
  } finally {
    loading.value = false
  }
}

onMounted(fetchRoles)

// ── 新增/编辑 ─────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle   = ref('')
const editMode      = ref(false)
const submitLoading = ref(false)
const currentRoleId = ref<number>()
const formRef       = ref<FormInstance>()

const formData = reactive({ roleName: '', roleCode: '', description: '' })
const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function openCreateDialog() {
  editMode.value = false
  dialogTitle.value = '新增角色'
  Object.assign(formData, { roleName: '', roleCode: '', description: '' })
  dialogVisible.value = true
}

function openEditDialog(row: RoleVO) {
  editMode.value = true
  dialogTitle.value = '编辑角色'
  currentRoleId.value = row.roleId
  Object.assign(formData, { roleName: row.roleName, roleCode: row.roleCode, description: row.description })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editMode.value) {
      await updateRole(currentRoleId.value!, formData)
    } else {
      await createRole(formData)
    }
    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchRoles()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: RoleVO) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '警告', { type: 'warning' })
  await deleteRole(row.roleId)
  ElMessage.success('删除成功')
  fetchRoles()
}

// ── 权限树 ────────────────────────────────────────────────
const permDialogVisible = ref(false)
const permLoading       = ref(false)
const menuTree          = ref<MenuVO[]>([])
const checkedMenuIds    = ref<number[]>([])
const permTreeRef       = ref<InstanceType<typeof ElTree>>()
const permRoleId        = ref<number>()

async function openPermDialog(row: RoleVO) {
  permRoleId.value      = row.roleId
  permDialogVisible.value = true
  permLoading.value     = true
  try {
    const [tree, perms] = await Promise.all([getMenuTree(), getRolePermissions(row.roleId)])
    menuTree.value       = tree
    checkedMenuIds.value = perms
  } finally {
    permLoading.value = false
  }
}

async function handleSavePerms() {
  const checkedKeys = permTreeRef.value?.getCheckedKeys(false) as number[] ?? []
  const halfChecked = permTreeRef.value?.getHalfCheckedKeys() as number[] ?? []
  submitLoading.value = true
  try {
    await assignPermissions(permRoleId.value!, [...checkedKeys, ...halfChecked])
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
.perm-tree { max-height: 400px; overflow-y: auto; }
</style>
