package com.focalizze.Focalizze.utils;

import com.focalizze.Focalizze.models.User;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // --- AQUÍ ESTÁ EL CAMBIO EN LA GENERACIÓN ---
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Verificamos si el UserDetails es realmente nuestra entidad User
        if (userDetails instanceof User user) {
            // Guardamos la versión actual del token en el payload
            claims.put("tokenVersion", user.getTokenVersion());
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // 10 horas de expiración
        final long expirationTime = 1000 * 60 * 60 * 10;

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    // --- AQUÍ ESTÁ EL CAMBIO EN LA VALIDACIÓN ---
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);

        // 1. Validación estándar: usuario coincide y no ha expirado
        boolean isStandardValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(token));

        if (!isStandardValid) {
            return false;
        }

        // 2. Validación de versión (Logout masivo)
        if (userDetails instanceof User user) {
            // Extraemos la versión que está escrita dentro del token
            Integer tokenVersionInClaim = extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));

            // Si el token no tiene versión (es viejo) o la versión no coincide con la actual en BD
            // significa que la sesión fue revocada.
            if (tokenVersionInClaim == null || !tokenVersionInClaim.equals(user.getTokenVersion())) {
                return false;
            }
        }

        return true;
    }
}
