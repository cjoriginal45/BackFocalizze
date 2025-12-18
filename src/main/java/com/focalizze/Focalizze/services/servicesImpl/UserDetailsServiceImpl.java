package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 * Loads user-specific data during authentication.
 * <p>
 * Implementación personalizada de {@link UserDetailsService} de Spring Security.
 * Carga datos específicos del usuario durante la autenticación.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by username or email.
     * Marked as Transactional to prevent LazyInitializationException if roles/authorities are fetched.
     * <p>
     * Carga un usuario por nombre de usuario o correo electrónico.
     * Marcado como Transactional para prevenir LazyInitializationException si se obtienen roles/autoridades.
     *
     * @param identifier Username or Email. / Nombre de usuario o Correo.
     * @return UserDetails object. / Objeto UserDetails.
     * @throws UsernameNotFoundException If user not found.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el identificador: " + identifier));
    }

}
