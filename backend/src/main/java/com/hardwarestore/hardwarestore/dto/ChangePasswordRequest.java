package com.hardwarestore.hardwarestore.dto;
import jakarta.validation.constraints.*;
public record ChangePasswordRequest(@NotBlank @Size(max=72) String currentPassword,
 @NotBlank @Size(min=8,max=72) String password) {}
