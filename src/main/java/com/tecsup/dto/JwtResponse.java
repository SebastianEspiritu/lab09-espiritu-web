package com.tecsup.dto;

import java.util.List;

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private final Long id;
    private final String username;
    private final String email;
    private final List<String> roles;

    public JwtResponse(Long id, String username, String email, String token, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.token = token;
        this.roles = List.copyOf(roles);
    }
    public String getAccessToken() { return token; }
    public void setAccessToken(String token) { this.token = token; }
    public String getTokenType() { return type; }
    public void setTokenType(String type) { this.type = type; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
}
