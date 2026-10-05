package com.hardwarestore.hardwarestore.dto;
import jakarta.validation.constraints.*;
public record VerificationRequest(@NotBlank @Size(max=36) String registrationId,
                                  @NotBlank @Pattern(regexp="[0-9]{6}", message="Enter the six-digit code") String code) {}
