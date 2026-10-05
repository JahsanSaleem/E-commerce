package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.dto.*;
import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class RegistrationVerificationService {
    private final PendingRegistrationRepository pending;
    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;
    private final VerificationMailer mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public RegistrationVerificationService(PendingRegistrationRepository pending, UserRepository users,
            BCryptPasswordEncoder encoder, VerificationMailer mail, Clock clock) {
        this.pending=pending; this.users=users; this.encoder=encoder; this.mail=mail; this.clock=clock;
    }
    @Transactional
    public RegistrationChallenge begin(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) throw new IllegalArgumentException("Email already registered. Please sign in.");
        var now=clock.instant();
        var previous=pending.findByEmail(email);
        if (previous.isPresent()) {
            if (previous.get().createdAt.plusSeconds(3600).isAfter(now))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Verification is already pending for this email. Use your verification screen, or try again in one hour.");
            pending.delete(previous.get()); pending.flush();
        }
        var registration=new PendingRegistration();
        registration.id=UUID.randomUUID().toString(); registration.name=request.getName().trim(); registration.email=email;
        registration.passwordHash=encoder.encode(request.getPassword()); registration.createdAt=now;
        issue(registration);
        return challenge(registration);
    }
    @Transactional
    public RegistrationChallenge resend(String id) {
        var registration=find(id); var now=clock.instant();
        if (registration.lastSentAt.plusSeconds(60).isAfter(now))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait 60 seconds before requesting another code.");
        if (registration.sendCount>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Email limit reached. Start a new registration after one hour.");
        issue(registration); return challenge(registration);
    }
    @Transactional(noRollbackFor=InvalidCodeException.class)
    public User verify(String id, String code) {
        var registration=find(id);
        if (!registration.expiresAt.isAfter(clock.instant())) throw new IllegalArgumentException("Code expired. Request a new code.");
        if (registration.attempts>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect codes. Request a new code.");
        if (!encoder.matches(code, registration.codeHash)) {
            registration.attempts++; pending.save(registration);
            throw new InvalidCodeException("Incorrect verification code. Please try again.");
        }
        if (users.findByEmail(registration.email).isPresent()) throw new IllegalArgumentException("Email already registered. Please sign in.");
        var user=new User(); user.setName(registration.name); user.setEmail(registration.email);
        user.setPassword(registration.passwordHash); user.setRole(Role.CUSTOMER);
        user=users.saveAndFlush(user); pending.delete(registration); return user;
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3600000)
    @Transactional
    public void cleanupExpiredRegistrations() {
        pending.deleteByCreatedAtBefore(clock.instant().minusSeconds(3600));
    }
    private PendingRegistration find(String id) {
        var registration=pending.findForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Registration not found. Please start again."));
        if (!registration.createdAt.plusSeconds(3600).isAfter(clock.instant()))
            throw new IllegalArgumentException("Registration expired. Please start again.");
        return registration;
    }
    private void issue(PendingRegistration registration) {
        String code=String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
        registration.codeHash=encoder.encode(code); registration.attempts=0; registration.sendCount++;
        registration.lastSentAt=clock.instant(); registration.expiresAt=registration.lastSentAt.plusSeconds(300);
        pending.saveAndFlush(registration); mail.sendCode(registration.email, code);
    }
    private RegistrationChallenge challenge(PendingRegistration registration) {
        return new RegistrationChallenge(registration.id, registration.email, registration.expiresAt, registration.lastSentAt.plusSeconds(60));
    }
    public static class InvalidCodeException extends IllegalArgumentException {
        public InvalidCodeException(String message) { super(message); }
    }
}
