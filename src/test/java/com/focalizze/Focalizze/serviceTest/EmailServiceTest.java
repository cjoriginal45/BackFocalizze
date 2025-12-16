package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.services.servicesImpl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Configuramos el mock para que devuelva un MimeMessage válido cuando se le pida
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendPasswordResetEmail: Debería enviar el correo correctamente")
    void sendPasswordResetEmail_ShouldSendEmail() {
        // Given
        String to = "user@test.com";
        String token = "reset-token-123";

        // When
        emailService.sendPasswordResetEmail(to, token);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendTwoFactorCode: Debería enviar el código 2FA correctamente")
    void sendTwoFactorCode_ShouldSendEmail() {
        // Given
        String to = "user@test.com";
        String code = "123456";

        // When
        emailService.sendTwoFactorCode(to, code);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendPasswordResetEmail: No debería lanzar excepción si el envío falla (manejo de error)")
    void sendPasswordResetEmail_WhenError_ShouldLogAndNotThrow() {
        // Given
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(MimeMessage.class));

        // When
        // No esperamos excepción porque el servicio la captura y loguea
        emailService.sendPasswordResetEmail("bad@email.com", "token");

        // Then
        verify(mailSender).send(mimeMessage);
    }
}
