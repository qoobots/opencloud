-- ============================================================
-- OpenCloud 控制台 — Flyway 初始化脚本
-- 版本：V1.0.0
-- 内容：
--   1. 认证授权模块：auth_user_credential / auth_login_log / auth_operation_log
--   2. 系统管理模块：sys_user / sys_role / sys_user_role / sys_menu / sys_role_menu / sys_operation_log
--   3. 初始数据：超管角色 / 只读角色 / 默认管理员账号 / 基础菜单树
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. 认证授权模块
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS auth_user_credential (
    id                    BIGINT          PRIMARY KEY,
    user_id               BIGINT          NOT NULL,
    tenant_id             BIGINT          NOT NULL,
    username              VARCHAR(64)     NOT NULL,
    password_hash         VARCHAR(128)    NOT NULL,
    status                VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    login_fail_count      SMALLINT        NOT NULL DEFAULT 0,
    lock_expire_time      TIMESTAMP,
    last_login_time       TIMESTAMP,
    password_update_time  TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT,
    deleted               SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  auth_user_credential IS '用户凭证表（登录认证信息）';
COMMENT ON COLUMN auth_user_credential.status IS 'ACTIVE=正常 | DISABLED=禁用 | LOCKED=锁定';

CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_cred_user_id  ON auth_user_credential(user_id)             WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_cred_username ON auth_user_credential(username, tenant_id)  WHERE deleted = 0;

-- 登录日志
CREATE TABLE IF NOT EXISTS auth_login_log (
    id              BIGINT          PRIMARY KEY,
    user_id         BIGINT,
    tenant_id       BIGINT,
    username        VARCHAR(64)     NOT NULL,
    login_ip        VARCHAR(64),
    user_agent      VARCHAR(512),
    login_time      TIMESTAMP       NOT NULL DEFAULT NOW(),
    logout_time     TIMESTAMP,
    status          VARCHAR(16)     NOT NULL,
    fail_reason     VARCHAR(128),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth_login_log IS '登录日志表';

CREATE INDEX IF NOT EXISTS idx_auth_login_log_user_id    ON auth_login_log(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_username   ON auth_login_log(username);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_login_time ON auth_login_log(login_time DESC);

-- 认证操作审计日志
CREATE TABLE IF NOT EXISTS auth_operation_log (
    id              BIGINT          PRIMARY KEY,
    operator_id     BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    target_user_id  BIGINT,
    operation_type  VARCHAR(32)     NOT NULL,
    operate_ip      VARCHAR(64),
    operate_time    TIMESTAMP       NOT NULL DEFAULT NOW(),
    result          VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    remark          VARCHAR(256),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth_operation_log IS '认证授权操作审计日志';

CREATE INDEX IF NOT EXISTS idx_auth_op_log_operator_id  ON auth_operation_log(operator_id);
CREATE INDEX IF NOT EXISTS idx_auth_op_log_operate_time ON auth_operation_log(operate_time DESC);

-- ─────────────────────────────────────────────────────────────
-- 2. 系统管理模块
-- ─────────────────────────────────────────────────────────────

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT          PRIMARY KEY,
    tenant_id   BIGINT          NOT NULL,
    username    VARCHAR(64)     NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(20),
    avatar      VARCHAR(512),
    gender      SMALLINT        NOT NULL DEFAULT 0,
    status      SMALLINT        NOT NULL DEFAULT 0,
    remark      VARCHAR(512),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_user IS '系统用户表（基础信息）';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_username  ON sys_user(username, tenant_id) WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_sys_user_tenant_id ON sys_user(tenant_id)            WHERE deleted = 0;

-- 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT          PRIMARY KEY,
    tenant_id   BIGINT          NOT NULL,
    role_name   VARCHAR(64)     NOT NULL,
    role_code   VARCHAR(64)     NOT NULL,
    description VARCHAR(256),
    status      SMALLINT        NOT NULL DEFAULT 0,
    sort        INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_role IS '系统角色表';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_code ON sys_role(role_code, tenant_id) WHERE deleted = 0;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色多对多关联';

-- 菜单权限表（树形）
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT          PRIMARY KEY,
    parent_id   BIGINT          NOT NULL DEFAULT 0,
    menu_name   VARCHAR(64)     NOT NULL,
    menu_type   CHAR(1)         NOT NULL,
    path        VARCHAR(256),
    component   VARCHAR(256),
    permission  VARCHAR(128),
    icon        VARCHAR(64),
    sort        INT             NOT NULL DEFAULT 0,
    hidden      SMALLINT        NOT NULL DEFAULT 0,
    status      SMALLINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_menu IS '菜单权限表（M=目录 C=菜单 B=按钮）';

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id) WHERE deleted = 0;

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单多对多关联';

-- 系统操作日志
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT,
    module          VARCHAR(64),
    operation       VARCHAR(128),
    method          VARCHAR(256),
    request_method  VARCHAR(16),
    request_url     VARCHAR(512),
    request_params  TEXT,
    response_result TEXT,
    ip              VARCHAR(64),
    user_agent      VARCHAR(512),
    user_id         BIGINT,
    username        VARCHAR(64),
    status          SMALLINT        NOT NULL DEFAULT 0,
    error_msg       TEXT,
    cost_time       BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE sys_operation_log IS '系统操作日志';

CREATE INDEX IF NOT EXISTS idx_sys_op_log_user_id    ON sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_op_log_tenant_id  ON sys_operation_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_op_log_created_at ON sys_operation_log(created_at DESC);

-- ─────────────────────────────────────────────────────────────
-- 3. 初始数据
-- ─────────────────────────────────────────────────────────────

-- 角色
INSERT INTO sys_role (id, tenant_id, role_name, role_code, description, status, sort, created_at, updated_at, deleted)
VALUES
    (1, 1, '超级管理员', 'ROLE_ADMIN',  '系统最高权限', 0, 1, NOW(), NOW(), 0),
    (2, 1, '只读用户',   'ROLE_VIEWER', '仅查看权限',   0, 2, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 默认管理员（密码 Admin@123，BCrypt cost=12）
INSERT INTO sys_user (id, tenant_id, username, nickname, status, created_at, updated_at, deleted)
VALUES (1, 1, 'admin', 'Administrator', 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_credential (id, user_id, tenant_id, username, password_hash, status, login_fail_count, created_at, updated_at, deleted)
VALUES (1, 1, 1, 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LuU5BL6N6zPzB8Gm2', 'ACTIVE', 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

-- 菜单树（系统管理）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(100, 0,   '系统管理',   'M', '/system',           NULL,                      NULL,                    'setting', 10, 0, 0, NOW(), NOW(), 0),
(101, 100, '用户管理',   'C', '/system/user',       'system/user/index',       NULL,                    'user',     1, 0, 0, NOW(), NOW(), 0),
(102, 100, '角色管理',   'C', '/system/role',       'system/role/index',       NULL,                    'role',     2, 0, 0, NOW(), NOW(), 0),
(103, 100, '菜单管理',   'C', '/system/menu',       'system/menu/index',       NULL,                    'menu',     3, 0, 0, NOW(), NOW(), 0),
(104, 100, '操作日志',   'C', '/system/oplog',      'system/oplog/index',      NULL,                    'log',      4, 0, 0, NOW(), NOW(), 0),
(105, 100, '登录日志',   'C', '/system/loginlog',   'system/loginlog/index',   NULL,                    'login',    5, 0, 0, NOW(), NOW(), 0),
-- 用户管理按钮
(1010, 101, '查询',    'B', NULL, NULL, 'system:user:list',      NULL, 1, 0, 0, NOW(), NOW(), 0),
(1011, 101, '新增',    'B', NULL, NULL, 'system:user:add',       NULL, 2, 0, 0, NOW(), NOW(), 0),
(1012, 101, '修改',    'B', NULL, NULL, 'system:user:edit',      NULL, 3, 0, 0, NOW(), NOW(), 0),
(1013, 101, '删除',    'B', NULL, NULL, 'system:user:delete',    NULL, 4, 0, 0, NOW(), NOW(), 0),
(1014, 101, '重置密码', 'B', NULL, NULL, 'system:user:resetpwd', NULL, 5, 0, 0, NOW(), NOW(), 0),
-- 角色管理按钮
(1020, 102, '查询', 'B', NULL, NULL, 'system:role:list',   NULL, 1, 0, 0, NOW(), NOW(), 0),
(1021, 102, '新增', 'B', NULL, NULL, 'system:role:add',    NULL, 2, 0, 0, NOW(), NOW(), 0),
(1022, 102, '修改', 'B', NULL, NULL, 'system:role:edit',   NULL, 3, 0, 0, NOW(), NOW(), 0),
(1023, 102, '删除', 'B', NULL, NULL, 'system:role:delete', NULL, 4, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 菜单树（云平台管理）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(200, 0,   '云平台管理', 'M', '/cloud',              NULL,                       NULL, 'cloud',   20, 0, 0, NOW(), NOW(), 0),
(201, 200, '集群配置',   'C', '/cloud/cluster',      'cloud/cluster/index',      NULL, 'cluster',  1, 0, 0, NOW(), NOW(), 0),
(202, 200, '云主机',     'C', '/cloud/server',       'cloud/server/index',       NULL, 'server',   2, 0, 0, NOW(), NOW(), 0),
(203, 200, '网络管理',   'C', '/cloud/network',      'cloud/network/index',      NULL, 'network',  3, 0, 0, NOW(), NOW(), 0),
(204, 200, '存储管理',   'C', '/cloud/storage',      'cloud/storage/index',      NULL, 'storage',  4, 0, 0, NOW(), NOW(), 0),
(205, 200, 'K8s 管理',  'C', '/cloud/kubernetes',   'cloud/kubernetes/index',   NULL, 'k8s',      5, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 菜单树（监控告警）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(300, 0,   '监控告警', 'M', '/monitor',              NULL,                          NULL, 'monitor', 30, 0, 0, NOW(), NOW(), 0),
(301, 300, '指标查询', 'C', '/monitor/metrics',      'monitor/metrics/index',       NULL, 'metrics',  1, 0, 0, NOW(), NOW(), 0),
(302, 300, 'Grafana',  'C', '/monitor/grafana',      'monitor/grafana/index',       NULL, 'grafana',  2, 0, 0, NOW(), NOW(), 0),
(303, 300, '告警规则', 'C', '/monitor/rules',        'monitor/rules/index',         NULL, 'rule',     3, 0, 0, NOW(), NOW(), 0),
(304, 300, '告警记录', 'C', '/monitor/records',      'monitor/records/index',       NULL, 'alert',    4, 0, 0, NOW(), NOW(), 0),
(305, 300, '静默规则', 'C', '/monitor/silence',      'monitor/silence/index',       NULL, 'silence',  5, 0, 0, NOW(), NOW(), 0),
(306, 300, '通知渠道', 'C', '/monitor/channels',     'monitor/channels/index',      NULL, 'channel',  6, 0, 0, NOW(), NOW(), 0),
(307, 300, '通知日志', 'C', '/monitor/notify-log',   'monitor/notify-log/index',    NULL, 'log',      7, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 绑定超管角色与所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0
ON CONFLICT DO NOTHING;
