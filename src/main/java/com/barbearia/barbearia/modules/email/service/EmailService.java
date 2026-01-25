package com.barbearia.barbearia.modules.email.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender sender;

    @Value("${app.mail.from}")
    private String from;

    public void sendReset(String to, String resetLink) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(new InternetAddress(from, "BarberCuttz"));
            helper.setTo(to);
            helper.setSubject("Recuperação de senha");

            String html = """
                <p>Para redefinir sua senha, clique no link:</p>
                <p><a href="%s">%s</a></p>
                """.formatted(resetLink, resetLink);

            helper.setText(html, true);

            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail de recuperação", e);
        }
    }
}