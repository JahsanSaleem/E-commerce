package com.hardwarestore.hardwarestore.model;
import jakarta.persistence.*;
@Entity public class SavedAddress {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long userId;
 @Column(nullable=false,length=80) public String label;
 @Column(nullable=false) public String recipientName;
 @Column(nullable=false,length=25) public String phone;
 @Column(nullable=false,length=500) public String address;
}
