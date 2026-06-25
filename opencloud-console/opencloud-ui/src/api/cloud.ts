import request from '@/utils/request'

// ──────────────────────────────────────────────
// 集群管理
// ──────────────────────────────────────────────

export function getClusterList() {
  return request.get<any, ClusterVO[]>('/cloud/clusters')
}

export function getClusterDetail(clusterId: number) {
  return request.get<any, ClusterVO>(`/cloud/clusters/${clusterId}`)
}

export function createCluster(data: ClusterFormReq) {
  return request.post<any, void>('/cloud/clusters', data)
}

export function updateCluster(clusterId: number, data: ClusterFormReq) {
  return request.put<any, void>(`/cloud/clusters/${clusterId}`, data)
}

export function deleteCluster(clusterId: number) {
  return request.delete<any, void>(`/cloud/clusters/${clusterId}`)
}

export function testClusterConnection(clusterId: number) {
  return request.post<any, { success: boolean; message: string }>(`/cloud/clusters/${clusterId}/test`)
}

// ──────────────────────────────────────────────
// OpenStack - 云主机
// ──────────────────────────────────────────────

export function getInstances(clusterId: number, params?: InstancePageReq) {
  return request.get<any, PageResult<InstanceVO>>(`/cloud/openstack/${clusterId}/instances`, { params })
}

export function getInstance(clusterId: number, instanceId: string) {
  return request.get<any, InstanceVO>(`/cloud/openstack/${clusterId}/instances/${instanceId}`)
}

export function createInstance(clusterId: number, data: CreateInstanceReq) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/instances`, data)
}

export function deleteInstance(clusterId: number, instanceId: string) {
  return request.delete<any, void>(`/cloud/openstack/${clusterId}/instances/${instanceId}`)
}

export function startInstance(clusterId: number, instanceId: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/instances/${instanceId}/start`)
}

export function stopInstance(clusterId: number, instanceId: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/instances/${instanceId}/stop`)
}

export function rebootInstance(clusterId: number, instanceId: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/instances/${instanceId}/reboot`)
}

// ──────────────────────────────────────────────
// OpenStack - 规格 / 镜像 / 网络 / 安全组
// ──────────────────────────────────────────────

export function getFlavors(clusterId: number) {
  return request.get<any, FlavorVO[]>(`/cloud/openstack/${clusterId}/flavors`)
}

export function getImages(clusterId: number) {
  return request.get<any, ImageVO[]>(`/cloud/openstack/${clusterId}/images`)
}

export function uploadImage(clusterId: number, data: FormData) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/images/upload`, data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteImage(clusterId: number, imageId: string) {
  return request.delete<any, void>(`/cloud/openstack/${clusterId}/images/${imageId}`)
}

export function getNetworks(clusterId: number) {
  return request.get<any, NetworkVO[]>(`/cloud/openstack/${clusterId}/networks`)
}

export function getSubnets(clusterId: number, networkId?: string) {
  return request.get<any, SubnetVO[]>(`/cloud/openstack/${clusterId}/subnets`, { params: { networkId } })
}

export function getSecurityGroups(clusterId: number) {
  return request.get<any, SecurityGroupVO[]>(`/cloud/openstack/${clusterId}/security-groups`)
}

export function getFloatingIps(clusterId: number) {
  return request.get<any, FloatingIpVO[]>(`/cloud/openstack/${clusterId}/floating-ips`)
}

export function allocateFloatingIp(clusterId: number, networkId: string) {
  return request.post<any, FloatingIpVO>(`/cloud/openstack/${clusterId}/floating-ips`, { networkId })
}

// ──────────────────────────────────────────────
// OpenStack - 云硬盘
// ──────────────────────────────────────────────

export function getVolumes(clusterId: number, params?: PageReq) {
  return request.get<any, PageResult<VolumeVO>>(`/cloud/openstack/${clusterId}/volumes`, { params })
}

export function createVolume(clusterId: number, data: CreateVolumeReq) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/volumes`, data)
}

export function deleteVolume(clusterId: number, volumeId: string) {
  return request.delete<any, void>(`/cloud/openstack/${clusterId}/volumes/${volumeId}`)
}

export function attachVolume(clusterId: number, volumeId: string, instanceId: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/volumes/${volumeId}/attach`, { instanceId })
}

export function detachVolume(clusterId: number, volumeId: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/volumes/${volumeId}/detach`)
}

export function getVolumeSnapshots(clusterId: number) {
  return request.get<any, VolumeSnapshotVO[]>(`/cloud/openstack/${clusterId}/volume-snapshots`)
}

export function createVolumeSnapshot(clusterId: number, volumeId: string, name: string) {
  return request.post<any, void>(`/cloud/openstack/${clusterId}/volume-snapshots`, { volumeId, name })
}

// ──────────────────────────────────────────────
// Ceph
// ──────────────────────────────────────────────

export function getCephOverview(clusterId: number) {
  return request.get<any, CephOverviewVO>(`/cloud/ceph/${clusterId}/overview`)
}

export function getCephPools(clusterId: number) {
  return request.get<any, CephPoolVO[]>(`/cloud/ceph/${clusterId}/pools`)
}

export function getCephOsdStatus(clusterId: number) {
  return request.get<any, CephOsdVO[]>(`/cloud/ceph/${clusterId}/osds`)
}

export function getCephBuckets(clusterId: number) {
  return request.get<any, BucketVO[]>(`/cloud/ceph/${clusterId}/buckets`)
}

export function createBucket(clusterId: number, name: string) {
  return request.post<any, void>(`/cloud/ceph/${clusterId}/buckets`, { name })
}

export function deleteBucket(clusterId: number, name: string) {
  return request.delete<any, void>(`/cloud/ceph/${clusterId}/buckets/${name}`)
}

export function getBucketObjects(clusterId: number, bucket: string, prefix?: string) {
  return request.get<any, ObjectVO[]>(`/cloud/ceph/${clusterId}/buckets/${bucket}/objects`, { params: { prefix } })
}

// ──────────────────────────────────────────────
// Kubernetes
// ──────────────────────────────────────────────

export function getDeployments(clusterId: number, namespace?: string) {
  return request.get<any, DeploymentVO[]>(`/cloud/k8s/${clusterId}/deployments`, { params: { namespace } })
}

export function getPods(clusterId: number, namespace?: string) {
  return request.get<any, PodVO[]>(`/cloud/k8s/${clusterId}/pods`, { params: { namespace } })
}

export function getPodLogs(clusterId: number, namespace: string, podName: string, container?: string) {
  return request.get<any, string>(`/cloud/k8s/${clusterId}/pods/${namespace}/${podName}/logs`, { params: { container } })
}

export function getServices(clusterId: number, namespace?: string) {
  return request.get<any, K8sServiceVO[]>(`/cloud/k8s/${clusterId}/services`, { params: { namespace } })
}

export function getConfigMaps(clusterId: number, namespace?: string) {
  return request.get<any, ConfigMapVO[]>(`/cloud/k8s/${clusterId}/configmaps`, { params: { namespace } })
}

export function getSecrets(clusterId: number, namespace?: string) {
  return request.get<any, SecretVO[]>(`/cloud/k8s/${clusterId}/secrets`, { params: { namespace } })
}

export function getNodes(clusterId: number) {
  return request.get<any, NodeVO[]>(`/cloud/k8s/${clusterId}/nodes`)
}

// ──────────────────────────────────────────────
// 配额
// ──────────────────────────────────────────────

export function getQuota(clusterId: number) {
  return request.get<any, QuotaVO>(`/cloud/openstack/${clusterId}/quota`)
}

// ──────────────────────────────────────────────
// Types
// ──────────────────────────────────────────────

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface PageReq {
  pageNum?: number
  pageSize?: number
}

export interface ClusterVO {
  clusterId: number
  clusterName: string
  clusterType: string  // OPENSTACK | CEPH | KUBERNETES
  endpoint: string
  status: string       // CONNECTED | DISCONNECTED | UNKNOWN
  description: string
  createTime: string
}

export interface ClusterFormReq {
  clusterName: string
  clusterType: string
  endpoint: string
  authConfig: Record<string, string>
  description?: string
}

export interface InstanceVO {
  id: string
  name: string
  status: string
  flavorId: string
  flavorName: string
  imageId: string
  imageName: string
  addresses: Record<string, { addr: string; type: string }[]>
  keyName: string
  tenantId: string
  created: string
  updated: string
}

export interface InstancePageReq extends PageReq {
  status?: string
  name?: string
}

export interface CreateInstanceReq {
  name: string
  flavorId: string
  imageId: string
  networkIds: string[]
  securityGroupIds?: string[]
  keyName?: string
  userData?: string
  count?: number
}

export interface FlavorVO {
  id: string
  name: string
  vcpus: number
  ram: number
  disk: number
}

export interface ImageVO {
  id: string
  name: string
  status: string
  size: number
  diskFormat: string
  containerFormat: string
  visibility: string
  createdAt: string
}

export interface NetworkVO {
  id: string
  name: string
  status: string
  adminStateUp: boolean
  shared: boolean
  tenantId: string
}

export interface SubnetVO {
  id: string
  name: string
  networkId: string
  cidr: string
  ipVersion: number
  enableDhcp: boolean
}

export interface SecurityGroupVO {
  id: string
  name: string
  description: string
  tenantId: string
}

export interface FloatingIpVO {
  id: string
  floatingIpAddress: string
  status: string
  fixedIpAddress?: string
  portId?: string
}

export interface VolumeVO {
  id: string
  name: string
  status: string
  size: number
  volumeType: string
  attachments: { serverId: string; device: string }[]
  createdAt: string
}

export interface CreateVolumeReq {
  name: string
  size: number
  volumeType?: string
  snapshotId?: string
}

export interface VolumeSnapshotVO {
  id: string
  name: string
  status: string
  size: number
  volumeId: string
  createdAt: string
}

export interface CephOverviewVO {
  status: string
  totalBytes: number
  usedBytes: number
  availBytes: number
  osdTotal: number
  osdUp: number
  osdIn: number
  pgTotal: number
  pgActive: number
}

export interface CephPoolVO {
  id: number
  name: string
  size: number
  pgNum: number
  usedBytes: number
  percentUsed: number
  compressionMode: string
}

export interface CephOsdVO {
  osdId: number
  name: string
  status: string
  weight: number
  reweight: number
  utilization: number
  host: string
  deviceClass: string
}

export interface BucketVO {
  name: string
  creationDate: string
  objectCount?: number
  totalSize?: number
}

export interface ObjectVO {
  key: string
  size: number
  lastModified: string
  etag: string
  storageClass: string
}

export interface DeploymentVO {
  name: string
  namespace: string
  replicas: number
  readyReplicas: number
  availableReplicas: number
  image: string
  creationTimestamp: string
}

export interface PodVO {
  name: string
  namespace: string
  status: string
  ip: string
  nodeName: string
  containers: { name: string; image: string; ready: boolean }[]
  creationTimestamp: string
}

export interface K8sServiceVO {
  name: string
  namespace: string
  type: string
  clusterIp: string
  externalIp: string
  ports: { port: number; targetPort: number; protocol: string }[]
  selector: Record<string, string>
  creationTimestamp: string
}

export interface ConfigMapVO {
  name: string
  namespace: string
  dataKeys: string[]
  creationTimestamp: string
}

export interface SecretVO {
  name: string
  namespace: string
  type: string
  dataKeys: string[]
  creationTimestamp: string
}

export interface NodeVO {
  name: string
  status: string
  roles: string[]
  version: string
  osImage: string
  cpuCapacity: string
  memoryCapacity: string
  cpuAllocatable: string
  memoryAllocatable: string
  creationTimestamp: string
}

export interface QuotaVO {
  instances: { limit: number; inUse: number; reserved: number }
  cores: { limit: number; inUse: number; reserved: number }
  ram: { limit: number; inUse: number; reserved: number }
  floatingIps: { limit: number; inUse: number; reserved: number }
  volumes: { limit: number; inUse: number; reserved: number }
  gigabytes: { limit: number; inUse: number; reserved: number }
}
