package com.tecsup.config;

import com.tecsup.jwt.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"No autorizado\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Acceso denegado\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/test/all").permitAll()
                        .requestMatchers("/api/test/admin").hasRole("ADMIN")
                        .requestMatchers("/api/test/mod").hasRole("MODERATOR")
                        .requestMatchers("/api/test/user").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        // Ejecutarlo una sola vez, dentro de Spring Security.
        registration.setEnabled(false);
        return registration;
    }
}
