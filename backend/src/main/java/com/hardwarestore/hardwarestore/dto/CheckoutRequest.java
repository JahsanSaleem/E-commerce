package com.hardwarestore.hardwarestore.dto;
import jakarta.validation.constraints.*;
public record CheckoutRequest(
 @NotBlank @Pattern(regexp="DELIVERY|COLLECTION") String fulfilment,
 @NotBlank @Size(max=255) String recipientName,
 @NotBlank @Pattern(regexp="[+0-9 ()-]{7,25}") String phone,
 @Size(max=500) String address) {
 public void validate() {
  if (fulfilment.equals("DELIVERY") && (address == null || address.trim().length()<8))
   throw new IllegalArgumentException("A complete delivery address is required.");
 }
}
