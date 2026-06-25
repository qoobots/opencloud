import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

// 白名单（无需登录）
const WHITE_LIST = ['/login']

// ─── 静态基础路由 ─────────────────────────────────────────────
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/403',
    name: '403',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { title: '无权限', hidden: true },
  },
  // 404
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFoundView.vue') },
]

// ─── 所有业务路由（备用完整静态路由，动态路由降级使用）──────────
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '资源总览', icon: 'DataBoard', affix: true },
      },
      // ─── 云平台 ───────────────────────────────────────────
      {
        path: 'openstack',
        name: 'OpenStack',
        redirect: '/openstack/instances',
        meta: { title: 'OpenStack', icon: 'Cloud' },
        children: [
          { path: 'instances', name: 'OSInstances', component: () => import('@/views/openstack/InstancesView.vue'), meta: { title: '云主机' } },
          { path: 'networks',  name: 'OSNetworks',  component: () => import('@/views/openstack/NetworksView.vue'),  meta: { title: '网络' } },
          { path: 'images',    name: 'OSImages',    component: () => import('@/views/openstack/ImagesView.vue'),    meta: { title: '镜像' } },
          { path: 'volumes',   name: 'OSVolumes',   component: () => import('@/views/openstack/VolumesView.vue'),   meta: { title: '云硬盘' } },
        ],
      },
      // ─── Ceph ─────────────────────────────────────────────
      {
        path: 'ceph',
        name: 'Ceph',
        redirect: '/ceph/overview',
        meta: { title: 'Ceph 存储', icon: 'FolderOpened' },
        children: [
          { path: 'overview', name: 'CephOverview', component: () => import('@/views/ceph/CephOverviewView.vue'), meta: { title: '存储概览' } },
          { path: 'pools',    name: 'CephPools',    component: () => import('@/views/ceph/PoolsView.vue'),        meta: { title: '存储池' } },
          { path: 'volumes',  name: 'CephVolumes',  component: () => import('@/views/ceph/VolumesView.vue'),      meta: { title: 'RBD 卷' } },
        ],
      },
      // ─── Kubernetes ────────────────────────────────────────
      {
        path: 'kubernetes',
        name: 'Kubernetes',
        redirect: '/kubernetes/workloads',
        meta: { title: 'Kubernetes', icon: 'SetUp' },
        children: [
          { path: 'workloads', name: 'K8sWorkloads', component: () => import('@/views/kubernetes/WorkloadsView.vue'), meta: { title: '工作负载' } },
          { path: 'services',  name: 'K8sServices',  component: () => import('@/views/kubernetes/ServicesView.vue'),  meta: { title: '服务/网络' } },
          { path: 'configs',   name: 'K8sConfigs',   component: () => import('@/views/kubernetes/ConfigsView.vue'),   meta: { title: '配置管理' } },
          { path: 'nodes',     name: 'K8sNodes',     component: () => import('@/views/kubernetes/NodesView.vue'),     meta: { title: '节点管理' } },
        ],
      },
      // ─── 监控告警 ─────────────────────────────────────────
      {
        path: 'monitor',
        name: 'Monitor',
        redirect: '/monitor/dashboard',
        meta: { title: '监控告警', icon: 'Monitor' },
        children: [
          { path: 'dashboard', name: 'MonitorDash',    component: () => import('@/views/monitor/GrafanaDashboard.vue'),   meta: { title: 'Grafana 面板' } },
          { path: 'alerts',    name: 'AlertList',      component: () => import('@/views/monitor/AlertListView.vue'),      meta: { title: '告警列表' } },
          { path: 'rules',     name: 'AlertRules',     component: () => import('@/views/monitor/AlertRulesView.vue'),     meta: { title: '告警规则' } },
          { path: 'notify',    name: 'NotifyChannel',  component: () => import('@/views/monitor/NotifyChannelView.vue'),  meta: { title: '通知渠道' } },
          { path: 'explore',   name: 'MetricExplore',  component: () => import('@/views/monitor/MetricExploreView.vue'),  meta: { title: '指标探索' } },
          { path: 'silences',  name: 'AlertSilences',  component: () => import('@/views/monitor/AlertSilenceView.vue'),   meta: { title: '静默规则' } },
        ],
      },
      // ─── 系统管理 ─────────────────────────────────────────
      {
        path: 'system',
        name: 'System',
        redirect: '/system/users',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          { path: 'users',      name: 'SysUsers',    component: () => import('@/views/system/UsersView.vue'),     meta: { title: '用户管理', permission: 'system:user:list' } },
          { path: 'roles',      name: 'SysRoles',    component: () => import('@/views/system/RolesView.vue'),     meta: { title: '角色管理', permission: 'system:role:list' } },
          { path: 'menus',      name: 'SysMenus',    component: () => import('@/views/system/MenusView.vue'),     meta: { title: '菜单管理', permission: 'system:menu:list' } },
          { path: 'op-logs',    name: 'OpLogs',      component: () => import('@/views/system/OpLogsView.vue'),    meta: { title: '操作日志', permission: 'system:log:list' } },
          { path: 'login-logs', name: 'LoginLogs',   component: () => import('@/views/system/LoginLogsView.vue'), meta: { title: '登录日志', permission: 'system:log:list' } },
        ],
      },
      // ─── 个人中心 ─────────────────────────────────────────
      {
        path: 'system/profile',
        name: 'Profile',
        component: () => import('@/views/system/ProfileView.vue'),
        meta: { title: '个人信息', hidden: true },
      },
      {
        path: 'system/password',
        name: 'ChangePassword',
        component: () => import('@/views/system/PasswordView.vue'),
        meta: { title: '修改密码', hidden: true },
      },
    ],
  },
]

// ─── 组件名到动态 import 的映射表 ─────────────────────────────
const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveComponent(component: string) {
  const key = `/src/views/${component}.vue`
  return viewModules[key] ?? (() => import('@/views/error/NotFoundView.vue'))
}

// ─── 将后端菜单树转换为 RouteRecordRaw[] ─────────────────────
export function menuToRoutes(menus: any[]): RouteRecordRaw[] {
  return menus
    .filter((m) => m.menuType !== 'BUTTON')
    .map((m) => {
      const route: RouteRecordRaw = {
        path: m.path || m.menuId.toString(),
        name: m.menuName + '_' + m.menuId,
        meta: {
          title: m.menuName,
          icon: m.icon,
          permission: m.permission,
          menuId: m.menuId,
          parentId: m.parentId,
          visible: m.visible,
          hidden: m.visible === 0,
        },
        component: m.component
          ? resolveComponent(m.component)
          : () => import('@/layouts/MainLayout.vue'),
      }
      if (m.children?.length) {
        route.children = menuToRoutes(m.children)
      }
      return route
    })
}

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

// ─── 路由守卫 ─────────────────────────────────────────────────
router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()

  // 白名单直接放行
  if (WHITE_LIST.includes(to.path)) {
    next()
    return
  }

  // 未登录跳转
  if (!userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 动态路由：未加载则先加载
  if (!userStore.routesLoaded) {
    await userStore.loadDynamicRoutes(router)
    // 重新导航，避免匹配空
    next({ ...to, replace: true })
    return
  }

  // 权限校验
  const requiredPerm = to.meta.permission as string | undefined
  if (requiredPerm && !userStore.hasPermission(requiredPerm) && !userStore.hasRole('ROLE_ADMIN')) {
    next({ path: '/403' })
    return
  }

  document.title = `${to.meta.title ?? ''} - OpenCloud 控制台`
  next()
})

router.afterEach(() => NProgress.done())

export default router
