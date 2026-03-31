package com.qoobot.opencloud.system.exception;

import lombok.Getter;

/**
 * 系统管理模块业务异常
 */
@Getter
public class SystemException extends RuntimeException {

    private final String code;

    private SystemException(String code, String message) {
        super(message);
        this.code = code;
    }

    // ---- 用户管理 ----
    public static SystemException usernameExists()          { return new SystemException("SYS_1001", "用户名已被占用"); }
    public static SystemException userNotFound()            { return new SystemException("SYS_1002", "用户不存在"); }
    public static SystemException selfOperationNotAllowed() { return new SystemException("SYS_1003", "不能对自身账号执行此操作"); }
    public static SystemException builtinUserNotAllowed()   { return new SystemException("SYS_1004", "内置账号不允许此操作"); }
    public static SystemException crossTenantForbidden()    { return new SystemException("SYS_1005", "禁止跨租户操作"); }

    // ---- 角色管理 ----
    public static SystemException roleCodeExists()          { return new SystemException("SYS_2001", "角色编码已存在"); }
    public static SystemException roleNotFound()            { return new SystemException("SYS_2002", "角色不存在"); }
    public static SystemException builtinRoleNotAllowed()   { return new SystemException("SYS_2003", "内置角色不允许此操作"); }
    public static SystemException roleInUse()               { return new SystemException("SYS_2004", "角色下存在用户，请先解绑"); }

    // ---- 菜单管理 ----
    public static SystemException menuNotFound()            { return new SystemException("SYS_3001", "菜单不存在"); }
    public static SystemException menuHasChildren()         { return new SystemException("SYS_3002", "请先删除子菜单"); }
    public static SystemException menuBoundByRole()         { return new SystemException("SYS_3003", "菜单已被角色绑定，请先解绑"); }
    public static SystemException buttonNoChildren()        { return new SystemException("SYS_3004", "按钮类型不允许添加子节点"); }
    public static SystemException permissionExists()        { return new SystemException("SYS_3005", "权限标识已存在"); }

    // ---- 部门管理 ----
    public static SystemException deptNotFound()            { return new SystemException("SYS_4001", "部门不存在"); }
    public static SystemException deptHasChildren()         { return new SystemException("SYS_4002", "请先删除子部门"); }
    public static SystemException deptHasUsers()            { return new SystemException("SYS_4003", "部门下存在用户，请先迁移"); }
    public static SystemException deptLevelExceeded()       { return new SystemException("SYS_4004", "部门层级最多 5 层"); }

    // ---- 字典管理 ----
    public static SystemException dictTypeNotFound()        { return new SystemException("SYS_5001", "字典类型不存在"); }
    public static SystemException dictTypeCodeExists()      { return new SystemException("SYS_5002", "字典类型编码已存在"); }
    public static SystemException systemDictNotAllowed()    { return new SystemException("SYS_5003", "系统内置字典不允许删除"); }
    public static SystemException dictValueExists()         { return new SystemException("SYS_5004", "字典值在当前类型下已存在"); }

    // ---- 租户管理 ----
    public static SystemException tenantNotFound()          { return new SystemException("SYS_6001", "租户不存在"); }
    public static SystemException tenantCodeExists()        { return new SystemException("SYS_6002", "租户编码已存在"); }
    public static SystemException defaultTenantNotAllowed() { return new SystemException("SYS_6003", "默认租户不允许删除"); }
    public static SystemException tenantHasUsers()          { return new SystemException("SYS_6004", "租户下存在用户，请先删除用户"); }
    public static SystemException tenantAccessDenied()      { return new SystemException("SYS_6005", "无权访问租户管理"); }

    // ---- 个人中心 ----
    public static SystemException invalidEmail()            { return new SystemException("SYS_7001", "邮箱格式不正确"); }
    public static SystemException invalidPhone()            { return new SystemException("SYS_7002", "手机号格式不正确"); }
}
