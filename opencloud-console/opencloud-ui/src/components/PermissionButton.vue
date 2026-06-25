<template>
  <component :is="tag" v-if="show" v-bind="$attrs">
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const props = withDefaults(defineProps<{
  permission?: string | string[]
  role?: string | string[]
  tag?: string
}>(), {
  tag: 'span',
})

const userStore = useUserStore()

const show = computed(() => {
  // 角色 ADMIN 全部放行
  if (userStore.hasRole('ROLE_ADMIN')) return true

  // 权限码校验（任意满足一个即可）
  if (props.permission) {
    const perms = Array.isArray(props.permission) ? props.permission : [props.permission]
    const hasPerm = perms.some(p => userStore.hasPermission(p))
    if (!hasPerm) return false
  }

  // 角色校验
  if (props.role) {
    const roles = Array.isArray(props.role) ? props.role : [props.role]
    const hasRole = roles.some(r => userStore.hasRole(r))
    if (!hasRole) return false
  }

  return true
})
</script>
