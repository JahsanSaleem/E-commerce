package com.hardwarestore.hardwarestore.dto;
import jakarta.validation.constraints.*;
public record ResendVerificationRequest(@NotBlank @Size(max=36) String registrationId) {}
