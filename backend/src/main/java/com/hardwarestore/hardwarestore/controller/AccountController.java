package com.hardwarestore.hardwarestore.controller;
import com.hardwarestore.hardwarestore.dto.*;
import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.service.PasswordResetMailDispatcher;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/account")
public class AccountController {
 private final UserRepository users; private final SavedAddressRepository addresses;
 private final BCryptPasswordEncoder encoder; private final PasswordResetMailDispatcher mail;
 public AccountController(UserRepository users,SavedAddressRepository addresses,BCryptPasswordEncoder encoder,PasswordResetMailDispatcher mail) {
  this.users=users;this.addresses=addresses;this.encoder=encoder;this.mail=mail;
 }
 private Long id(HttpSession session) {
  if(!(session.getAttribute("userId") instanceof Long id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in first.");return id;
 }
 private User account(HttpSession session) {return users.findById(id(session)).orElseThrow();}
 @GetMapping public Map<String,Object> profile(HttpSession session) {
  var user=account(session);return Map.of("name",user.getName(),"email",user.getEmail(),"googleAccount",user.getGoogleSubject()!=null);
 }
 public record ProfileRequest(@NotBlank @Size(max=255) String name) {}
 @PutMapping @Transactional public Map<String,String> update(@Valid @RequestBody ProfileRequest request,HttpSession session) {
  var user=users.findByIdForUpdate(id(session)).orElseThrow();user.setName(request.name().trim());users.save(user);return Map.of("message","Profile updated.");
 }
 @PostMapping("/password") @Transactional public Map<String,String> password(@Valid @RequestBody ChangePasswordRequest request,HttpSession session) {
  var user=users.findByIdForUpdate(id(session)).orElseThrow();
  if(user.getGoogleSubject()!=null) throw new IllegalArgumentException("Use your Google account to manage its password.");
  if(!encoder.matches(request.currentPassword(),user.getPassword()))throw new IllegalArgumentException("Current password is incorrect.");
  if(request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
  user.setPassword(encoder.encode(request.password()));user.setCredentialVersion(user.getCredentialVersion()+1);users.saveAndFlush(user);
  session.setAttribute("credentialVersion",user.getCredentialVersion());mail.sendConfirmation(user.getEmail());return Map.of("message","Password changed. Other sessions will be signed out.");
 }
 @GetMapping("/addresses") public List<SavedAddress> list(HttpSession session) {return addresses.findByUserIdOrderByIdAsc(id(session));}
 @PostMapping("/addresses") @Transactional public SavedAddress add(@Valid @RequestBody AddressRequest request,HttpSession session) {
  Long owner=id(session);users.findByIdForUpdate(owner).orElseThrow();
  if(addresses.countByUserId(owner)>=10)throw new IllegalArgumentException("You can save up to ten addresses.");
  var address=new SavedAddress();address.userId=owner;return save(address,request);
 }
 @PutMapping("/addresses/{addressId}") @Transactional public SavedAddress edit(@PathVariable Long addressId,@Valid @RequestBody AddressRequest request,HttpSession session) {return save(owned(addressId,session),request);}
 @DeleteMapping("/addresses/{addressId}") @Transactional public void delete(@PathVariable Long addressId,HttpSession session) {addresses.delete(owned(addressId,session));}
 private SavedAddress owned(Long addressId,HttpSession session) {
  var address=addresses.findById(addressId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Address not found."));
  if(!address.userId.equals(id(session)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Address not found.");return address;
 }
 private SavedAddress save(SavedAddress address,AddressRequest request) {
  address.label=request.label().trim();address.recipientName=request.recipientName().trim();address.phone=request.phone().trim();address.address=request.address().trim();return addresses.save(address);
 }
}
