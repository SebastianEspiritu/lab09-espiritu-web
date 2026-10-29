package com.tecsup.jwt;

import com.tecsup.service.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private final Key signingKey;
    private final long expirationMs;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expirationMs) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");
        }
        if (expirationMs <= 0) throw new IllegalArgumentException("JWT_EXPIRATION debe ser positivo");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        Date issuedAt = new Date();
        return Jwts.builder().setSubject(principal.getUsername()).setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }
    public String getUserNameFromJwtToken(String token) {
        return parser().parseClaimsJws(token).getBody().getSubject();
    }
    public boolean validateJwtToken(String token) {
        try {
            // Exigir un JWS firmado: no aceptar JWT sin firma.
            var claims = parser().parseClaimsJws(token).getBody();
            return claims.getSubject() != null && !claims.getSubject().isBlank()
                    && claims.getExpiration() != null;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
    private JwtParser parser() {
        return Jwts.parserBuilder().setSigningKey(signingKey).build();
    }
}
