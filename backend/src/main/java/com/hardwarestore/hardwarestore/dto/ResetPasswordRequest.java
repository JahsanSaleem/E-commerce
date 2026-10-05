package com.hardwarestore.hardwarestore.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 36) String resetId,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code,
        @NotBlank @Size(min = 8, max = 72) String password) {}
