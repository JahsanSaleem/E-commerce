package com.hardwarestore.hardwarestore.controller;

import com.hardwarestore.hardwarestore.dto.*;
import com.hardwarestore.hardwarestore.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {
    private final PasswordResetService resets;
    private final RegistrationRateLimiter limiter;

    public PasswordResetController(PasswordResetService resets, RegistrationRateLimiter limiter) {
        this.resets = resets;
        this.limiter = limiter;
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr());
        return ResponseEntity.accepted().body(resets.begin(request.email()));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        resets.complete(request.resetId(), request.code(), request.password());
        var session = http.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Password updated. Sign in with your new password."));
    }
}
