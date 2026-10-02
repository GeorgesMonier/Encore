package com.encore.encoreapi.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        String verificationLink = "http://localhost:8080/api/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("hello@dev.george.com"); // tu dominio verificado
        message.setTo(to);
        message.setSubject("Verifica tu cuenta en Encore");
        message.setText("¡Bienvenido a Encore! Haz clic en el siguiente enlace para verificar tu cuenta:\n\n"
                + verificationLink
                + "\n\nEste enlace expira en 24 horas.");

        mailSender.send(message);
    }
}