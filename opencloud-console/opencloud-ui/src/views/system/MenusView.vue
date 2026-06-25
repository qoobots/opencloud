<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">菜单管理</span>
          <el-button type="primary" :icon="Plus" v-permission="'system:menu:add'" @click="openCreateDialog(0)">新增根菜单</el-button>
        </div>
      </template>

      <el-table
        :data="menuTree"
        v-loading="loading"
        border
        row-key="menuId"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200">
          <template #default="{ row }">
            <el-icon v-if="row.icon" style="vertical-align:middle;margin-right:6px"><component :is="row.icon" /></el-icon>
            {{ row.menuName }}
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTagMap[row.menuType]" size="small">{{ menuTypeMap[row.menuType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="component" label="组件路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="permission" label="权限标识" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="visible" label="显示" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 'BUTTON'" link type="primary" :icon="Plus" @click="openCreateDialog(row.menuId)">子菜单</el-button>
            <el-button link type="warning" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="父节点">
          <el-tree-select
            v-model="formData.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', value: 'menuId', children: 'children' }"
            :default-expand-all="true"
            placeholder="根节点"
            clearable
            check-strictly
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="formData.menuType">
            <el-radio value="DIRECTORY">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="formData.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'BUTTON'" label="路由路径">
          <el-input v-model="formData.path" placeholder="如：/system/users" />
        </el-form-item>
        <el-form-item v-if="formData.menuType === 'MENU'" label="组件路径">
          <el-input v-model="formData.component" placeholder="如：views/system/UsersView" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'DIRECTORY'" label="权限标识">
          <el-input v-model="formData.permission" placeholder="如：system:user:list" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'BUTTON'" label="图标">
          <el-input v-model="formData.icon" placeholder="Element Plus Icons 名称，如 Setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'BUTTON'" label="显示状态">
          <el-radio-group v-model="formData.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system'
import type { MenuVO } from '@/api/system'

const loading   = ref(false)
const menuTree  = ref<MenuVO[]>([])

async function fetchMenuTree() {
  loading.value = true
  try { menuTree.value = await getMenuTree() }
  finally { loading.value = false }
}

onMounted(fetchMenuTree)

const menuTypeMap: Record<string, string>    = { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮' }
const menuTypeTagMap: Record<string, string> = { DIRECTORY: '', MENU: 'success', BUTTON: 'info' }

// ── 树形 Select 数据（带根节点占位）─────────────────────
const menuTreeOptions = ref<any[]>([{ menuId: 0, menuName: '根节点', children: [] }])
onMounted(() => {
  menuTreeOptions.value[0].children = menuTree.value
})

// ── Dialog ────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle   = ref('')
const editMode      = ref(false)
const submitLoading = ref(false)
const editMenuId    = ref<number>()
const formRef       = ref<FormInstance>()

const formData = reactive({
  parentId: 0,
  menuName: '',
  menuType: 'MENU' as string,
  path: '',
  component: '',
  icon: '',
  permission: '',
  sortOrder: 0,
  visible: 1,
})

const formRules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true }],
}

function openCreateDialog(parentId: number) {
  editMode.value = false
  dialogTitle.value = '新增菜单'
  Object.assign(formData, { parentId, menuName: '', menuType: 'MENU', path: '', component: '', icon: '', permission: '', sortOrder: 0, visible: 1 })
  dialogVisible.value = true
}

function openEditDialog(row: MenuVO) {
  editMode.value = true
  dialogTitle.value = '编辑菜单'
  editMenuId.value = row.menuId
  Object.assign(formData, {
    parentId:   row.parentId,
    menuName:   row.menuName,
    menuType:   row.menuType,
    path:       row.path ?? '',
    component:  row.component ?? '',
    icon:       row.icon ?? '',
    permission: row.permission ?? '',
    sortOrder:  row.sortOrder,
    visible:    row.visible,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editMode.value) {
      await updateMenu(editMenuId.value!, formData)
    } else {
      await createMenu(formData)
    }
    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchMenuTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: MenuVO) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」？（含子菜单将一并删除）`, '警告', { type: 'warning' })
  await deleteMenu(row.menuId)
  ElMessage.success('删除成功')
  fetchMenuTree()
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 15px; font-weight: 600; }
</style>
