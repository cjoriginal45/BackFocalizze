package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.LoginRequestDto;
import com.focalizze.Focalizze.dto.LoginResponseDto;
import com.focalizze.Focalizze.dto.mappers.LoginMapper;
import com.focalizze.Focalizze.models.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.focalizze.Focalizze.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    private final LoginMapper loginMapper;

    public LoginController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                           JwtUtil jwtUtil, LoginMapper loginMapper) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.loginMapper = loginMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {

        // AUTHENTICATION WITH SPRING SECURITY
        System.out.println("contrasenia: "+loginRequest.password());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.identifier(), loginRequest.password())
            );
        } catch (Exception e) {
            System.out.println("exception: "+e);
            // Si las credenciales son inválidas, devolvemos un error 401 Unauthorized.
            // If the credentials are invalid, we return a 401 Unauthorized error.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Si la autenticación fue exitosa, cargamos los UserDetails.
        // If authentication was successful, we load the UserDetails.
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.identifier());

        // Hacemos un cast del UserDetails a nuestra entidad User.
        // We cast the UserDetails to our User entity.
        User user = (User) userDetails;

        // Generamos el token JWT.
        // We generate the JWT token.
        final String token = jwtUtil.generateToken(userDetails);

        System.out.println("token: "+ token);

        // Le pasamos la entidad 'user' y el 'token' al mapper.
        // We pass the 'user' entity and the 'token' to the mapper.
        LoginResponseDto response = loginMapper.toDto(user, token);

        // Devolvemos la respuesta.
        // Return the response.
        return ResponseEntity.ok(response);
    }
}