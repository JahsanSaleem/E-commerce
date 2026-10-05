package com.hardwarestore.hardwarestore.dto;

import java.time.Instant;

public record PasswordResetChallenge(String resetId, Instant expiresAt, Instant resendAt, String message) {}
