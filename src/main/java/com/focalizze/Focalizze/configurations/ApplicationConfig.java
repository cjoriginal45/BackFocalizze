package com.focalizze.Focalizze.configurations;

import com.focalizze.Focalizze.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Core configuration for application beans, primarily security components.
 * <p>
 * Configuración central para los beans de la aplicación, principalmente componentes de seguridad.
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    /**
     * Bean that tells Spring Security how to retrieve user details from the database.
     * <p>
     * Bean que le dice a Spring Security cómo recuperar los detalles del usuario de la base de datos.
     *
     * @return The UserDetailsService implementation. / La implementación de UserDetailsService.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Bean that defines the authentication provider.
     * Binds the UserDetailsService with the PasswordEncoder.
     * <p>
     * Bean que define el proveedor de autenticación.
     * Vincula el UserDetailsService con el PasswordEncoder.
     *
     * @return The AuthenticationProvider. / El AuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Bean for password encoding using BCrypt.
     * <p>
     * Bean para la codificación de contraseñas usando BCrypt.
     *
     * @return The PasswordEncoder. / El PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean that exposes the AuthenticationManager.
     * <p>
     * Bean que expone el AuthenticationManager.
     *
     * @param config The AuthenticationConfiguration. / La AuthenticationConfiguration.
     * @return The AuthenticationManager. / El AuthenticationManager.
     * @throws Exception If configuration fails. / Si la configuración falla.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
