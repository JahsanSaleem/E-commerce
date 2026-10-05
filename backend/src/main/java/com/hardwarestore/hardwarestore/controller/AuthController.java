package com.hardwarestore.hardwarestore.controller;

import com.hardwarestore.hardwarestore.dto.LoginRequest;
import com.hardwarestore.hardwarestore.dto.VerificationRequest;
import com.hardwarestore.hardwarestore.dto.ResendVerificationRequest;
import com.hardwarestore.hardwarestore.service.RegistrationVerificationService;
import com.hardwarestore.hardwarestore.service.RegistrationRateLimiter;
import com.hardwarestore.hardwarestore.dto.RegisterRequest;
import com.hardwarestore.hardwarestore.dto.UserResponse;
import com.hardwarestore.hardwarestore.model.User;
import com.hardwarestore.hardwarestore.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    private final RegistrationVerificationService verification;
    private final RegistrationRateLimiter rateLimiter;
    public AuthController(UserService userService, RegistrationVerificationService verification, RegistrationRateLimiter rateLimiter) {
        this.userService=userService; this.verification=verification; this.rateLimiter=rateLimiter;
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(verification.begin(request));
    }
    @PostMapping("/register/resend")
    public ResponseEntity<?> resend(@Valid @RequestBody ResendVerificationRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(httpRequest.getRemoteAddr());
        return ResponseEntity.ok(verification.resend(request.registrationId()));
    }
    @PostMapping("/register/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerificationRequest request, HttpSession session, HttpServletRequest httpRequest) {
        User user=verification.verify(request.registrationId(), request.code());
        httpRequest.changeSessionId(); storeUserSession(session, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session, HttpServletRequest httpRequest
    ) {
        try {
            User user = userService.loginUser(
                    request.getEmail(),
                    request.getPassword()
            );

            httpRequest.changeSessionId();
            storeUserSession(session, user);
            return ResponseEntity.ok(toUserResponse(user));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", exception.getMessage())
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Please login first")
            );
        }

        return ResponseEntity.ok(
                toUserResponse(userService.getUserById(userId))
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private void storeUserSession(HttpSession session, User user) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole());
        session.setAttribute("credentialVersion", user.getCredentialVersion());
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
