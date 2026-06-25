<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <el-select v-model="selectedClusterId" placeholder="选择集群" style="width:220px" @change="fetchAll">
        <el-option v-for="c in clusterList" :key="c.clusterId" :label="c.clusterName" :value="c.clusterId" />
      </el-select>
    </el-card>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 网络 -->
      <el-tab-pane label="VPC 网络" name="networks">
        <el-table :data="networks" v-loading="loadingMap.networks" border stripe>
          <el-table-column prop="name" label="网络名称" min-width="180" />
          <el-table-column prop="id" label="网络 ID" min-width="280" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="shared" label="共享" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.shared ? 'success' : 'info'" size="small">{{ row.shared ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 子网 -->
      <el-tab-pane label="子网" name="subnets">
        <el-table :data="subnets" v-loading="loadingMap.subnets" border stripe>
          <el-table-column prop="name" label="子网名称" min-width="160" />
          <el-table-column prop="cidr" label="CIDR" width="160" />
          <el-table-column prop="ipVersion" label="IP 版本" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small">IPv{{ row.ipVersion }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enableDhcp" label="DHCP" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enableDhcp ? 'success' : 'info'" size="small">{{ row.enableDhcp ? '开启' : '关闭' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="networkId" label="所属网络" min-width="280" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <!-- 安全组 -->
      <el-tab-pane label="安全组" name="security-groups">
        <el-table :data="securityGroups" v-loading="loadingMap.sgs" border stripe>
          <el-table-column prop="name" label="安全组名称" min-width="180" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="id" label="安全组 ID" min-width="280" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <!-- 浮动 IP -->
      <el-tab-pane label="浮动 IP" name="floatingips">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="handleAllocateIp">申请浮动 IP</el-button>
        </div>
        <el-table :data="floatingIps" v-loading="loadingMap.fips" border stripe>
          <el-table-column prop="floatingIpAddress" label="浮动 IP" min-width="160" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <StatusBadge :status="row.status.toLowerCase()" :label="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="fixedIpAddress" label="绑定内网 IP" min-width="160" />
          <el-table-column prop="portId" label="绑定端口" min-width="280" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getClusterList, getNetworks, getSubnets, getSecurityGroups, getFloatingIps, allocateFloatingIp } from '@/api/cloud'
import type { ClusterVO, NetworkVO, SubnetVO, SecurityGroupVO, FloatingIpVO } from '@/api/cloud'
import StatusBadge from '@/components/StatusBadge.vue'

const clusterList        = ref<ClusterVO[]>([])
const selectedClusterId  = ref<number>()
const activeTab          = ref('networks')

const networks       = ref<NetworkVO[]>([])
const subnets        = ref<SubnetVO[]>([])
const securityGroups = ref<SecurityGroupVO[]>([])
const floatingIps    = ref<FloatingIpVO[]>([])

const loadingMap = reactive({ networks: false, subnets: false, sgs: false, fips: false })

onMounted(async () => {
  clusterList.value = await getClusterList()
  if (clusterList.value.length) {
    selectedClusterId.value = clusterList.value[0].clusterId
    fetchAll()
  }
})

async function fetchAll() {
  if (!selectedClusterId.value) return
  const cid = selectedClusterId.value

  loadingMap.networks = true
  getNetworks(cid).then(res => { networks.value = res }).finally(() => { loadingMap.networks = false })

  loadingMap.subnets = true
  getSubnets(cid).then(res => { subnets.value = res }).finally(() => { loadingMap.subnets = false })

  loadingMap.sgs = true
  getSecurityGroups(cid).then(res => { securityGroups.value = res }).finally(() => { loadingMap.sgs = false })

  loadingMap.fips = true
  getFloatingIps(cid).then(res => { floatingIps.value = res }).finally(() => { loadingMap.fips = false })
}

async function handleAllocateIp() {
  const extNetworkId = networks.value.find(n => n.shared)?.id
  if (!extNetworkId) { ElMessage.warning('未找到共享外部网络'); return }
  await ElMessageBox.confirm('确定从外部网络申请一个浮动 IP？', '确认')
  await allocateFloatingIp(selectedClusterId.value!, extNetworkId)
  ElMessage.success('申请成功')
  loadingMap.fips = true
  getFloatingIps(selectedClusterId.value!).then(res => { floatingIps.value = res }).finally(() => { loadingMap.fips = false })
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.toolbar-card :deep(.el-card__body) { padding: 12px 16px; }
.tab-toolbar { margin-bottom: 12px; }
</style>
