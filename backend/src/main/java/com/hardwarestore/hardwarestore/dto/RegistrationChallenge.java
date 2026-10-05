package com.hardwarestore.hardwarestore.dto;
import java.time.Instant;
public record RegistrationChallenge(String registrationId, String email, Instant expiresAt, Instant resendAt) {}
