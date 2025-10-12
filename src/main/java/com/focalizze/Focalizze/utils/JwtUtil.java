package com.focalizze.Focalizze.utils;

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

    // Una clave secreta para firmar el token.
    // A secret key to sign the token.
    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    private Key key;

    // Este método se ejecuta una vez después de que se inyectan las dependencias.
    // This method is executed once after dependencies are injected.
    @PostConstruct
    public void init() {
        // Convierte la clave secreta (String) en un objeto Key que jjwt puede usar.
        // Converts the secret key (String) to a Key object that jjwt can use.
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Extrae el nombre de usuario del token JWT.
    // Extract the username from the JWT token.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrae la fecha de expiración del token JWT.
    // Extract the JWT token expiration date.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Método genérico para extraer una "claim" del token.
    // Generic method to extract a claim from the token.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Usa la nueva API de jjwt con el objeto Key.
    // Use the new jjwt API with the Key object.
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Verifica si el token ha expirado.
    // Check if the token has expired.
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Genera un token JWT para un usuario.
    // Generate a JWT token for a user.
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    // Usa la nueva API de jjwt con el objeto Key.
    // Use the new jjwt API with the Key object.
    private String createToken(Map<String, Object> claims, String subject) {
        final long expirationTime = 1000 * 60 * 60 * 10;

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    // Valida si un token es correcto y no ha expirado.
    // Validates if a token is correct and has not expired.
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
