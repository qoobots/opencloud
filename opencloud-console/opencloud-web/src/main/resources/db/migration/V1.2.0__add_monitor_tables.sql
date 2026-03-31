-- ============================================================
-- OpenCloud 控制台 — Flyway 迁移脚本
-- 版本：V1.2.0
-- 内容：监控告警模块数据表
--   monitor_alert_rule / monitor_alert_record / monitor_alert_silence
--   monitor_notify_channel / monitor_notify_log
-- ============================================================

-- 告警规则表
CREATE TABLE IF NOT EXISTS monitor_alert_rule (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    rule_name       VARCHAR(128)    NOT NULL,
    expr            TEXT            NOT NULL,
    duration        VARCHAR(32),
    severity        VARCHAR(16)     NOT NULL DEFAULT 'warning',
    summary         VARCHAR(256),
    description     TEXT,
    labels          JSONB,
    notify_channels TEXT,
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE monitor_alert_rule IS '告警规则表';
COMMENT ON COLUMN monitor_alert_rule.severity IS 'critical=严重 | warning=警告 | info=信息';

CREATE INDEX IF NOT EXISTS idx_alert_rule_tenant_id ON monitor_alert_rule(tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_rule_severity  ON monitor_alert_rule(severity)  WHERE deleted = 0;

-- 告警记录表
CREATE TABLE IF NOT EXISTS monitor_alert_record (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    rule_id         BIGINT,
    alert_name      VARCHAR(128)    NOT NULL,
    severity        VARCHAR(16)     NOT NULL,
    summary         VARCHAR(512),
    description     TEXT,
    labels          JSONB           NOT NULL DEFAULT '{}',
    status          VARCHAR(16)     NOT NULL DEFAULT 'firing',
    fired_at        TIMESTAMP       NOT NULL,
    resolved_at     TIMESTAMP,
    acked_at        TIMESTAMP,
    acked_by        BIGINT,
    notify_count    INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE monitor_alert_record IS '告警事件记录表';
COMMENT ON COLUMN monitor_alert_record.status IS 'firing=告警中 | resolved=已恢复 | acked=已确认';
COMMENT ON COLUMN monitor_alert_record.labels IS '告警标签 JSONB，支持 GIN 索引多维查询';

CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_record_dedup    ON monitor_alert_record(alert_name, (labels->>'instance'), fired_at);
CREATE        INDEX IF NOT EXISTS idx_alert_record_status   ON monitor_alert_record(status);
CREATE        INDEX IF NOT EXISTS idx_alert_record_fired_at ON monitor_alert_record(fired_at DESC);
CREATE        INDEX IF NOT EXISTS idx_alert_record_tenant   ON monitor_alert_record(tenant_id);
CREATE        INDEX IF NOT EXISTS idx_alert_record_labels   ON monitor_alert_record USING GIN(labels);

-- 告警静默规则表
CREATE TABLE IF NOT EXISTS monitor_alert_silence (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    silence_name    VARCHAR(128)    NOT NULL,
    match_labels    JSONB           NOT NULL DEFAULT '{}',
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,
    comment         VARCHAR(512),
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE monitor_alert_silence IS '告警静默规则表';

CREATE INDEX IF NOT EXISTS idx_alert_silence_tenant_id  ON monitor_alert_silence(tenant_id)            WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_silence_time_range ON monitor_alert_silence(start_time, end_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_silence_match      ON monitor_alert_silence USING GIN(match_labels) WHERE deleted = 0;

-- 通知渠道表
CREATE TABLE IF NOT EXISTS monitor_notify_channel (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    channel_name    VARCHAR(64)     NOT NULL,
    channel_type    VARCHAR(32)     NOT NULL,
    config_json     TEXT            NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE monitor_notify_channel IS '告警通知渠道表';
COMMENT ON COLUMN monitor_notify_channel.channel_type IS 'EMAIL | DINGTALK | WECHAT_WORK | WEBHOOK';
COMMENT ON COLUMN monitor_notify_channel.config_json  IS '渠道配置 JSON，AES-256-GCM 加密存储';

CREATE INDEX IF NOT EXISTS idx_notify_channel_tenant_id ON monitor_notify_channel(tenant_id)    WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_notify_channel_type      ON monitor_notify_channel(channel_type) WHERE deleted = 0;

-- 通知发送日志表
CREATE TABLE IF NOT EXISTS monitor_notify_log (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    alert_record_id BIGINT          NOT NULL,
    channel_id      BIGINT          NOT NULL,
    channel_type    VARCHAR(32)     NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    retry_count     SMALLINT        NOT NULL DEFAULT 0,
    error_message   TEXT,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE monitor_notify_log IS '通知发送日志';

CREATE INDEX IF NOT EXISTS idx_notify_log_alert_record_id ON monitor_notify_log(alert_record_id);
CREATE INDEX IF NOT EXISTS idx_notify_log_channel_id      ON monitor_notify_log(channel_id);
CREATE INDEX IF NOT EXISTS idx_notify_log_created_at      ON monitor_notify_log(created_at DESC);
