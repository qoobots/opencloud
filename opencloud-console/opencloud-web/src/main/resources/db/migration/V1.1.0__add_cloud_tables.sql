-- ============================================================
-- OpenCloud 控制台 — Flyway 迁移脚本
-- 版本：V1.1.0
-- 内容：云平台对接模块数据表
--   cloud_cluster_config / cloud_task / cloud_quota / cloud_sync_log
-- ============================================================

-- 集群配置表
CREATE TABLE IF NOT EXISTS cloud_cluster_config (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    cluster_name        VARCHAR(64)     NOT NULL,
    cluster_type        VARCHAR(32)     NOT NULL,
    config_json         TEXT            NOT NULL,
    description         VARCHAR(256),
    status              SMALLINT        NOT NULL DEFAULT 0,
    last_check_time     TIMESTAMP,
    last_check_result   VARCHAR(16),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE cloud_cluster_config IS '云平台集群配置表';
COMMENT ON COLUMN cloud_cluster_config.cluster_type    IS 'OPENSTACK | CEPH | KUBERNETES';
COMMENT ON COLUMN cloud_cluster_config.config_json     IS '连接配置 JSON，AES-256-GCM 加密存储';
COMMENT ON COLUMN cloud_cluster_config.status          IS '0=启用 | 1=禁用 | 2=离线';

CREATE UNIQUE INDEX IF NOT EXISTS idx_cloud_cluster_name ON cloud_cluster_config(cluster_name, tenant_id) WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_cloud_cluster_type ON cloud_cluster_config(cluster_type, tenant_id) WHERE deleted = 0;

-- 云平台异步任务表
CREATE TABLE IF NOT EXISTS cloud_task (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    cluster_id      BIGINT          NOT NULL,
    task_type       VARCHAR(64)     NOT NULL,
    task_status     VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    resource_id     VARCHAR(128),
    resource_type   VARCHAR(32),
    request_params  TEXT,
    result_message  TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE cloud_task IS '云平台异步任务记录表';
COMMENT ON COLUMN cloud_task.task_status IS 'PENDING | RUNNING | SUCCESS | FAILED';

CREATE INDEX IF NOT EXISTS idx_cloud_task_cluster_id  ON cloud_task(cluster_id);
CREATE INDEX IF NOT EXISTS idx_cloud_task_status      ON cloud_task(task_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cloud_task_created_at  ON cloud_task(created_at DESC);

-- 资源配额表
CREATE TABLE IF NOT EXISTS cloud_quota (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    cluster_id      BIGINT          NOT NULL,
    resource_type   VARCHAR(32)     NOT NULL,
    quota_limit     BIGINT          NOT NULL,
    quota_used      BIGINT          NOT NULL DEFAULT 0,
    unit            VARCHAR(16),
    synced_at       TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE cloud_quota IS '云平台资源配额表';
COMMENT ON COLUMN cloud_quota.resource_type IS 'VCPU | MEMORY_MB | DISK_GB | INSTANCE | FLOATING_IP';

CREATE UNIQUE INDEX IF NOT EXISTS idx_cloud_quota_unique ON cloud_quota(tenant_id, cluster_id, resource_type) WHERE deleted = 0;

-- 资源同步日志表
CREATE TABLE IF NOT EXISTS cloud_sync_log (
    id              BIGINT          PRIMARY KEY,
    cluster_id      BIGINT          NOT NULL,
    sync_type       VARCHAR(32)     NOT NULL,
    sync_status     VARCHAR(16)     NOT NULL,
    synced_count    INT,
    error_message   TEXT,
    started_at      TIMESTAMP       NOT NULL,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cloud_sync_log IS '云平台资源同步日志';

CREATE INDEX IF NOT EXISTS idx_cloud_sync_log_cluster_id  ON cloud_sync_log(cluster_id);
CREATE INDEX IF NOT EXISTS idx_cloud_sync_log_created_at  ON cloud_sync_log(created_at DESC);
