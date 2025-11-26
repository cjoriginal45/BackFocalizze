package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // URL que el usuario clickeará. Cambia el puerto si es necesario.
            String resetUrl = "http://localhost:4200/new-password?token=" + token;

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
}
