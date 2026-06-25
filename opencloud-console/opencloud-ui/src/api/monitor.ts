import request from '@/utils/request'

// ──────────────────────────────────────────────
// 指标查询
// ──────────────────────────────────────────────

export function queryMetric(params: MetricQueryReq) {
  return request.get<any, MetricDataVO>('/monitor/metrics/query', { params })
}

export function queryMetricRange(params: MetricRangeQueryReq) {
  return request.get<any, MetricRangeDataVO>('/monitor/metrics/query_range', { params })
}

// ──────────────────────────────────────────────
// Grafana
// ──────────────────────────────────────────────

export function getGrafanaDashboards() {
  return request.get<any, GrafanaDashboardVO[]>('/monitor/grafana/dashboards')
}

export function getGrafanaPanelUrl(dashboardUid: string, panelId: number, params?: Record<string, string>) {
  return request.get<any, string>(`/monitor/grafana/embed/${dashboardUid}/${panelId}`, { params })
}

// ──────────────────────────────────────────────
// 告警记录
// ──────────────────────────────────────────────

export function getAlertRecordPage(params: AlertRecordPageReq) {
  return request.get<any, PageResult<AlertRecordVO>>('/monitor/alerts', { params })
}

export function getAlertRecordDetail(recordId: number) {
  return request.get<any, AlertRecordVO>(`/monitor/alerts/${recordId}`)
}

export function ackAlertRecord(recordId: number, comment?: string) {
  return request.post<any, void>(`/monitor/alerts/${recordId}/ack`, { comment })
}

export function getAlertStats() {
  return request.get<any, AlertStatsVO>('/monitor/alerts/stats')
}

// ──────────────────────────────────────────────
// 告警规则
// ──────────────────────────────────────────────

export function getAlertRulePage(params: AlertRulePageReq) {
  return request.get<any, PageResult<AlertRuleVO>>('/monitor/rules', { params })
}

export function getAlertRuleDetail(ruleId: number) {
  return request.get<any, AlertRuleVO>(`/monitor/rules/${ruleId}`)
}

export function createAlertRule(data: AlertRuleFormReq) {
  return request.post<any, void>('/monitor/rules', data)
}

export function updateAlertRule(ruleId: number, data: AlertRuleFormReq) {
  return request.put<any, void>(`/monitor/rules/${ruleId}`, data)
}

export function deleteAlertRule(ruleId: number) {
  return request.delete<any, void>(`/monitor/rules/${ruleId}`)
}

export function updateAlertRuleStatus(ruleId: number, enabled: boolean) {
  return request.put<any, void>(`/monitor/rules/${ruleId}/status`, { enabled })
}

// ──────────────────────────────────────────────
// 通知渠道
// ──────────────────────────────────────────────

export function getNotifyChannelList() {
  return request.get<any, NotifyChannelVO[]>('/monitor/notify-channels')
}

export function getNotifyChannelDetail(channelId: number) {
  return request.get<any, NotifyChannelVO>(`/monitor/notify-channels/${channelId}`)
}

export function createNotifyChannel(data: NotifyChannelFormReq) {
  return request.post<any, void>('/monitor/notify-channels', data)
}

export function updateNotifyChannel(channelId: number, data: NotifyChannelFormReq) {
  return request.put<any, void>(`/monitor/notify-channels/${channelId}`, data)
}

export function deleteNotifyChannel(channelId: number) {
  return request.delete<any, void>(`/monitor/notify-channels/${channelId}`)
}

export function testNotifyChannel(channelId: number) {
  return request.post<any, void>(`/monitor/notify-channels/${channelId}/test`)
}

// ──────────────────────────────────────────────
// 告警静默规则
// ──────────────────────────────────────────────

export function getAlertSilencePage(params: AlertSilencePageReq) {
  return request.get<any, PageResult<AlertSilenceVO>>('/monitor/silences', { params })
}

export function getAlertSilenceDetail(silenceId: number) {
  return request.get<any, AlertSilenceVO>(`/monitor/silences/${silenceId}`)
}

export function createAlertSilence(data: AlertSilenceFormReq) {
  return request.post<any, void>('/monitor/silences', data)
}

export function updateAlertSilence(silenceId: number, data: AlertSilenceFormReq) {
  return request.put<any, void>(`/monitor/silences/${silenceId}`, data)
}

export function deleteAlertSilence(silenceId: number) {
  return request.delete<any, void>(`/monitor/silences/${silenceId}`)
}

// ──────────────────────────────────────────────
// 通知日志
// ──────────────────────────────────────────────

export function getNotifyLogPage(params: NotifyLogPageReq) {
  return request.get<any, PageResult<NotifyLogVO>>('/monitor/notify-logs', { params })
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

export interface MetricQueryReq {
  query: string
  time?: number
}

export interface MetricDataVO {
  resultType: string
  result: { metric: Record<string, string>; value: [number, string] }[]
}

export interface MetricRangeQueryReq {
  query: string
  start: number
  end: number
  step: number
}

export interface MetricRangeDataVO {
  resultType: string
  result: { metric: Record<string, string>; values: [number, string][] }[]
}

export interface GrafanaDashboardVO {
  uid: string
  title: string
  url: string
  tags: string[]
}

export interface AlertRecordVO {
  recordId: number
  alertName: string
  severity: string   // CRITICAL | WARNING | INFO
  status: string     // FIRING | RESOLVED | ACKNOWLEDGED
  labels: Record<string, string>
  annotations: Record<string, string>
  summary: string
  description: string
  fingerprint: string
  startsAt: string
  endsAt?: string
  acknowledgedBy?: string
  acknowledgedAt?: string
  acknowledgeComment?: string
  createTime: string
}

export interface AlertRecordPageReq {
  alertName?: string
  severity?: string
  status?: string
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export interface AlertStatsVO {
  total: number
  critical: number
  warning: number
  info: number
  firing: number
  resolved: number
  acknowledged: number
}

export interface AlertRuleVO {
  ruleId: number
  ruleName: string
  expr: string
  duration: string
  severity: string
  summary: string
  description: string
  channelIds: number[]
  enabled: boolean
  createTime: string
}

export interface AlertRuleFormReq {
  ruleName: string
  expr: string
  duration: string
  severity: string
  summary?: string
  description?: string
  channelIds?: number[]
  enabled?: boolean
}

export interface AlertRulePageReq {
  ruleName?: string
  severity?: string
  enabled?: boolean
  pageNum: number
  pageSize: number
}

export interface NotifyChannelVO {
  channelId: number
  channelName: string
  channelType: string  // EMAIL | DINGTALK | WECHAT_WORK | WEBHOOK
  config: Record<string, string>
  enabled: boolean
  createTime: string
}

export interface NotifyChannelFormReq {
  channelName: string
  channelType: string
  config: Record<string, string>
  enabled?: boolean
}

export interface NotifyLogVO {
  logId: number
  recordId: number
  channelId: number
  channelName: string
  channelType: string
  status: string
  errorMsg?: string
  createTime: string
}

export interface NotifyLogPageReq {
  recordId?: number
  channelId?: number
  status?: string
  pageNum: number
  pageSize: number
}

// ──────────────────────────────────────────────
// Alert Silence Types
// ──────────────────────────────────────────────

export interface AlertSilenceVO {
  silenceId: number
  alertName?: string
  matchLabels: Record<string, string>
  comment: string
  createdBy: string
  startsAt: string
  endsAt: string
  expired: boolean
  createTime: string
}

export interface AlertSilenceFormReq {
  alertName?: string
  matchLabels?: Record<string, string>
  comment: string
  startsAt: string
  endsAt: string
}

export interface AlertSilencePageReq {
  alertName?: string
  expired?: boolean
  pageNum: number
  pageSize: number
}
