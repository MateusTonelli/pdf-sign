package com.pdfsign.service;

import com.pdfsign.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public EmailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    public void sendSigningInvitation(String toEmail, String signerName, String documentTitle, String signUrl) {
        String subject = "Please sign: " + documentTitle;
        String body = """
                Hello %s,

                You have been invited to sign the document "%s".

                Open this link to review and sign:
                %s

                This link expires in %d days.

                — PDF Sign
                """.formatted(signerName, documentTitle, signUrl, appProperties.getMagicTokenTtlDays());

        if (appProperties.getMail().isLogOnly()) {
            log.info("[EMAIL log-only] To: {} | Subject: {} | Body:\n{}", toEmail, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMail().getFrom());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {} — logging content instead: {}", toEmail, e.getMessage());
            log.info("[EMAIL fallback] To: {} | Subject: {} | Body:\n{}", toEmail, subject, body);
        }
    }
}
