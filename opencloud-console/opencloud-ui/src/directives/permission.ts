import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 指令
 * 用法：
 *   v-permission="'system:user:add'"
 *   v-permission="['system:user:add', 'system:user:edit']"
 */
const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const userStore = useUserStore()

    // ADMIN 全放行
    if (userStore.hasRole('ROLE_ADMIN')) return

    const perms = Array.isArray(binding.value) ? binding.value : [binding.value]
    const hasPermission = perms.some(p => userStore.hasPermission(p))

    if (!hasPermission) {
      // 从 DOM 中移除元素
      el.parentNode?.removeChild(el)
    }
  },
}

export default permission
