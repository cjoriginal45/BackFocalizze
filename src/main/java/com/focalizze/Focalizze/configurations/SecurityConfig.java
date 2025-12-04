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

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final AuthenticationProvider authenticationProvider;

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
                        .requestMatchers("/api/auth/**").permitAll()

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

                        // C. TODO LO DEMÁS REQUIERE LOGIN
                        .anyRequest().authenticated()
                )

                // 5. Proveedor de autenticación
                .authenticationProvider(authenticationProvider)

                // 6. Añadir el filtro JWT antes del filtro de usuario/password estándar
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (Angular, React, Postman/Local)
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000", "http://localhost:8080"));

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
