package com.barbearia.barbearia.modules.email.controller;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/text-mail")
@RequiredArgsConstructor
public class MailTextController {

    private final JavaMailSender sender;

    @GetMapping
    public String test(@RequestParam String to) throws Exception {
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
        helper.setFrom("noreply@barbercuttz.me");
        helper.setTo(to);
        helper.setSubject("Teste Brevo SMTP");
        helper.setText("Se chegou, está OK.");
        sender.send(msg);
        return "enviado";
    }
}