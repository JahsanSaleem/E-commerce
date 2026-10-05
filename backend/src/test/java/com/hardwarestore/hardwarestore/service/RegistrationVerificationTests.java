package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.dto.*;
import com.hardwarestore.hardwarestore.repository.*;
import com.hardwarestore.hardwarestore.model.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationVerificationTests {
    @Autowired RegistrationVerificationService verification;
    @Autowired PendingRegistrationRepository pending;
    @Autowired UserRepository users;
    @Autowired UserService accounts;
    @MockitoBean VerificationMailer mail;
    RegisterRequest request;
    @BeforeEach void setup() {
        request=new RegisterRequest(); request.setName("OTP Customer");
        request.setEmail(UUID.randomUUID()+"@example.com"); request.setPassword("TestPassword123");
    }
    String code() {
        var capture=ArgumentCaptor.forClass(String.class);
        verify(mail, atLeastOnce()).sendCode(eq(request.getEmail()), capture.capture());
        return capture.getValue();
    }
    @Test void noUserUntilVerificationThenNormalLoginAndCodeCannotBeReused() {
        var challenge=verification.begin(request); var otp=code();
        assertTrue(otp.matches("[0-9]{6}"));
        assertTrue(users.findByEmail(request.getEmail()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> accounts.loginUser(request.getEmail(),request.getPassword()));
        var row=pending.findById(challenge.registrationId()).orElseThrow();
        assertNotEquals(otp,row.codeHash); assertNotEquals(request.getPassword(),row.passwordHash);
        var user=verification.verify(challenge.registrationId(),otp);
        assertEquals(Role.CUSTOMER,user.getRole());
        assertEquals(user.getId(),accounts.loginUser(request.getEmail(),request.getPassword()).getId());
        assertThrows(IllegalArgumentException.class,()->verification.verify(challenge.registrationId(),otp));
    }
    @Test void incorrectAttemptsPersistAndLockEvenCorrectCodeAfterFiveFailures() {
        var challenge=verification.begin(request); var otp=code();
        var wrong=otp.equals("000000") ? "999999" : "000000";
        for(int i=0;i<5;i++) assertThrows(IllegalArgumentException.class,()->verification.verify(challenge.registrationId(),wrong));
        assertEquals(5,pending.findById(challenge.registrationId()).orElseThrow().attempts);
        assertThrows(ResponseStatusException.class,()->verification.verify(challenge.registrationId(),otp));
        assertTrue(users.findByEmail(request.getEmail()).isEmpty());
    }
    @Test void expiredCodeCannotCreateAccount() {
        var challenge=verification.begin(request); var otp=code();
        var row=pending.findById(challenge.registrationId()).orElseThrow(); row.expiresAt=Instant.now().minusSeconds(1); pending.saveAndFlush(row);
        assertThrows(IllegalArgumentException.class,()->verification.verify(challenge.registrationId(),otp));
        assertTrue(users.findByEmail(request.getEmail()).isEmpty());
    }
    @Test void resendHasCooldownAndReplacesPreviousHash() {
        var challenge=verification.begin(request);
        assertThrows(ResponseStatusException.class,()->verification.resend(challenge.registrationId()));
        var row=pending.findById(challenge.registrationId()).orElseThrow(); String oldHash=row.codeHash;
        row.lastSentAt=Instant.now().minusSeconds(61); pending.saveAndFlush(row);
        verification.resend(challenge.registrationId());
        row=pending.findById(challenge.registrationId()).orElseThrow();
        assertNotEquals(oldHash,row.codeHash); assertEquals(2,row.sendCount);
        verification.verify(challenge.registrationId(),code());
    }
    @Test void sendingFailureRollsBackPendingRegistration() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email unavailable")).when(mail).sendCode(anyString(),anyString());
        assertThrows(ResponseStatusException.class,()->verification.begin(request));
        assertFalse(pending.existsByEmail(request.getEmail()));
        assertTrue(users.findByEmail(request.getEmail()).isEmpty());
    }
    @Test void existingAccountCannotBeOverwrittenAndPendingRegistrationCannotBeReplaced() {
        var challenge=verification.begin(request);
        assertThrows(ResponseStatusException.class,()->verification.begin(request));
        verification.verify(challenge.registrationId(),code());
        assertThrows(IllegalArgumentException.class,()->verification.begin(request));
    }
    @Test void expiredRegistrationAndResendLimitAreEnforced() {
        var challenge=verification.begin(request);
        var row=pending.findById(challenge.registrationId()).orElseThrow();
        row.lastSentAt=Instant.now().minusSeconds(61); row.sendCount=5; pending.saveAndFlush(row);
        assertThrows(ResponseStatusException.class,()->verification.resend(challenge.registrationId()));
        row.createdAt=Instant.now().minusSeconds(3601); pending.saveAndFlush(row);
        assertThrows(IllegalArgumentException.class,()->verification.verify(challenge.registrationId(),code()));
        assertNotEquals(challenge.registrationId(),verification.begin(request).registrationId());
    }
    @Test void ipRateLimitRejectsSixthSendRequest() {
        var limiter=new RegistrationRateLimiter(java.time.Clock.systemUTC());
        for(int i=0;i<5;i++) limiter.check("test-address");
        assertThrows(ResponseStatusException.class,()->limiter.check("test-address"));
    }
}
