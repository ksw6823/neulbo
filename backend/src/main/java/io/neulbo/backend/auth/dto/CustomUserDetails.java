package io.neulbo.backend.auth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 인증에서 사용되는 사용자 인증 정보
 */
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String provider;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId, String provider) {
        this.userId = userId;
        this.provider = provider;
        // 기본적으로 모든 사용자에게 USER 역할 부여
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public CustomUserDetails(Long userId, String provider, List<String> roles) {
        this.userId = userId;
        this.provider = provider;
        // 역할 문자열 리스트를 GrantedAuthority로 변환
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    public CustomUserDetails(Long userId, String provider, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.provider = provider;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // OAuth 로그인이므로 패스워드 없음
    }

    @Override
    public String getUsername() {
        return userId.toString(); // userId를 username으로 사용
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
        return true;
    }
} 