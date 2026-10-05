package com.hardwarestore.hardwarestore.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.time.Clock;

@org.springframework.scheduling.annotation.EnableScheduling
@Configuration
public class RegistrationMailConfig {
    @Bean Clock registrationClock() { return Clock.systemUTC(); }
    @Bean JavaMailSenderImpl registrationMailSender(
            @Value("${spring.mail.host:smtp-relay.brevo.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String login,
            @Value("${spring.mail.password:}") String key) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(host); sender.setPort(port); sender.setUsername(login); sender.setPassword(key);
        var properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
