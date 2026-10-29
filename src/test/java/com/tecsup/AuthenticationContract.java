package com.tecsup;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecsup.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AuthenticationContract {
    private static final String SECRET = Optional.ofNullable(System.getenv("JWT_SECRET"))
            .orElseGet(() -> {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                return Base64.getEncoder().encodeToString(bytes);
            });
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> SECRET);
    }
    @Autowired TestRestTemplate http;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    final String suffix = UUID.randomUUID().toString();
    final String password = UUID.randomUUID().toString();
    final String userEmail = "user-" + suffix + "@test.local";
    final String adminEmail = "admin-" + suffix + "@test.local";
    String userToken;
    String adminToken;

    @BeforeAll
    void registerAndLogin() {
        assertThat(signup("user-" + suffix, userEmail, Set.of("USER")).getStatusCode().value()).isEqualTo(200);
        assertThat(signup("admin-" + suffix, adminEmail, Set.of("ADMIN")).getStatusCode().value()).isEqualTo(200);
        userToken = login(userEmail, password).getBody().get("accessToken").asText();
        adminToken = login(adminEmail, password).getBody().get("accessToken").asText();
    }
    ResponseEntity<JsonNode> signup(String username, String email, Set<String> roles) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        if (roles != null) body.put("roles", roles);
        return http.postForEntity("/api/auth/signup", body, JsonNode.class);
    }
    ResponseEntity<JsonNode> login(String email, String candidate) {
        return http.postForEntity("/api/auth/signin", Map.of("email", email, "password", candidate), JsonNode.class);
    }
    ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
    @Test void case1PublicAccess() {
        var response = get("/api/test/all", null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Public Content");
    }
    @Test void case2NoToken() {
        var response = get("/api/test/user", null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("No autorizado");
    }
    @Test void case3TamperedToken() {
        int start = userToken.lastIndexOf('.') + 1;
        String tampered = userToken.substring(0, start)
                + (userToken.charAt(start) == 'A' ? 'B' : 'A') + userToken.substring(start + 1);
        assertThat(get("/api/test/user", tampered).getStatusCode().value()).isEqualTo(401);
    }
    @Test void case4ValidUserToken() {
        var response = get("/api/test/user", userToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("User Content");
    }
    @Test void case5UserCannotAccessAdmin() {
        var response = get("/api/test/admin", userToken);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).contains("Acceso denegado");
    }
    @Test void case6AuthorizedAdmin() {
        var response = get("/api/test/admin", adminToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Admin Content");
    }
    @Test void adminCanAccessUser() {
        assertThat(get("/api/test/user", adminToken).getStatusCode().value()).isEqualTo(200);
    }
    @Test void passwordsAreHashedWithBcrypt() {
        var stored = users.findByEmail(userEmail).orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo(password).startsWith("$2");
        assertThat(encoder.matches(password, stored)).isTrue();
    }
    @Test void duplicateUsernameRejected() {
        assertThat(signup("user-" + suffix, "other-" + suffix + "@test.local", null)
                .getStatusCode().value()).isEqualTo(400);
    }
    @Test void duplicateEmailRejected() {
        assertThat(signup("other-" + suffix, userEmail, null).getStatusCode().value()).isEqualTo(400);
    }
    @Test void wrongPasswordRejected() {
        assertThat(login(userEmail, "incorrect-password").getStatusCode().value()).isEqualTo(401);
    }
    @Test void defaultRoleIsUser() {
        String email = "default-" + suffix + "@test.local";
        assertThat(signup("default-" + suffix, email, null).getStatusCode().value()).isEqualTo(200);
        var response = login(email, password);
        assertThat(response.getBody().get("roles").toString()).isEqualTo("[\"ROLE_USER\"]");
        assertThat(response.getBody().get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(response.getBody().get("username").asText()).isEqualTo(email);
    }
    @Test void expiredTokenRejected() {
        String token = Jwts.builder().setSubject(userEmail)
                .setIssuedAt(new Date(System.currentTimeMillis() - 120000))
                .setExpiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
        assertThat(get("/api/test/user", token).getStatusCode().value()).isEqualTo(401);
    }
    @Test void unsignedTokenRejected() {
        String token = Jwts.builder().setSubject(userEmail)
                .setExpiration(new Date(System.currentTimeMillis() + 60000)).compact();
        assertThat(get("/api/test/user", token).getStatusCode().value()).isEqualTo(401);
    }
    @Test void malformedTokenRejected() {
        assertThat(get("/api/test/user", "invalid-token").getStatusCode().value()).isEqualTo(401);
    }
    @Test void validTokenForUnknownUserRejected() {
        String token = Jwts.builder().setSubject("missing-" + suffix + "@test.local")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
        assertThat(get("/api/test/user", token).getStatusCode().value()).isEqualTo(401);
    }
    @Test void invalidSignupRejected() {
        var response = http.postForEntity("/api/auth/signup",
                Map.of("username", "", "email", "invalid", "password", "123"), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
    @Test void requestsDoNotCreateSessions() {
        assertThat(get("/api/test/user", userToken).getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(get("/api/test/user", null).getStatusCode().value()).isEqualTo(401);
    }
}
