package com.qoobot.opencloud.common.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Spring Security 登录用户信息载体
 */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;          // 0=正常，1=禁用
    private Set<String> roles;       // 角色编码集合，如 ROLE_ADMIN
    private Set<String> permissions; // 权限标识集合，如 system:user:list
    private List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 0;
    }
}
