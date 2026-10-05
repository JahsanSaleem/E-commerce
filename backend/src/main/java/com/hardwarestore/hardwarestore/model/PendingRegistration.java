package com.hardwarestore.hardwarestore.model;

import jakarta.persistence.*;
import java.time.Instant;

// Separate from User: unverified registrations cannot log in or shop.
@Entity
public class PendingRegistration {
    @Id public String id;
    @Column(nullable = false, unique = true) public String email;
    @Column(nullable = false) public String name;
    @Column(nullable = false) public String passwordHash;
    @Column(nullable = false) public String codeHash;
    @Column(nullable = false) public Instant expiresAt;
    @Column(nullable = false) public Instant createdAt;
    @Column(nullable = false) public Instant lastSentAt;
    public int attempts;
    public int sendCount;
}
