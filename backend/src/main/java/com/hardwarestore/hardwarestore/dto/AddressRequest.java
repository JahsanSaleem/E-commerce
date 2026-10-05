package com.hardwarestore.hardwarestore.dto;
import jakarta.validation.constraints.*;
public record AddressRequest(@NotBlank @Size(max=80) String label,
 @NotBlank @Size(max=255) String recipientName,
 @NotBlank @Pattern(regexp="[+0-9 ()-]{7,25}") String phone,
 @NotBlank @Size(min=8,max=500) String address) {}
