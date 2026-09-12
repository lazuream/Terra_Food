package com.dayan.food.security;

import com.dayan.food.entity.enums.UserRole;
import com.dayan.food.entity.po.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/** Immutable session identity. Usernames remain display/login attributes, never identity keys. */
public record AppUserPrincipal(
        Long userId,
        String subjectId,
        long authVersion,
        String username,
        String password,
        UserRole role,
        boolean active
) implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getSubjectId(), user.getAuthVersion(),
                user.getUsername(), user.getPassword(), user.getRole(), user.isActive());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
