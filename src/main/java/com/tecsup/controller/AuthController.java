package com.tecsup.controller;

import com.tecsup.dto.*;
import com.tecsup.entity.Role;
import com.tecsup.entity.User;
import com.tecsup.jwt.JwtUtils;
import com.tecsup.repository.RoleRepository;
import com.tecsup.repository.UserRepository;
import com.tecsup.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                          AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest request) {
        if (users.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username ya existe"));
        }
        if (users.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email ya existe"));
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Password supera 72 bytes"));
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        Set<String> requested = request.getRoles();
        if (requested == null || requested.isEmpty()) requested = Set.of("USER");
        Set<Role> assigned = new HashSet<>();
        for (String value : requested) {
            String name = value == null ? "" : value.toUpperCase(Locale.ROOT);
            // Mantiene el comportamiento de la guía: roles desconocidos pasan a USER.
            if (!Set.of("ADMIN", "MODERATOR").contains(name)) name = "USER";
            assigned.add(roles.findByName(name).orElseThrow(() -> new IllegalStateException("Rol no encontrado")));
        }
        user.setRoles(assigned);
        users.save(user);
        return ResponseEntity.ok(new MessageResponse("Usuario registrado correctamente"));
    }
    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        var authorities = user.getAuthorities().stream().map(item -> item.getAuthority()).sorted().toList();
        return ResponseEntity.ok(new JwtResponse(user.getId(), user.getUsername(), user.getEmail(), token, authorities));
    }
}
