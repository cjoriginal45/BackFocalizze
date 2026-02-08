package com.focalizze.Focalizze.configurations;

import com.focalizze.Focalizze.utils.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;


/**
 * Security configuration class.
 * Defines the security filter chain, CORS settings, and route authorization rules.
 * <p>
 * Clase de configuración de seguridad.
 * Define la cadena de filtros de seguridad, configuraciones CORS y reglas de autorización de rutas.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final AuthenticationProvider authenticationProvider;

    @Value("${app.cors.allowed-origins:*}") 
    private String allowedOrigins;

    /**
     * Configures the security filter chain.
     * <p>
     * Configura la cadena de filtros de seguridad.
     *
     * @param http HttpSecurity object. / Objeto HttpSecurity.
     * @return The built SecurityFilterChain. / El SecurityFilterChain construido.
     * @throws Exception If configuration fails. / Si la configuración falla.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Configuración de CORS (Permitir peticiones del Front)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Deshabilitar CSRF (Sintaxis moderna para APIs Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Gestión de Sesión (Sin cookies, Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Reglas de Autorización (El orden importa: de más específico a más general)
                .authorizeHttpRequests(auth -> auth
                        // A. RUTAS PÚBLICAS (Sin Token)
                        // Autenticación, Registro y 2FA
                        .requestMatchers("/api/auth/**", "/api/health").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // Imágenes y recursos estáticos
                        .requestMatchers("/images/**", "/api/profiles/avatars/**").permitAll()

                        // B. LECTURA PÚBLICA (GET)
                        .requestMatchers(HttpMethod.GET,
                                "/api/profiles/**",
                                "/api/thread/**",
                                "/api/categories/**",
                                "/api/search/**",
                                "/api/threads/*/comments"
                        ).permitAll()

                        .requestMatchers("/api/comments/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 5. Proveedor de autenticación
                .authenticationProvider(authenticationProvider)

                // 6. Añadir el filtro JWT antes del filtro de usuario/password estándar
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines CORS configuration.
     * Uses immutable lists for thread safety and performance.
     * <p>
     * Define la configuración CORS.
     * Utiliza listas inmutables para seguridad de hilos y rendimiento.
     *
     * @return The CORS source. / La fuente CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (Angular, React, Postman/Local)
         configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(","))); 

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Cabeceras permitidas (Authorization es vital para el JWT)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));

        // Permitir credenciales (cookies/headers auth)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
