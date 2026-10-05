package com.hardwarestore.hardwarestore.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class PasswordReset {
    @Id public String email;
    @Column(nullable = false, unique = true) public String resetId;
    public Long userId;
    public String passwordHash;
    @Column(nullable = false) public String codeHash;
    @Column(nullable = false) public Instant expiresAt;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant lastSentAt;
    public int attempts;
    public int sendCount;
}
