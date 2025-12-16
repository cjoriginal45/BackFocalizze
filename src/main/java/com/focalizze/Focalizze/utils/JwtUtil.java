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

/**
 * Utility class for JSON Web Token (JWT) operations.
 * Handles token generation, parsing, and validation (including version control for logout).
 * <p>
 * Clase de utilidad para operaciones con JSON Web Token (JWT).
 * Maneja la generación, análisis y validación de tokens (incluyendo control de versiones para cierre de sesión).
 */
@Component
public class JwtUtil {
    @Value("${jwt.secret.key}")
    private String SECRET_KEY;
    private Key key;
    private static final long JWT_TOKEN_VALIDITY = 1000 * 60 * 60 * 10;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Extracts the username (subject) from the token.
     * <p>
     * Extrae el nombre de usuario (sujeto) del token.
     *
     * @param token The JWT token. / El token JWT.
     * @return The username. / El nombre de usuario.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the token.
     * <p>
     * Extrae la fecha de expiración del token.
     *
     * @param token The JWT token. / El token JWT.
     * @return The expiration date. / La fecha de expiración.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim.
     * <p>
     * Método genérico para extraer un reclamo específico.
     *
     * @param token          The JWT token. / El token JWT.
     * @param claimsResolver Function to resolve the claim. / Función para resolver el reclamo.
     * @param <T>            The type of the claim. / El tipo del reclamo.
     * @return The claim value. / El valor del reclamo.
     */
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


    /**
     * Generates a new JWT token for the user.
     * Includes "tokenVersion" claim for invalidation support.
     * <p>
     * Genera un nuevo token JWT para el usuario.
     * Incluye el reclamo "tokenVersion" para soporte de invalidación.
     *
     * @param userDetails The user details. / Los detalles del usuario.
     * @return The signed JWT string. / La cadena JWT firmada.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Embed token version for revocation logic
        // Incrustar versión del token para lógica de revocación
        if (userDetails instanceof User user) {
            claims.put("tokenVersion", user.getTokenVersion());
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /**
     * Validates the token against the user details and versioning.
     * Checks if token is expired, if username matches, and if token version matches the DB.
     * <p>
     * Valida el token contra los detalles del usuario y el versionado.
     * Comprueba si el token ha expirado, si el usuario coincide y si la versión del token coincide con la BD.
     *
     * @param token       The JWT token. / El token JWT.
     * @param userDetails The loaded user details from DB. / Los detalles del usuario cargados de la BD.
     * @return {@code true} if valid, {@code false} otherwise.
     */
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
