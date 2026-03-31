-- ============================================================
-- opencloud-cloud 数据库初始化脚本
-- 数据库: PostgreSQL 15+
-- Schema: cloud
-- 字符集: UTF-8
-- ============================================================

-- 创建 Schema
CREATE SCHEMA IF NOT EXISTS cloud;

-- ============================================================
-- 1. 集群配置表 cloud_cluster_config
-- 用途: 存储 OpenStack、Ceph、Kubernetes 的连接配置信息
-- ============================================================
CREATE TABLE cloud.cloud_cluster_config (
    id                  bigserial       PRIMARY KEY,
    tenant_id           varchar(36)     NOT NULL,
    name                varchar(64)     NOT NULL,
    type                varchar(16)     NOT NULL,  -- OPENSTACK / CEPH / KUBERNETES
    endpoint            varchar(256)    NOT NULL,
    config_json         text            NOT NULL,  -- AES 加密的 JSON
    status              varchar(16)     NOT NULL DEFAULT 'PENDING',  -- ACTIVE / ERROR / PENDING
    last_check_time     timestamptz,
    error_msg           varchar(512),
    created_at          timestamptz     NOT NULL DEFAULT now(),
    updated_at          timestamptz     NOT NULL DEFAULT now(),
    created_by          varchar(36)     NOT NULL,
    updated_by          varchar(36)     NOT NULL,
    deleted             smallint        NOT NULL DEFAULT 0
);

-- 索引
CREATE UNIQUE INDEX idx_ccc_name    ON cloud.cloud_cluster_config(tenant_id, name) WHERE deleted = 0;
CREATE INDEX        idx_ccc_tenant  ON cloud.cloud_cluster_config(tenant_id);
CREATE INDEX        idx_ccc_type    ON cloud.cloud_cluster_config(type);
CREATE INDEX        idx_ccc_status  ON cloud.cloud_cluster_config(status);

-- 注释
COMMENT ON TABLE  cloud.cloud_cluster_config           IS '云平台集群配置表';
COMMENT ON COLUMN cloud.cloud_cluster_config.tenant_id IS '租户 ID';
COMMENT ON COLUMN cloud.cloud_cluster_config.name      IS '集群名称（租户内唯一）';
COMMENT ON COLUMN cloud.cloud_cluster_config.type      IS '集群类型: OPENSTACK / CEPH / KUBERNETES';
COMMENT ON COLUMN cloud.cloud_cluster_config.endpoint  IS '服务端点 URL';
COMMENT ON COLUMN cloud.cloud_cluster_config.config_json IS '连接配置 JSON（AES-256-GCM 加密）';
COMMENT ON COLUMN cloud.cloud_cluster_config.status    IS '状态: ACTIVE=正常 ERROR=连接失败 PENDING=待测试';
COMMENT ON COLUMN cloud.cloud_cluster_config.last_check_time IS '上次连接测试时间';
COMMENT ON COLUMN cloud.cloud_cluster_config.error_msg IS '错误信息（连接失败时记录）';
COMMENT ON COLUMN cloud.cloud_cluster_config.deleted   IS '逻辑删除标记: 0=未删除 1=已删除';

-- ============================================================
-- 2. 异步任务表 cloud_task
-- 用途: 记录云资源异步操作的任务状态
-- ============================================================
CREATE TABLE cloud.cloud_task (
    id              bigserial       PRIMARY KEY,
    task_id         varchar(36)     NOT NULL UNIQUE,
    tenant_id       varchar(36)     NOT NULL,
    type            varchar(32)     NOT NULL,  -- INSTANCE_CREATE / VOLUME_CREATE / ...
    status          varchar(16)     NOT NULL DEFAULT 'PENDING',  -- PENDING / RUNNING / SUCCESS / FAILED / CANCELLED
    resource_type   varchar(32)     NOT NULL,
    resource_id     varchar(64),
    cluster_id      varchar(36)     NOT NULL,
    progress        smallint        NOT NULL DEFAULT 0,
    result_json     text,
    error_msg       varchar(512),
    created_at      timestamptz     NOT NULL DEFAULT now(),
    started_at      timestamptz,
    completed_at    timestamptz,
    created_by      varchar(36)     NOT NULL
);

-- 索引
CREATE UNIQUE INDEX idx_ct_task_id   ON cloud.cloud_task(task_id);
CREATE INDEX        idx_ct_tenant    ON cloud.cloud_task(tenant_id);
CREATE INDEX        idx_ct_status    ON cloud.cloud_task(status);
CREATE INDEX        idx_ct_type      ON cloud.cloud_task(type);
CREATE INDEX        idx_ct_cluster   ON cloud.cloud_task(cluster_id);
CREATE INDEX        idx_ct_resource  ON cloud.cloud_task(resource_type, resource_id);
CREATE INDEX        idx_ct_created   ON cloud.cloud_task(created_at DESC);

-- 注释
COMMENT ON TABLE  cloud.cloud_task              IS '异步任务表，跟踪云资源操作';
COMMENT ON COLUMN cloud.cloud_task.task_id      IS '任务唯一标识（UUID）';
COMMENT ON COLUMN cloud.cloud_task.type         IS '任务类型: INSTANCE_CREATE / INSTANCE_DELETE / VOLUME_CREATE / VOLUME_DELETE / IMAGE_UPLOAD / CLUSTER_SYNC / ...';
COMMENT ON COLUMN cloud.cloud_task.status       IS '状态: PENDING / RUNNING / SUCCESS / FAILED / CANCELLED';
COMMENT ON COLUMN cloud.cloud_task.resource_type IS '资源类型: INSTANCE / VOLUME / IMAGE / ...';
COMMENT ON COLUMN cloud.cloud_task.resource_id  IS '资源 ID（云平台返回的 ID）';
COMMENT ON COLUMN cloud.cloud_task.cluster_id   IS '关联集群 ID';
COMMENT ON COLUMN cloud.cloud_task.progress     IS '进度百分比（0-100）';
COMMENT ON COLUMN cloud.cloud_task.result_json  IS '执行结果 JSON（成功时）';
COMMENT ON COLUMN cloud.cloud_task.error_msg    IS '错误信息（失败时）';

-- ============================================================
-- 3. 配额配置表 cloud_quota
-- 用途: 存储租户在各平台的资源配额限制
-- ============================================================
CREATE TABLE cloud.cloud_quota (
    id                              bigserial       PRIMARY KEY,
    tenant_id                       varchar(36)     NOT NULL UNIQUE,
    openstack_vcpu_limit            integer         NOT NULL DEFAULT -1,
    openstack_memory_limit          bigint          NOT NULL DEFAULT -1,
    openstack_instance_limit        integer         NOT NULL DEFAULT -1,
    openstack_volume_count_limit    integer         NOT NULL DEFAULT -1,
    openstack_volume_storage_limit  bigint          NOT NULL DEFAULT -1,
    ceph_storage_limit              bigint          NOT NULL DEFAULT -1,
    k8s_cpu_limit                   integer         NOT NULL DEFAULT -1,
    k8s_memory_limit                bigint          NOT NULL DEFAULT -1,
    created_at                      timestamptz     NOT NULL DEFAULT now(),
    updated_at                      timestamptz     NOT NULL DEFAULT now()
);

-- 索引
CREATE UNIQUE INDEX idx_cq_tenant ON cloud.cloud_quota(tenant_id);

-- 注释
COMMENT ON TABLE  cloud.cloud_quota                           IS '租户资源配额表';
COMMENT ON COLUMN cloud.cloud_quota.tenant_id                 IS '租户 ID';
COMMENT ON COLUMN cloud.cloud_quota.openstack_vcpu_limit      IS 'OpenStack vCPU 限制（-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_memory_limit    IS 'OpenStack 内存限制（MB，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_instance_limit  IS 'OpenStack 实例数限制（-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_volume_count_limit IS 'OpenStack 卷数量限制（-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_volume_storage_limit IS 'OpenStack 卷存储限制（GB，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.ceph_storage_limit        IS 'Ceph 存储限制（GB，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.k8s_cpu_limit             IS 'K8s CPU 限制（核，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.k8s_memory_limit          IS 'K8s 内存限制（MB，-1=无限制）';

-- ============================================================
-- 4. 资源同步日志表 cloud_sync_log
-- 用途: 记录集群资源同步的历史记录
-- ============================================================
CREATE TABLE cloud.cloud_sync_log (
    id                bigserial       PRIMARY KEY,
    cluster_id        varchar(36)     NOT NULL,
    tenant_id         varchar(36)     NOT NULL,
    sync_type         varchar(16)     NOT NULL,  -- AUTO / MANUAL
    status            varchar(16)     NOT NULL,  -- SUCCESS / FAILED
    resource_counts   jsonb,
    error_msg         varchar(512),
    started_at        timestamptz     NOT NULL,
    completed_at      timestamptz,
    created_at        timestamptz     NOT NULL DEFAULT now()
);

-- 索引
CREATE INDEX idx_csl_cluster  ON cloud.cloud_sync_log(cluster_id);
CREATE INDEX idx_csl_tenant   ON cloud.cloud_sync_log(tenant_id);
CREATE INDEX idx_csl_status   ON cloud.cloud_sync_log(status);
CREATE INDEX idx_csl_created  ON cloud.cloud_sync_log(created_at DESC);

-- 注释
COMMENT ON TABLE  cloud.cloud_sync_log            IS '资源同步日志表';
COMMENT ON COLUMN cloud.cloud_sync_log.cluster_id IS '集群 ID';
COMMENT ON COLUMN cloud.cloud_sync_log.sync_type  IS '同步类型: AUTO=自动定时同步 MANUAL=手动触发';
COMMENT ON COLUMN cloud.cloud_sync_log.status     IS '状态: SUCCESS / FAILED';
COMMENT ON COLUMN cloud.cloud_sync_log.resource_counts IS '各资源类型同步数量统计 JSON';
COMMENT ON COLUMN cloud.cloud_sync_log.error_msg  IS '错误信息（失败时）';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 为默认租户插入默认配额（0 表示拒绝创建，需管理员手动分配）
-- 注意: 请根据实际 tenant_id 修改
-- INSERT INTO cloud.cloud_quota (tenant_id, openstack_vcpu_limit, openstack_memory_limit, openstack_instance_limit)
-- VALUES ('default-tenant-id', 0, 0, 0);

-- ============================================================
-- 数据清理任务（可选，通过应用定时任务实现）
-- ============================================================

-- 清理 7 天前的已完成任务
-- DELETE FROM cloud.cloud_task 
-- WHERE status IN ('SUCCESS', 'FAILED', 'CANCELLED') 
-- AND completed_at < now() - interval '7 days';

-- 清理 30 天前的同步日志
-- DELETE FROM cloud.cloud_sync_log 
-- WHERE created_at < now() - interval '30 days';

-- ============================================================
-- 脚本信息
-- ============================================================
-- 版本: 1.0
-- 创建日期: 2026-03-31
-- 维护人: 开发团队
-- ============================================================
