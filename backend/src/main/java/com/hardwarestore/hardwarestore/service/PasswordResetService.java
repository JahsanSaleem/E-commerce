package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.dto.PasswordResetChallenge;
import com.hardwarestore.hardwarestore.model.PasswordReset;
import com.hardwarestore.hardwarestore.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;

@Service
public class PasswordResetService {
    private static final String MESSAGE = "If an email/password account exists for this address, we sent a reset code. Check your inbox and spam folder.";
    private static final String INVALID = "Invalid or expired reset code. Request a new code.";
    private final PasswordResetRepository resets;
    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;
    private final PasswordResetMailDispatcher mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetRepository resets, UserRepository users,
            BCryptPasswordEncoder encoder, PasswordResetMailDispatcher mail, Clock clock) {
        this.resets = resets;
        this.users = users;
        this.encoder = encoder;
        this.mail = mail;
        this.clock = clock;
    }

    @Transactional
    public PasswordResetChallenge begin(String address) {
        String email = address.trim().toLowerCase(Locale.ROOT);
        var now = clock.instant();
        var reset = resets.findEmailForUpdate(email).orElseGet(PasswordReset::new);
        if (reset.createdAt != null && reset.createdAt.plusSeconds(3600).isAfter(now)) {
            if (reset.lastSentAt.plusSeconds(60).isAfter(now))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait 60 seconds before requesting another code.");
            if (reset.sendCount >= 5)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Email limit reached. Please try again after one hour.");
        } else {
            reset.createdAt = now;
            reset.sendCount = 0;
        }
        reset.email = email;
        reset.resetId = UUID.randomUUID().toString();
        reset.attempts = 0;
        reset.sendCount++;
        reset.lastSentAt = now;
        reset.expiresAt = now.plusSeconds(300);
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
        reset.codeHash = encoder.encode(code);
        var account = users.findByEmail(email).filter(user -> user.getGoogleSubject() == null).orElse(null);
        reset.userId = account == null ? null : account.getId();
        reset.passwordHash = account == null ? null : account.getPassword();
        // Delivery runs after commit so network timing cannot disclose account existence.
        if (account != null) mail.sendCode(email, code);
        resets.saveAndFlush(reset);
        return new PasswordResetChallenge(reset.resetId, reset.expiresAt, now.plusSeconds(60), MESSAGE);
    }

    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public void complete(String id, String code, String password) {
        var reset = resets.findResetForUpdate(id).orElseThrow(() -> new IllegalArgumentException(INVALID));
        if (!reset.expiresAt.isAfter(clock.instant())) throw new IllegalArgumentException(INVALID);
        if (reset.attempts >= 5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect codes. Request a new code.");
        boolean matches = encoder.matches(code, reset.codeHash);
        if (!matches || reset.userId == null) {
            reset.attempts++;
            resets.save(reset);
            throw new InvalidResetCodeException(INVALID);
        }
        var user = users.findByIdForUpdate(reset.userId).orElseThrow(() -> new IllegalArgumentException(INVALID));
        if (user.getGoogleSubject() != null || !Objects.equals(user.getPassword(), reset.passwordHash))
            throw new IllegalArgumentException(INVALID);
        // BCrypt supports at most 72 UTF-8 bytes, including for non-ASCII passwords.
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        user.setPassword(encoder.encode(password));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        users.saveAndFlush(user);
        mail.sendConfirmation(user.getEmail());
        // Preserve the email's hourly send counter while consuming its code.
        reset.userId = null;
        reset.passwordHash = null;
        reset.expiresAt = clock.instant();
        resets.save(reset);
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanup() {
        resets.deleteByCreatedAtBefore(clock.instant().minusSeconds(3600));
    }

    public static class InvalidResetCodeException extends IllegalArgumentException {
        public InvalidResetCodeException(String message) { super(message); }
    }
}
