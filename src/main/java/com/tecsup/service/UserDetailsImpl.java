package com.tecsup.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tecsup.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String username;
    private final String email;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = List.copyOf(authorities);
    }
    public static UserDetailsImpl build(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase(Locale.ROOT)))
                .toList();
        return new UserDetailsImpl(user.getId(), user.getUsername(), user.getEmail(),
                user.getPassword(), authorities);
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayUsername() { return username; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof UserDetailsImpl user && Objects.equals(id, user.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
