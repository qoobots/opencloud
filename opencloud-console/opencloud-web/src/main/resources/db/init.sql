-- ============================================================
-- OpenCloud 控制台数据库初始化脚本
-- 数据库：PostgreSQL 16
-- 扩展：pgvector（为后续 AI 功能准备）
-- ============================================================

-- 创建 pgvector 扩展（需要先安装 pgvector 插件）
-- CREATE EXTENSION IF NOT EXISTS vector;

-- ─────────────────────────────────────────────────────────────
-- 用户表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT          PRIMARY KEY,
    username    VARCHAR(64)     NOT NULL UNIQUE COMMENT '登录名',
    password    VARCHAR(128)    NOT NULL COMMENT 'BCrypt 加密密码',
    nickname    VARCHAR(64)     COMMENT '昵称',
    email       VARCHAR(128)    COMMENT '邮箱',
    phone       VARCHAR(20)     COMMENT '手机号',
    avatar      VARCHAR(512)    COMMENT '头像 URL',
    status      SMALLINT        NOT NULL DEFAULT 0 COMMENT '状态：0=正常，1=禁用',
    remark      VARCHAR(512)    COMMENT '备注',
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.status IS '0=正常 1=禁用';

-- ─────────────────────────────────────────────────────────────
-- 角色表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT          PRIMARY KEY,
    role_name   VARCHAR(64)     NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(64)     NOT NULL UNIQUE COMMENT '角色编码，如 ROLE_ADMIN',
    description VARCHAR(256)    COMMENT '描述',
    status      SMALLINT        NOT NULL DEFAULT 0,
    sort        INT             NOT NULL DEFAULT 0,
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_role IS '系统角色';

-- ─────────────────────────────────────────────────────────────
-- 用户角色关联表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- ─────────────────────────────────────────────────────────────
-- 菜单/权限表（树形）
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT          PRIMARY KEY,
    parent_id   BIGINT          NOT NULL DEFAULT 0 COMMENT '父节点，0=根',
    menu_name   VARCHAR(64)     NOT NULL,
    menu_type   CHAR(1)         NOT NULL COMMENT 'M=目录 C=菜单 B=按钮',
    path        VARCHAR(256)    COMMENT '前端路由',
    component   VARCHAR(256)    COMMENT '前端组件路径',
    permission  VARCHAR(128)    COMMENT '权限标识，如 system:user:list',
    icon        VARCHAR(64)     COMMENT '图标',
    sort        INT             NOT NULL DEFAULT 0,
    hidden      SMALLINT        NOT NULL DEFAULT 0,
    status      SMALLINT        NOT NULL DEFAULT 0,
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_menu IS '菜单权限';

-- ─────────────────────────────────────────────────────────────
-- 角色菜单关联表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- ─────────────────────────────────────────────────────────────
-- 操作日志表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          PRIMARY KEY,
    module          VARCHAR(64)     COMMENT '模块名',
    operation       VARCHAR(128)    COMMENT '操作描述',
    method          VARCHAR(256)    COMMENT '请求方法全路径',
    request_method  VARCHAR(16)     COMMENT 'HTTP 方法',
    request_url     VARCHAR(512)    COMMENT '请求 URL',
    request_params  TEXT            COMMENT '请求参数（JSON）',
    response_result TEXT            COMMENT '响应结果（JSON，可选）',
    ip              VARCHAR(64)     COMMENT '操作 IP',
    user_agent      VARCHAR(512)    COMMENT 'User-Agent',
    user_id         BIGINT          COMMENT '操作用户 ID',
    username        VARCHAR(64)     COMMENT '操作用户名',
    status          SMALLINT        COMMENT '0=成功，1=失败',
    error_msg       TEXT            COMMENT '异常信息',
    cost_time       BIGINT          COMMENT '耗时（ms）',
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_op_log_user_id    ON sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_op_log_create_time ON sys_operation_log(create_time DESC);

COMMENT ON TABLE sys_operation_log IS '操作日志';

-- ─────────────────────────────────────────────────────────────
-- 登录日志表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT      PRIMARY KEY,
    username    VARCHAR(64) COMMENT '登录用户名',
    ip          VARCHAR(64) COMMENT '登录 IP',
    user_agent  VARCHAR(512),
    status      SMALLINT    COMMENT '0=成功，1=失败',
    message     VARCHAR(256) COMMENT '提示消息',
    create_time TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_login_log_username    ON sys_login_log(username);
CREATE INDEX IF NOT EXISTS idx_login_log_create_time ON sys_login_log(create_time DESC);

-- ─────────────────────────────────────────────────────────────
-- 集群配置表（OpenStack / Ceph / K8s 连接信息，AES 加密存储）
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cloud_cluster_config (
    id              BIGINT          PRIMARY KEY,
    cluster_name    VARCHAR(64)     NOT NULL UNIQUE COMMENT '集群名称',
    cluster_type    VARCHAR(32)     NOT NULL COMMENT 'OPENSTACK / CEPH / KUBERNETES',
    config_json     TEXT            NOT NULL COMMENT '配置 JSON（AES 加密）',
    description     VARCHAR(256),
    status          SMALLINT        NOT NULL DEFAULT 0 COMMENT '0=启用，1=禁用',
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE cloud_cluster_config IS '云平台集群连接配置';

-- ─────────────────────────────────────────────────────────────
-- 告警规则表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alert_rule (
    id              BIGINT          PRIMARY KEY,
    rule_name       VARCHAR(128)    NOT NULL COMMENT '规则名称',
    expr            TEXT            NOT NULL COMMENT 'PromQL 表达式',
    duration        VARCHAR(32)     COMMENT '持续时间，如 5m',
    severity        VARCHAR(32)     NOT NULL DEFAULT 'warning' COMMENT 'critical/warning/info',
    summary         VARCHAR(256)    COMMENT '摘要',
    description     TEXT            COMMENT '描述',
    status          SMALLINT        NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

-- ─────────────────────────────────────────────────────────────
-- 告警记录表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alert_record (
    id              BIGINT          PRIMARY KEY,
    rule_id         BIGINT          COMMENT '触发规则 ID',
    alert_name      VARCHAR(128)    NOT NULL,
    severity        VARCHAR(32)     NOT NULL,
    summary         VARCHAR(512),
    description     TEXT,
    labels          JSONB           COMMENT '标签（JSONB，支持索引）',
    status          VARCHAR(32)     NOT NULL DEFAULT 'firing' COMMENT 'firing/resolved',
    fired_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW()
    -- 预留 embedding 字段，用于 AI 根因分析
    -- embedding     vector(1536)
);

CREATE INDEX IF NOT EXISTS idx_alert_record_status     ON alert_record(status);
CREATE INDEX IF NOT EXISTS idx_alert_record_fired_at   ON alert_record(fired_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_record_labels     ON alert_record USING GIN(labels);

COMMENT ON TABLE alert_record IS '告警记录';

-- ─────────────────────────────────────────────────────────────
-- 通知渠道表
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notify_channel (
    id              BIGINT          PRIMARY KEY,
    channel_name    VARCHAR(64)     NOT NULL COMMENT '渠道名',
    channel_type    VARCHAR(32)     NOT NULL COMMENT 'EMAIL / DINGTALK / WEBHOOK',
    config_json     TEXT            NOT NULL COMMENT '渠道配置（AES 加密）',
    status          SMALLINT        NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

-- ============================================================
-- 初始数据
-- ============================================================

-- 超级管理员角色
INSERT INTO sys_role (id, role_name, role_code, description, status, sort, create_time, update_time, deleted)
VALUES (1, '超级管理员', 'ROLE_ADMIN', '系统最高权限', 0, 1, NOW(), NOW(), 0)
ON CONFLICT (role_code) DO NOTHING;

-- 只读角色
INSERT INTO sys_role (id, role_name, role_code, description, status, sort, create_time, update_time, deleted)
VALUES (2, '只读用户', 'ROLE_VIEWER', '仅查看权限', 0, 2, NOW(), NOW(), 0)
ON CONFLICT (role_code) DO NOTHING;

-- 默认管理员账号（密码：Admin@123，BCrypt 加密）
INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time, deleted)
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Administrator', 0, NOW(), NOW(), 0)
ON CONFLICT (username) DO NOTHING;

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)
ON CONFLICT DO NOTHING;
