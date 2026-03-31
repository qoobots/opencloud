-- ============================================================
-- opencloud-system 数据库初始化脚本
-- 数据库：PostgreSQL 15+
-- Schema：system
-- 创建时间：2026-03-31
-- ============================================================

-- ============================================================
-- Schema
-- ============================================================
CREATE SCHEMA IF NOT EXISTS system;

-- ============================================================
-- 租户表 sys_tenant（先建，其他表 tenant_id 引用它）
-- ============================================================
CREATE TABLE system.sys_tenant (
    id             varchar(36)   PRIMARY KEY,
    tenant_code    varchar(32)   NOT NULL,
    tenant_name    varchar(64)   NOT NULL,
    contact_name   varchar(32),
    contact_email  varchar(128),
    status         varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    is_default     boolean       NOT NULL DEFAULT false,
    expire_time    timestamptz,
    remark         varchar(256),
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    deleted        smallint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_st_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_st_code   ON system.sys_tenant(tenant_code) WHERE deleted = 0;
CREATE INDEX        idx_st_status ON system.sys_tenant(status);

COMMENT ON TABLE  system.sys_tenant             IS '租户信息表';
COMMENT ON COLUMN system.sys_tenant.tenant_code IS '租户编码，全局唯一，创建后不可修改';
COMMENT ON COLUMN system.sys_tenant.is_default  IS '是否默认租户，默认租户不可删除';
COMMENT ON COLUMN system.sys_tenant.expire_time IS '有效期，NULL 表示永久有效';

-- ============================================================
-- 部门表 sys_dept
-- ============================================================
CREATE TABLE system.sys_dept (
    id          bigserial     PRIMARY KEY,
    tenant_id   varchar(36)   NOT NULL,
    parent_id   bigint        NOT NULL DEFAULT 0,
    dept_name   varchar(64)   NOT NULL,
    leader      varchar(32),
    phone       varchar(16),
    email       varchar(128),
    sort        integer       NOT NULL DEFAULT 0,
    status      varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sd_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_sd_parent ON system.sys_dept(parent_id);
CREATE INDEX idx_sd_tenant ON system.sys_dept(tenant_id);

COMMENT ON TABLE  system.sys_dept           IS '部门/组织架构树';
COMMENT ON COLUMN system.sys_dept.parent_id IS '父部门 ID，0 表示根节点';

-- ============================================================
-- 用户表 sys_user
-- ============================================================
CREATE TABLE system.sys_user (
    id          varchar(36)   PRIMARY KEY,
    tenant_id   varchar(36)   NOT NULL,
    dept_id     bigint,
    username    varchar(64)   NOT NULL,
    nickname    varchar(64)   NOT NULL,
    avatar      varchar(512),
    email       varchar(128),
    phone       varchar(16),
    status      varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    is_builtin  boolean       NOT NULL DEFAULT false,
    sort        integer       NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_su_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_su_username_tenant ON system.sys_user(username, tenant_id) WHERE deleted = 0;
CREATE INDEX        idx_su_tenant          ON system.sys_user(tenant_id);
CREATE INDEX        idx_su_dept            ON system.sys_user(dept_id);
CREATE INDEX        idx_su_status          ON system.sys_user(status);

COMMENT ON TABLE  system.sys_user            IS '用户基础信息表（不含认证凭证）';
COMMENT ON COLUMN system.sys_user.id         IS '用户 ID（UUID），与 auth_user_credential.user_id 关联';
COMMENT ON COLUMN system.sys_user.is_builtin IS '是否内置账号，内置账号不允许删除/禁用';
COMMENT ON COLUMN system.sys_user.status     IS '账号状态: ACTIVE=正常 DISABLED=禁用';

-- ============================================================
-- 角色表 sys_role
-- ============================================================
CREATE TABLE system.sys_role (
    id          bigserial     PRIMARY KEY,
    tenant_id   varchar(36)   NOT NULL,
    role_name   varchar(64)   NOT NULL,
    role_code   varchar(64)   NOT NULL,
    description varchar(256),
    is_builtin  boolean       NOT NULL DEFAULT false,
    status      varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    sort        integer       NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sr_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_sr_code_tenant ON system.sys_role(role_code, tenant_id) WHERE deleted = 0;
CREATE INDEX        idx_sr_tenant      ON system.sys_role(tenant_id);

COMMENT ON TABLE  system.sys_role            IS '角色定义表';
COMMENT ON COLUMN system.sys_role.role_code  IS '角色编码，如 SUPER_ADMIN / ROLE_ADMIN，同租户内唯一';
COMMENT ON COLUMN system.sys_role.is_builtin IS '是否内置角色，内置角色不可删除/修改编码';

-- ============================================================
-- 用户-角色关联表 sys_user_role
-- ============================================================
CREATE TABLE system.sys_user_role (
    id          bigserial   PRIMARY KEY,
    user_id     varchar(36) NOT NULL,
    role_id     bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_sur_user_role ON system.sys_user_role(user_id, role_id);
CREATE INDEX        idx_sur_role      ON system.sys_user_role(role_id);

COMMENT ON TABLE system.sys_user_role IS '用户-角色多对多关联表';

-- ============================================================
-- 菜单/权限表 sys_menu
-- ============================================================
CREATE TABLE system.sys_menu (
    id          bigserial     PRIMARY KEY,
    parent_id   bigint        NOT NULL DEFAULT 0,
    menu_name   varchar(64)   NOT NULL,
    menu_type   smallint      NOT NULL,
    path        varchar(256),
    component   varchar(256),
    icon        varchar(64),
    permission  varchar(128),
    visible     boolean       NOT NULL DEFAULT true,
    sort        integer       NOT NULL DEFAULT 0,
    remark      varchar(256),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sm_type CHECK (menu_type IN (0, 1, 2))
);

CREATE INDEX        idx_sm_parent     ON system.sys_menu(parent_id);
CREATE INDEX        idx_sm_type       ON system.sys_menu(menu_type);
CREATE UNIQUE INDEX idx_sm_permission ON system.sys_menu(permission) WHERE deleted = 0 AND permission IS NOT NULL;

COMMENT ON TABLE  system.sys_menu            IS '菜单/权限树';
COMMENT ON COLUMN system.sys_menu.menu_type  IS '类型: 0=目录 1=菜单 2=按钮/权限';
COMMENT ON COLUMN system.sys_menu.permission IS '权限标识，格式 module:resource:action，按钮节点必填，全平台唯一';
COMMENT ON COLUMN system.sys_menu.visible    IS '是否在侧边栏显示';

-- ============================================================
-- 角色-菜单关联表 sys_role_menu
-- ============================================================
CREATE TABLE system.sys_role_menu (
    id          bigserial   PRIMARY KEY,
    role_id     bigint      NOT NULL,
    menu_id     bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_srm_role_menu ON system.sys_role_menu(role_id, menu_id);
CREATE INDEX        idx_srm_menu      ON system.sys_role_menu(menu_id);

COMMENT ON TABLE system.sys_role_menu IS '角色-菜单多对多关联表';

-- ============================================================
-- 字典类型表 sys_dict_type
-- ============================================================
CREATE TABLE system.sys_dict_type (
    id          bigserial     PRIMARY KEY,
    dict_type   varchar(64)   NOT NULL,
    dict_name   varchar(64)   NOT NULL,
    is_system   boolean       NOT NULL DEFAULT false,
    status      varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    remark      varchar(256),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_sdt_type ON system.sys_dict_type(dict_type) WHERE deleted = 0;

COMMENT ON TABLE  system.sys_dict_type           IS '字典类型表';
COMMENT ON COLUMN system.sys_dict_type.is_system IS '是否系统内置字典，系统字典不允许删除';

-- ============================================================
-- 字典数据表 sys_dict_data
-- ============================================================
CREATE TABLE system.sys_dict_data (
    id          bigserial     PRIMARY KEY,
    dict_type   varchar(64)   NOT NULL,
    dict_label  varchar(64)   NOT NULL,
    dict_value  varchar(64)   NOT NULL,
    sort        integer       NOT NULL DEFAULT 0,
    is_default  boolean       NOT NULL DEFAULT false,
    status      varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    remark      varchar(256),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     smallint      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_sdd_type_value ON system.sys_dict_data(dict_type, dict_value) WHERE deleted = 0;
CREATE INDEX        idx_sdd_type       ON system.sys_dict_data(dict_type);

COMMENT ON TABLE  system.sys_dict_data            IS '字典数据表';
COMMENT ON COLUMN system.sys_dict_data.dict_type  IS '所属字典类型编码，关联 sys_dict_type.dict_type';
COMMENT ON COLUMN system.sys_dict_data.is_default IS '是否为该字典类型的默认选项';

-- ============================================================
-- 操作日志表 sys_operation_log
-- ============================================================
CREATE TABLE system.sys_operation_log (
    id              bigserial     PRIMARY KEY,
    user_id         varchar(36),
    tenant_id       varchar(36),
    username        varchar(64),
    module_name     varchar(64)   NOT NULL,
    operation_type  varchar(64)   NOT NULL,
    method          varchar(8)    NOT NULL,
    request_url     varchar(512)  NOT NULL,
    request_param   text,
    response_result text,
    exception_msg   text,
    status          varchar(16)   NOT NULL,
    cost_time       integer,
    operate_ip      varchar(64),
    operate_time    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT chk_sol_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_sol_user   ON system.sys_operation_log(user_id, operate_time DESC);
CREATE INDEX idx_sol_tenant ON system.sys_operation_log(tenant_id, operate_time DESC);
CREATE INDEX idx_sol_module ON system.sys_operation_log(module_name);
CREATE INDEX idx_sol_status ON system.sys_operation_log(status);
CREATE INDEX idx_sol_time   ON system.sys_operation_log(operate_time DESC);

COMMENT ON TABLE  system.sys_operation_log                IS '操作日志表，只追加写入，不修改删除';
COMMENT ON COLUMN system.sys_operation_log.request_param  IS '请求参数（JSON，已脱敏 password/token 等字段）';
COMMENT ON COLUMN system.sys_operation_log.cost_time      IS '接口耗时（毫秒）';

-- ============================================================
-- updated_at 自动更新触发器
-- ============================================================
CREATE OR REPLACE FUNCTION system.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_st_updated_at  BEFORE UPDATE ON system.sys_tenant     FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sd_updated_at  BEFORE UPDATE ON system.sys_dept       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_su_updated_at  BEFORE UPDATE ON system.sys_user       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sr_updated_at  BEFORE UPDATE ON system.sys_role       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sm_updated_at  BEFORE UPDATE ON system.sys_menu       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sdt_updated_at BEFORE UPDATE ON system.sys_dict_type  FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sdd_updated_at BEFORE UPDATE ON system.sys_dict_data  FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认租户
INSERT INTO system.sys_tenant (id, tenant_code, tenant_name, is_default, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', '默认租户', true, 'ACTIVE');

-- 超级管理员用户（密码由 auth 模块写入 auth_user_credential）
INSERT INTO system.sys_user (id, tenant_id, username, nickname, status, is_builtin)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'admin', '超级管理员', 'ACTIVE', true);

-- 超级管理员角色
INSERT INTO system.sys_role (tenant_id, role_name, role_code, is_builtin, status, sort)
VALUES ('00000000-0000-0000-0000-000000000001', '超级管理员', 'SUPER_ADMIN', true, 'ACTIVE', 0);

-- 绑定超级管理员用户与角色（角色 ID = 1）
INSERT INTO system.sys_user_role (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000002', 1);

-- 系统菜单初始化（系统管理目录）
INSERT INTO system.sys_menu (parent_id, menu_name, menu_type, path, component, icon, sort)
VALUES (0, '系统管理', 0, '/system', 'Layout', 'Setting', 100);

-- 系统内置字典类型
INSERT INTO system.sys_dict_type (dict_type, dict_name, is_system, status) VALUES
('sys_user_status',   '用户状态',   true, 'ACTIVE'),
('sys_login_status',  '登录状态',   true, 'ACTIVE'),
('sys_yes_no',        '是否',       true, 'ACTIVE'),
('sys_menu_type',     '菜单类型',   true, 'ACTIVE'),
('sys_op_status',     '操作状态',   true, 'ACTIVE');

-- 字典数据
INSERT INTO system.sys_dict_data (dict_type, dict_label, dict_value, sort) VALUES
('sys_user_status',  '正常',    'ACTIVE',        1),
('sys_user_status',  '禁用',    'DISABLED',      2),
('sys_login_status', '登录成功',  'SUCCESS',       1),
('sys_login_status', '登录失败',  'FAILED',        2),
('sys_login_status', '主动登出',  'LOGOUT',        3),
('sys_login_status', '强制下线',  'FORCE_LOGOUT',  4),
('sys_yes_no',       '是',       '1',             1),
('sys_yes_no',       '否',       '0',             2),
('sys_menu_type',    '目录',     '0',             1),
('sys_menu_type',    '菜单',     '1',             2),
('sys_menu_type',    '按钮',     '2',             3),
('sys_op_status',    '成功',    'SUCCESS',        1),
('sys_op_status',    '失败',    'FAILED',         2);

-- ============================================================
-- 运维视图
-- ============================================================

-- 最近 7 天各模块操作统计
CREATE OR REPLACE VIEW system.v_operation_stat_7d AS
SELECT
    module_name,
    operation_type,
    status,
    COUNT(*) AS cnt,
    AVG(cost_time) AS avg_cost_ms
FROM system.sys_operation_log
WHERE operate_time >= now() - INTERVAL '7 days'
GROUP BY module_name, operation_type, status
ORDER BY cnt DESC;

-- 各租户用户数量统计
CREATE OR REPLACE VIEW system.v_tenant_user_count AS
SELECT
    t.id              AS tenant_id,
    t.tenant_name,
    t.status,
    COUNT(u.id)       AS user_count
FROM system.sys_tenant t
LEFT JOIN system.sys_user u
    ON u.tenant_id = t.id AND u.deleted = 0
WHERE t.deleted = 0
GROUP BY t.id, t.tenant_name, t.status
ORDER BY user_count DESC;
