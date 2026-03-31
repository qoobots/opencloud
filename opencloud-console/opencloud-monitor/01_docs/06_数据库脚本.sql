-- ========================================================
-- opencloud-monitor 模块数据库脚本
-- 对应文档：02_业务设计.md, 03_应用设计.md, 04_数据设计.md, 05_技术设计.md
-- 生成时间：2026-03-31
-- ========================================================

-- --------------------------------------------------------
-- 1. 创建自定义类型
-- --------------------------------------------------------

-- 通知渠道类型枚举
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notify_channel_type') THEN
        CREATE TYPE notify_channel_type AS ENUM ('EMAIL', 'DINGTALK', 'WECHAT_WORK', 'WEBHOOK');
    END IF;
END$$;

-- --------------------------------------------------------
-- 2. 创建告警规则表 (monitor_alert_rule)
-- --------------------------------------------------------

DROP TABLE IF EXISTS monitor_alert_rule CASCADE;

CREATE TABLE monitor_alert_rule (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           VARCHAR(36)   NOT NULL,                   -- 租户 ID
    rule_name           VARCHAR(100)  NOT NULL,                   -- 规则名称（同租户唯一）
    description         VARCHAR(500)  DEFAULT '',                 -- 规则描述
    promql_expr         TEXT          NOT NULL,                   -- PromQL 表达式
    severity            VARCHAR(20)   NOT NULL                    -- 严重级别：INFO / WARNING / CRITICAL
                        CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    duration            VARCHAR(10)   NOT NULL DEFAULT '5m',      -- 持续时间（如 5m），最小 1m，最大 60m
    notify_channel_ids  TEXT          DEFAULT '[]',              -- 绑定的通知渠道 ID 列表（JSON 数组）
    notify_on_resolve   BOOLEAN       NOT NULL DEFAULT FALSE,     -- 告警恢复时是否发送恢复通知
    status              VARCHAR(20)   NOT NULL DEFAULT 'ENABLED'  -- ENABLED / DISABLED
                        CHECK (status IN ('ENABLED', 'DISABLED')),
    deleted             SMALLINT      NOT NULL DEFAULT 0,         -- 逻辑删除：0=正常，1=已删除
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(36)   DEFAULT NULL,               -- 创建人 userId
    updated_by          VARCHAR(36)   DEFAULT NULL
);

COMMENT ON TABLE  monitor_alert_rule IS '告警规则表';
COMMENT ON COLUMN monitor_alert_rule.tenant_id IS '租户 ID';
COMMENT ON COLUMN monitor_alert_rule.rule_name IS '规则名称，同租户下唯一';
COMMENT ON COLUMN monitor_alert_rule.promql_expr IS 'PromQL 表达式，新增/编辑时须通过 Prometheus 语法校验';
COMMENT ON COLUMN monitor_alert_rule.severity IS '严重级别：INFO / WARNING / CRITICAL';
COMMENT ON COLUMN monitor_alert_rule.duration IS '持续时间：告警条件持续满足此时长才触发，格式如 5m、10m';
COMMENT ON COLUMN monitor_alert_rule.notify_channel_ids IS '绑定通知渠道 ID 列表，JSON 数组格式，如 [1,2,3]';
COMMENT ON COLUMN monitor_alert_rule.notify_on_resolve IS '告警恢复时是否发送恢复通知';
COMMENT ON COLUMN monitor_alert_rule.status IS 'ENABLED=启用，DISABLED=禁用';
COMMENT ON COLUMN monitor_alert_rule.deleted IS '逻辑删除：0=正常，1=已删除';

-- 唯一索引：同租户下规则名唯一
CREATE UNIQUE INDEX idx_alert_rule_name_tenant ON monitor_alert_rule(tenant_id, rule_name) WHERE deleted = 0;

-- 普通索引：按租户+状态查询
CREATE INDEX idx_alert_rule_tenant_status ON monitor_alert_rule(tenant_id, status);

-- --------------------------------------------------------
-- 3. 创建告警记录表 (monitor_alert_record)
-- --------------------------------------------------------

DROP TABLE IF EXISTS monitor_alert_record CASCADE;

CREATE TABLE monitor_alert_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(36)   NOT NULL,
    rule_id         BIGINT        DEFAULT NULL,               -- 匹配到的规则 ID（可为 NULL，表示未知规则）
    rule_name       VARCHAR(100)  NOT NULL,                   -- 规则名称快照（规则删除后仍可查）
    alert_name      VARCHAR(200)  NOT NULL,                   -- AlertManager labels.alertname
    severity        VARCHAR(20)   NOT NULL
                    CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    instance        VARCHAR(200)  DEFAULT '',                 -- labels.instance
    labels          TEXT          NOT NULL DEFAULT '{}',      -- 完整 labels JSON
    annotations     TEXT          NOT NULL DEFAULT '{}',      -- 完整 annotations JSON
    summary         VARCHAR(500)  DEFAULT '',                 -- annotations.summary 快照（便于列表展示）
    status          VARCHAR(20)   NOT NULL DEFAULT 'FIRING'
                    CHECK (status IN ('FIRING', 'ACKNOWLEDGED', 'RESOLVED')),
    fired_at        TIMESTAMPTZ   NOT NULL,                   -- 告警触发时间（startsAt）
    resolved_at     TIMESTAMPTZ   DEFAULT NULL,               -- 告警解除时间（endsAt）
    ack_by          VARCHAR(36)   DEFAULT NULL,               -- 确认人 userId
    ack_at          TIMESTAMPTZ   DEFAULT NULL,               -- 确认时间
    ack_note        VARCHAR(500)  DEFAULT NULL,               -- 确认备注
    notified        BOOLEAN       NOT NULL DEFAULT FALSE,     -- 是否已发送通知（含是否命中静默）
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  monitor_alert_record IS '告警记录表，仅追加，不做逻辑删除';
COMMENT ON COLUMN monitor_alert_record.rule_id IS '匹配到的规则 ID；AlertManager 推送的告警如未匹配到规则，此字段为 NULL';
COMMENT ON COLUMN monitor_alert_record.rule_name IS '规则名称快照，即使规则被删除也能追溯';
COMMENT ON COLUMN monitor_alert_record.alert_name IS 'AlertManager labels.alertname';
COMMENT ON COLUMN monitor_alert_record.instance IS '告警实例地址';
COMMENT ON COLUMN monitor_alert_record.labels IS 'AlertManager 原始 labels JSON 字符串';
COMMENT ON COLUMN monitor_alert_record.annotations IS 'AlertManager 原始 annotations JSON 字符串';
COMMENT ON COLUMN monitor_alert_record.summary IS '告警摘要，便于列表展示';
COMMENT ON COLUMN monitor_alert_record.status IS 'FIRING=触发中，ACKNOWLEDGED=已确认，RESOLVED=已解决';
COMMENT ON COLUMN monitor_alert_record.notified IS 'false 表示命中静默或发送失败';

-- 唯一索引：幂等写入去重（alertName + instance + firedAt）
CREATE UNIQUE INDEX idx_alert_record_dedup ON monitor_alert_record(alert_name, instance, fired_at);

-- 普通索引
CREATE INDEX idx_alert_record_tenant_status ON monitor_alert_record(tenant_id, status);
CREATE INDEX idx_alert_record_tenant_fired ON monitor_alert_record(tenant_id, fired_at DESC);
CREATE INDEX idx_alert_record_rule_id ON monitor_alert_record(rule_id);

-- --------------------------------------------------------
-- 4. 创建通知渠道表 (monitor_notify_channel)
-- --------------------------------------------------------

DROP TABLE IF EXISTS monitor_notify_channel CASCADE;

CREATE TABLE monitor_notify_channel (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(36)   NOT NULL,
    channel_name    VARCHAR(100)  NOT NULL,                   -- 渠道名称
    channel_type    VARCHAR(20)   NOT NULL,                   -- 渠道类型：EMAIL / DINGTALK / WECHAT_WORK / WEBHOOK
    config          TEXT          NOT NULL DEFAULT '{}',      -- 渠道配置（JSON，敏感字段 AES-256-GCM 加密）
    description     VARCHAR(300)  DEFAULT '',
    deleted         SMALLINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36)   DEFAULT NULL,
    updated_by      VARCHAR(36)   DEFAULT NULL
);

COMMENT ON TABLE  monitor_notify_channel IS '通知渠道配置表';
COMMENT ON COLUMN monitor_notify_channel.channel_type IS '渠道类型：EMAIL / DINGTALK / WECHAT_WORK / WEBHOOK';
COMMENT ON COLUMN monitor_notify_channel.config IS '渠道配置 JSON，各类型字段不同；敏感字段（密码、secret、token）经 AES-256-GCM 加密存储';
COMMENT ON COLUMN monitor_notify_channel.deleted IS '逻辑删除：0=正常，1=已删除';

-- 普通索引
CREATE INDEX idx_notify_channel_tenant ON monitor_notify_channel(tenant_id) WHERE deleted = 0;

-- --------------------------------------------------------
-- 5. 创建通知发送日志表 (monitor_notify_log)
-- --------------------------------------------------------

DROP TABLE IF EXISTS monitor_notify_log CASCADE;

CREATE TABLE monitor_notify_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(36)   NOT NULL,
    alert_record_id BIGINT        NOT NULL,                   -- 关联告警记录 ID
    channel_id      BIGINT        NOT NULL,                   -- 关联通知渠道 ID
    channel_name    VARCHAR(100)  NOT NULL,                   -- 渠道名称快照
    channel_type    VARCHAR(20)   NOT NULL,                   -- 渠道类型快照
    is_test         BOOLEAN       NOT NULL DEFAULT FALSE,     -- 是否为测试发送
    status          VARCHAR(20)   NOT NULL
                    CHECK (status IN ('SUCCESS', 'FAILED')),
    error_msg       TEXT          DEFAULT NULL,               -- 失败原因
    retry_count     SMALLINT      NOT NULL DEFAULT 0,         -- 重试次数
    sent_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  monitor_notify_log IS '通知发送日志，只追加，不做逻辑删除';
COMMENT ON COLUMN monitor_notify_log.is_test IS '是否为测试发送';
COMMENT ON COLUMN monitor_notify_log.retry_count IS '重试次数，最大 3 次';

-- 普通索引
CREATE INDEX idx_notify_log_record ON monitor_notify_log(alert_record_id);
CREATE INDEX idx_notify_log_tenant ON monitor_notify_log(tenant_id, sent_at DESC);
CREATE INDEX idx_notify_log_channel ON monitor_notify_log(channel_id, sent_at DESC);

-- --------------------------------------------------------
-- 6. 创建告警静默表 (monitor_alert_silence)
-- --------------------------------------------------------

DROP TABLE IF EXISTS monitor_alert_silence CASCADE;

CREATE TABLE monitor_alert_silence (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(36)   NOT NULL,
    silence_name    VARCHAR(100)  NOT NULL,
    description     VARCHAR(300)  DEFAULT '',
    match_labels    TEXT          NOT NULL DEFAULT '[]',      -- 标签匹配规则（JSON 数组）
    start_at        TIMESTAMPTZ   NOT NULL,                   -- 静默开始时间
    end_at          TIMESTAMPTZ   NOT NULL,                   -- 静默结束时间
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'EXPIRED')), -- EXPIRED：手动停止或自动过期
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36)   DEFAULT NULL,
    updated_by      VARCHAR(36)   DEFAULT NULL
);

COMMENT ON TABLE  monitor_alert_silence IS '告警静默规则表';
COMMENT ON COLUMN monitor_alert_silence.match_labels IS 'JSON 数组，示例：[{"key":"alertname","value":"NodeDown"},{"key":"severity","value":"CRITICAL"}]，所有条件 AND 匹配';
COMMENT ON COLUMN monitor_alert_silence.status IS 'ACTIVE=生效中，EXPIRED=已停止或自动过期；按时间判断是否有效（需同时满足 status=ACTIVE AND now BETWEEN start_at AND end_at）';

-- 普通索引
CREATE INDEX idx_silence_tenant_status ON monitor_alert_silence(tenant_id, status);
CREATE INDEX idx_silence_time ON monitor_alert_silence(start_at, end_at);

-- --------------------------------------------------------
-- 7. 创建 updated_at 自动更新触发器
-- --------------------------------------------------------

-- 创建触发器函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为各表创建触发器
DROP TRIGGER IF EXISTS trg_monitor_alert_rule_updated_at ON monitor_alert_rule;
CREATE TRIGGER trg_monitor_alert_rule_updated_at
    BEFORE UPDATE ON monitor_alert_rule
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_monitor_notify_channel_updated_at ON monitor_notify_channel;
CREATE TRIGGER trg_monitor_notify_channel_updated_at
    BEFORE UPDATE ON monitor_notify_channel
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_monitor_alert_silence_updated_at ON monitor_alert_silence;
CREATE TRIGGER trg_monitor_alert_silence_updated_at
    BEFORE UPDATE ON monitor_alert_silence
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_monitor_alert_record_updated_at ON monitor_alert_record;
CREATE TRIGGER trg_monitor_alert_record_updated_at
    BEFORE UPDATE ON monitor_alert_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- --------------------------------------------------------
-- 8. 初始化示例数据（可选）
-- --------------------------------------------------------

-- 示例：插入一条测试用的邮件通知渠道配置（需替换为实际配置）
-- INSERT INTO monitor_notify_channel (tenant_id, channel_name, channel_type, config, description)
-- VALUES (
--     'default',
--     '运维邮件组',
--     'EMAIL',
--     '{"smtpHost":"smtp.example.com","smtpPort":465,"smtpUser":"alert@example.com","smtpPass":"<ENCRYPTED>","senderAddr":"alert@example.com","receiverAddrs":"ops@example.com","sslEnabled":true}',
--     '默认运维邮件通知渠道'
-- );

-- --------------------------------------------------------
-- 9. 权限说明
-- --------------------------------------------------------
-- 以下权限标识需在 opencloud-system 模块的 sys_menu 表中初始化：
-- monitor:metrics:query          - 查询监控指标（含 Grafana）
-- monitor:alert:rule:list        - 查看告警规则 / 告警记录 / 统计 / 静默列表
-- monitor:alert:rule:add         - 新增告警规则 / 静默
-- monitor:alert:rule:edit        - 编辑告警规则 / 启用禁用 / 停止静默
-- monitor:alert:rule:delete      - 删除告警规则 / 静默
-- monitor:alert:record:ack       - 确认告警记录
-- monitor:notify:channel:list    - 查看通知渠道 / 通知日志
-- monitor:notify:channel:add     - 新增通知渠道
-- monitor:notify:channel:edit    - 编辑通知渠道 / 发送测试消息
-- monitor:notify:channel:delete  - 删除通知渠道
-- --------------------------------------------------------
