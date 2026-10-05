package com.hardwarestore.hardwarestore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class VerificationMailer {
    private final JavaMailSenderImpl sender;
    private final String from;
    public VerificationMailer(JavaMailSenderImpl sender, @Value("${store.mail.from:}") String from) {
        this.sender = sender; this.from = from;
    }
    public void sendCode(String email, String code) {
        send(email, code, "registration", "verify your email");
    }
    public void sendResetCode(String email, String code) {
        send(email, code, "password reset", "reset your password");
    }
    public void sendOrderMessage(String email,String subject,String body) { deliver(email,subject,body); }
    public void sendPasswordChanged(String email) {
        deliver(email, "Mustafa Hardware — password changed",
                "Your Mustafa Hardware password was changed.\nIf you did not make this change, reset your password immediately and contact the store.\nYour existing store sessions have been signed out.");
    }
    private void send(String email, String code, String purpose, String subject) {
        deliver(email, "Mustafa Hardware — " + subject,
                "Your Mustafa Hardware " + purpose + " code is: " + code
                + "\n\nThis code expires in 5 minutes. Do not share it with anyone."
                + "\nIf you did not request this, ignore this email. Your password has not been changed.");
    }
    private void deliver(String email, String subject, String body) {
        if (from.isBlank() || sender.getUsername() == null || sender.getUsername().isBlank()
                || sender.getPassword() == null || sender.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured yet. Please try again later.");
        }
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try { sender.send(message); }
        catch (MailException exception) {
            // Never return provider credentials, SMTP errors or the OTP to clients/logs.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification email. Please try again later.");
        }
    }
}
