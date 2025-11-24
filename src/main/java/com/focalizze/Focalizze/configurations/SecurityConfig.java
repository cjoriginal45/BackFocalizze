package com.focalizze.Focalizze.configurations;

import com.focalizze.Focalizze.utils.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter, AuthenticationProvider authenticationProvider) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // --- BLOQUE 1: RUTAS PÚBLICAS (ACCESO SIN LOGIN) ---
                        // Se permite el acceso a todos los endpoints de autenticación y registro.
                        .requestMatchers(
                                "/api/auth/**",
                                "/images/**",
                                "/api/profiles/avatars/**" ).permitAll()

                        // Se permite la LECTURA (GET) de contenido público.
                        // Esto incluye ver perfiles, avatares, hilos individuales, categorías, y resultados de búsqueda.
                        .requestMatchers(HttpMethod.GET,
                                "/api/profiles/**",
                                "/api/thread/**",
                                "/api/categories/**",
                                "/api/search/**",
                                "/api/threads/*/comments"
                        ).permitAll()

                        // --- BLOQUE 2: RUTAS CON AUTENTICACIÓN ---
                        // Si una petición no coincide con ninguna de las reglas anteriores,
                        // esta regla final se aplica, exigiendo que el usuario esté autenticado.
                        // Esto protege automáticamente:
                        //   - GET /api/feed (tu feed personalizado)
                        //   - GET /api/saved-threads
                        //   - GET /api/users/me/interactions
                        //   - POST, PATCH, DELETE a /api/thread/**
                        //   - POST a /api/users/{username}/follow
                        //   - Y cualquier otro endpoint que crees en el futuro.
                        .requestMatchers("/api/comments/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider);

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // Bean para configurar CORS. Reemplaza tu clase WebConfig.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
