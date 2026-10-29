package com.tecsup.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public class SignupRequest {
    @NotBlank @Size(max = 100)
    private String username;
    @NotBlank @Email @Size(max = 200)
    private String email;
    @NotBlank @Size(min = 6, max = 72)
    private String password;

    private Set<String> roles;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
