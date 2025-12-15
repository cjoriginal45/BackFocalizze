package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link EmailService} interface.
 * Handles sending transactional emails using JavaMailSender.
 * <p>
 * Implementación de la interfaz {@link EmailService}.
 * Maneja el envío de correos transaccionales usando JavaMailSender.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    /**
     * Sends a password reset link to the user.
     * Executed asynchronously to prevent blocking the HTTP response.
     * <p>
     * Envía un enlace de restablecimiento de contraseña al usuario.
     * Ejecutado asincrónicamente para evitar bloquear la respuesta HTTP.
     *
     * @param to    The recipient's email address. / La dirección de correo del destinatario.
     * @param token The reset token. / El token de restablecimiento.
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // URL que el usuario clickeará. Cambia el puerto si es necesario.
            String resetUrl = "http://localhost:4200/reset-password?token=" + token;

            // Un HTML simple para el correo. Puedes hacerlo tan complejo como quieras.
            String htmlMsg = "<h3>Restauración de Contraseña</h3>"
                    + "<p>Has solicitado restaurar tu contraseña. Por favor, haz clic en el botón de abajo para continuar. Este enlace expirará en 15 minutos.</p>"
                    + "<a href=\"" + resetUrl + "\" style=\"background-color: #01344a; color: white; padding: 15px 25px; text-align: center; text-decoration: none; display: inline-block; border-radius: 5px;\">Restaurar Contraseña</a>"
                    + "<p>Si no solicitaste esto, por favor ignora este correo.</p>";

            helper.setText(htmlMsg, true); // true indica que el texto es HTML
            helper.setTo(to);
            helper.setSubject("Focalizze - Solicitud de Restauración de Contraseña");
            helper.setFrom("noreply@focalizze.com"); // Puedes poner lo que quieras aquí

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Manejar la excepción, por ejemplo, logueándola
            System.err.println("Error al enviar el email: " + e.getMessage());
        }
    }

    /**
     * Sends a 2FA (Two-Factor Authentication) code to the user.
     * <p>
     * Envía un código de 2FA (Autenticación de Dos Factores) al usuario.
     *
     * @param to   The recipient's email address. / La dirección de correo del destinatario.
     * @param code The numeric OTP code. / El código numérico OTP.
     */
    @Async
    @Override
    public void sendTwoFactorCode(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Diseño HTML para el código OTP
            String htmlMsg = "<div style=\"font-family: Arial, sans-serif; color: #333;\">"
                    + "<h2 style=\"color: #01344a;\">Verificación de Seguridad</h2>"
                    + "<p>Alguien está intentando iniciar sesión en tu cuenta de Focalizze. Usa el siguiente código para completar el proceso:</p>"
                    + "<div style=\"background-color: #f4f4f4; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #01344a;\">" + code + "</span>"
                    + "</div>"
                    + "<p>Este código expirará en <strong>5 minutos</strong>.</p>"
                    + "<p style=\"font-size: 12px; color: #777;\">Si no fuiste tú, te recomendamos cambiar tu contraseña inmediatamente.</p>"
                    + "</div>";

            helper.setText(htmlMsg, true); // true para HTML
            helper.setTo(to);
            helper.setSubject("Focalizze - Tu código de verificación: " + code);
            helper.setFrom("noreply@focalizze.com");

            mailSender.send(mimeMessage);
            System.out.println("Email 2FA enviado a " + to);

        } catch (MessagingException e) {
            System.err.println("Error al enviar el email de 2FA: " + e.getMessage());
        }
    }
}
