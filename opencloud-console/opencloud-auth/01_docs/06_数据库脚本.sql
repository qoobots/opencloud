-- ============================================================
-- opencloud-auth 数据库初始化脚本
-- 数据库：PostgreSQL 15+
-- Schema ：auth
-- 版本   ：v1.0.0
-- 更新   ：2026-03-31
-- ============================================================
-- 执行顺序：
--   1. 创建 Schema
--   2. 创建枚举类型
--   3. 创建表 + 索引 + 注释
--   4. 初始化数据（超级管理员凭证）
-- ============================================================

-- ------------------------------------------------------------
-- 0. 扩展（uuid 生成）
-- ------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ============================================================
-- 1. Schema
-- ============================================================
CREATE SCHEMA IF NOT EXISTS auth;

-- 后续建表均使用 search_path，可按需调整
SET search_path TO auth, public;


-- ============================================================
-- 2. 枚举类型
-- ============================================================

-- 账号状态
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
        CREATE TYPE auth.account_status AS ENUM ('ACTIVE', 'DISABLED', 'LOCKED');
    END IF;
END$$;

-- 登录/登出事件状态
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'login_status') THEN
        CREATE TYPE auth.login_status AS ENUM ('SUCCESS', 'FAILED', 'LOGOUT', 'FORCE_LOGOUT');
    END IF;
END$$;

-- 操作日志类型
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'operation_type') THEN
        CREATE TYPE auth.operation_type AS ENUM ('CHANGE_PASSWORD', 'FORCE_LOGOUT', 'UNLOCK_ACCOUNT');
    END IF;
END$$;


-- ============================================================
-- 3. 表结构
-- ============================================================

-- ------------------------------------------------------------
-- 3.1 用户凭证表 auth_user_credential
-- ------------------------------------------------------------
--   存储用户认证凭证，与 system 模块 sys_user 通过 user_id 关联但解耦。
--   status 使用 varchar 而非枚举类型，方便 MyBatis-Plus 直接映射。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_user_credential (
    id                   bigserial       NOT NULL,
    user_id              varchar(36)     NOT NULL,
    tenant_id            varchar(36)     NOT NULL,
    username             varchar(64)     NOT NULL,
    password_hash        varchar(128)    NOT NULL,
    status               varchar(16)     NOT NULL DEFAULT 'ACTIVE',
    login_fail_count     smallint        NOT NULL DEFAULT 0,
    lock_expire_time     timestamptz,
    last_login_time      timestamptz,
    password_update_time timestamptz,
    created_at           timestamptz     NOT NULL DEFAULT now(),
    updated_at           timestamptz     NOT NULL DEFAULT now(),
    deleted_at           timestamptz,

    CONSTRAINT pk_auth_user_credential    PRIMARY KEY (id),
    CONSTRAINT chk_auc_status             CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED')),
    CONSTRAINT chk_auc_login_fail_count   CHECK (login_fail_count >= 0)
);

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_auc_username
    ON auth.auth_user_credential (username)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_auc_user_id
    ON auth.auth_user_credential (user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_auc_tenant
    ON auth.auth_user_credential (tenant_id);

CREATE INDEX IF NOT EXISTS idx_auc_status
    ON auth.auth_user_credential (status)
    WHERE deleted_at IS NULL;

-- 注释
COMMENT ON TABLE  auth.auth_user_credential                          IS '用户认证凭证表 - 存储登录账号、密码哈希、账号状态及安全相关字段';
COMMENT ON COLUMN auth.auth_user_credential.id                       IS '自增主键';
COMMENT ON COLUMN auth.auth_user_credential.user_id                  IS '关联 sys_user.id（UUID 格式），租户内唯一';
COMMENT ON COLUMN auth.auth_user_credential.tenant_id                IS '所属租户 ID';
COMMENT ON COLUMN auth.auth_user_credential.username                 IS '登录账号，同一租户内唯一（逻辑删除不计入唯一约束）';
COMMENT ON COLUMN auth.auth_user_credential.password_hash            IS 'BCrypt 加密密码（cost=12），禁止明文存储';
COMMENT ON COLUMN auth.auth_user_credential.status                   IS '账号状态: ACTIVE=正常 | DISABLED=禁用 | LOCKED=锁定';
COMMENT ON COLUMN auth.auth_user_credential.login_fail_count         IS '连续登录失败次数；≥5 次触发锁定；登录成功 / 管理员解锁后清零';
COMMENT ON COLUMN auth.auth_user_credential.lock_expire_time         IS '锁定自动解除时间；下次登录时若 lock_expire_time <= now() 则自动解锁';
COMMENT ON COLUMN auth.auth_user_credential.last_login_time          IS '最近一次登录成功时间';
COMMENT ON COLUMN auth.auth_user_credential.password_update_time     IS '最近一次修改密码时间';
COMMENT ON COLUMN auth.auth_user_credential.created_at               IS '记录创建时间';
COMMENT ON COLUMN auth.auth_user_credential.updated_at               IS '记录最近更新时间（MyBatis-Plus 自动维护）';
COMMENT ON COLUMN auth.auth_user_credential.deleted_at               IS '逻辑删除时间；非 NULL 表示已删除';


-- ------------------------------------------------------------
-- 3.2 登录日志表 auth_login_log
-- ------------------------------------------------------------
--   记录每次登录 / 登出行为，只追加写入。
--   login_time / logout_time 在同一行，登出时补填 logout_time。
--   数据保留策略：90 天归档（由定时任务处理）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_login_log (
    id          bigserial       NOT NULL,
    user_id     varchar(36)     NOT NULL,
    tenant_id   varchar(36)     NOT NULL,
    username    varchar(64)     NOT NULL,
    login_ip    varchar(64),
    user_agent  varchar(512),
    login_time  timestamptz,
    logout_time timestamptz,
    status      varchar(16)     NOT NULL,
    fail_reason varchar(256),
    created_at  timestamptz     NOT NULL DEFAULT now(),

    CONSTRAINT pk_auth_login_log  PRIMARY KEY (id),
    CONSTRAINT chk_all_status     CHECK (status IN ('SUCCESS', 'FAILED', 'LOGOUT', 'FORCE_LOGOUT'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_all_user_time
    ON auth.auth_login_log (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_all_tenant_time
    ON auth.auth_login_log (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_all_status
    ON auth.auth_login_log (status);

CREATE INDEX IF NOT EXISTS idx_all_login_time
    ON auth.auth_login_log (login_time DESC);

-- 注释
COMMENT ON TABLE  auth.auth_login_log              IS '登录/登出日志表 - 只追加写入，用于审计与异常分析';
COMMENT ON COLUMN auth.auth_login_log.id           IS '自增主键';
COMMENT ON COLUMN auth.auth_login_log.user_id      IS '登录用户 ID';
COMMENT ON COLUMN auth.auth_login_log.tenant_id    IS '所属租户 ID';
COMMENT ON COLUMN auth.auth_login_log.username     IS '登录账号（冗余字段，方便日志查询，不做外键约束）';
COMMENT ON COLUMN auth.auth_login_log.login_ip     IS '客户端 IP，优先取 X-Forwarded-For，其次取 RemoteAddr';
COMMENT ON COLUMN auth.auth_login_log.user_agent   IS '浏览器 / 客户端标识（HTTP User-Agent）';
COMMENT ON COLUMN auth.auth_login_log.login_time   IS '登录时间（登录事件写入）';
COMMENT ON COLUMN auth.auth_login_log.logout_time  IS '登出时间（登出 / 强制下线时补填）';
COMMENT ON COLUMN auth.auth_login_log.status       IS '事件状态: SUCCESS=登录成功 | FAILED=登录失败 | LOGOUT=主动登出 | FORCE_LOGOUT=被强制下线';
COMMENT ON COLUMN auth.auth_login_log.fail_reason  IS '登录失败原因（仅 status=FAILED 时有值）: PASSWORD_ERROR / ACCOUNT_LOCKED / ACCOUNT_DISABLED 等';
COMMENT ON COLUMN auth.auth_login_log.created_at   IS '记录创建时间';


-- ------------------------------------------------------------
-- 3.3 操作日志表 auth_operation_log
-- ------------------------------------------------------------
--   记录修改密码、强制下线、解锁账号等关键管理操作，用于安全审计。
--   数据保留策略：永久保留（或按合规要求定期归档）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_operation_log (
    id              bigserial       NOT NULL,
    operator_id     varchar(36)     NOT NULL,
    tenant_id       varchar(36)     NOT NULL,
    target_user_id  varchar(36),
    operation_type  varchar(32)     NOT NULL,
    operate_time    timestamptz     NOT NULL DEFAULT now(),
    operate_ip      varchar(64),
    remark          varchar(256),

    CONSTRAINT pk_auth_operation_log   PRIMARY KEY (id),
    CONSTRAINT chk_aol_operation_type  CHECK (operation_type IN ('CHANGE_PASSWORD', 'FORCE_LOGOUT', 'UNLOCK_ACCOUNT'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_aol_operator_time
    ON auth.auth_operation_log (operator_id, operate_time DESC);

CREATE INDEX IF NOT EXISTS idx_aol_target_time
    ON auth.auth_operation_log (target_user_id, operate_time DESC);

CREATE INDEX IF NOT EXISTS idx_aol_type
    ON auth.auth_operation_log (operation_type);

CREATE INDEX IF NOT EXISTS idx_aol_tenant_time
    ON auth.auth_operation_log (tenant_id, operate_time DESC);

-- 注释
COMMENT ON TABLE  auth.auth_operation_log                 IS '关键管理操作审计日志表 - 用于安全审计，永久保留';
COMMENT ON COLUMN auth.auth_operation_log.id              IS '自增主键';
COMMENT ON COLUMN auth.auth_operation_log.operator_id     IS '操作人用户 ID（管理员或本人）';
COMMENT ON COLUMN auth.auth_operation_log.tenant_id       IS '操作人所属租户 ID';
COMMENT ON COLUMN auth.auth_operation_log.target_user_id  IS '被操作用户 ID；自操作（修改密码）时与 operator_id 相同';
COMMENT ON COLUMN auth.auth_operation_log.operation_type  IS '操作类型: CHANGE_PASSWORD=修改密码 | FORCE_LOGOUT=强制下线 | UNLOCK_ACCOUNT=解锁账号';
COMMENT ON COLUMN auth.auth_operation_log.operate_time    IS '操作时间';
COMMENT ON COLUMN auth.auth_operation_log.operate_ip      IS '操作人客户端 IP';
COMMENT ON COLUMN auth.auth_operation_log.remark          IS '附加说明（可选）';


-- ============================================================
-- 4. 触发器：updated_at 自动更新
-- ============================================================
CREATE OR REPLACE FUNCTION auth.fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auc_updated_at ON auth.auth_user_credential;
CREATE TRIGGER trg_auc_updated_at
    BEFORE UPDATE ON auth.auth_user_credential
    FOR EACH ROW EXECUTE FUNCTION auth.fn_update_updated_at();


-- ============================================================
-- 5. 初始化数据
-- ============================================================

-- ------------------------------------------------------------
-- 5.1 初始化默认租户超级管理员凭证
-- ------------------------------------------------------------
-- 说明：
--   - tenant_id  = '00000000-0000-0000-0000-000000000001'（平台默认租户）
--   - user_id    = '00000000-0000-0000-0000-000000000001'（超级管理员）
--   - username   = 'superadmin'
--   - password   = 'Admin@12345'（BCrypt cost=12 预生成，首次登录后请立即修改）
--   - 生产环境部署前必须修改默认密码
-- ------------------------------------------------------------
INSERT INTO auth.auth_user_credential
    (user_id, tenant_id, username, password_hash, status, login_fail_count, created_at, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'superadmin',
        -- BCrypt(Admin@12345, cost=12)  -- TODO: 生产环境替换为实际哈希值
        '$2a$12$eImiTXuWVxfM37uY4JANjQ.G0X35x5JMHpRVkC2RzXkQUKxpAzqIC',
        'ACTIVE',
        0,
        now(),
        now()
    )
ON CONFLICT DO NOTHING;


-- ============================================================
-- 6. 视图（可选，方便运维查询）
-- ============================================================

-- 近 7 天登录失败统计（按用户）
CREATE OR REPLACE VIEW auth.v_recent_login_failures AS
SELECT
    user_id,
    username,
    tenant_id,
    COUNT(*)                         AS fail_count,
    MAX(login_time)                  AS last_fail_time,
    COUNT(DISTINCT login_ip)         AS distinct_ip_count
FROM auth.auth_login_log
WHERE status = 'FAILED'
  AND created_at >= now() - INTERVAL '7 days'
GROUP BY user_id, username, tenant_id;

COMMENT ON VIEW auth.v_recent_login_failures IS '近 7 天登录失败统计，按用户聚合';

-- 当前被锁定账号
CREATE OR REPLACE VIEW auth.v_locked_accounts AS
SELECT
    user_id,
    tenant_id,
    username,
    login_fail_count,
    lock_expire_time,
    CASE
        WHEN lock_expire_time IS NULL OR lock_expire_time > now() THEN '锁定中'
        ELSE '锁定已到期（待自动解锁）'
    END AS lock_state,
    created_at
FROM auth.auth_user_credential
WHERE status = 'LOCKED'
  AND deleted_at IS NULL;

COMMENT ON VIEW auth.v_locked_accounts IS '当前处于锁定状态的账号列表';


-- ============================================================
-- END
-- ============================================================
-- 验证建表结果：
--   SELECT table_name FROM information_schema.tables
--   WHERE table_schema = 'auth' ORDER BY table_name;
-- ============================================================
