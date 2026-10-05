package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetTests {
    @Autowired PasswordResetService resets;
    @Autowired PasswordResetRepository pending;
    @Autowired UserRepository users;
    @Autowired UserService accounts;
    @Autowired MockMvc mvc;
    @MockitoBean VerificationMailer mail;
    User user;
    @BeforeEach void setup() {
        user = new User(); user.setName("Reset customer");
        user.setEmail(UUID.randomUUID()+"@example.com"); user.setPassword("OldPassword123");
        user = accounts.registerUser(user);
    }
    String code() {
        var capture = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(2000).atLeastOnce()).sendResetCode(eq(user.getEmail()), capture.capture());
        return capture.getValue();
    }
    @Test void changesOnlyTargetPasswordAndConsumesCode() {
        var challenge = resets.begin(user.getEmail().toUpperCase()); var otp = code();
        assertTrue(otp.matches("[0-9]{6}"));
        assertNotEquals(otp,pending.findById(user.getEmail()).orElseThrow().codeHash);
        assertDoesNotThrow(() -> accounts.loginUser(user.getEmail(), "OldPassword123"));
        resets.complete(challenge.resetId(),otp,"NewPassword123");
        assertThrows(IllegalArgumentException.class,()->accounts.loginUser(user.getEmail(),"OldPassword123"));
        var updated = accounts.loginUser(user.getEmail(),"NewPassword123");
        assertEquals(Role.CUSTOMER,updated.getRole()); assertEquals(1,updated.getCredentialVersion());
        verify(mail,timeout(2000)).sendPasswordChanged(user.getEmail());
        assertNotEquals("NewPassword123",updated.getPassword());
        assertThrows(IllegalArgumentException.class,()->resets.complete(challenge.resetId(),otp,"ThirdPassword123"));
    }
    @Test void fiveIncorrectAttemptsPersistAndLockCode() {
        var challenge=resets.begin(user.getEmail()); var otp=code();
        String wrong=otp.equals("000000")?"999999":"000000";
        for(int i=0;i<5;i++) assertThrows(IllegalArgumentException.class,()->resets.complete(challenge.resetId(),wrong,"NewPassword123"));
        assertEquals(5,pending.findById(user.getEmail()).orElseThrow().attempts);
        assertThrows(ResponseStatusException.class,()->resets.complete(challenge.resetId(),otp,"NewPassword123"));
        assertDoesNotThrow(()->accounts.loginUser(user.getEmail(),"OldPassword123"));
    }
    @Test void expiryAndInvalidIdCannotReset() {
        var challenge=resets.begin(user.getEmail()); var otp=code();
        var row=pending.findById(user.getEmail()).orElseThrow(); row.expiresAt=Instant.now().minusSeconds(1); pending.saveAndFlush(row);
        assertThrows(IllegalArgumentException.class,()->resets.complete(challenge.resetId(),otp,"NewPassword123"));
        assertThrows(IllegalArgumentException.class,()->resets.complete(UUID.randomUUID().toString(),otp,"NewPassword123"));
    }
    @Test void resendReplacesChallengeAndEnforcesCooldownAndHourlyLimit() {
        var first=resets.begin(user.getEmail()); var otp=code();
        assertThrows(ResponseStatusException.class,()->resets.begin(user.getEmail()));
        var row=pending.findById(user.getEmail()).orElseThrow(); row.lastSentAt=Instant.now().minusSeconds(61); pending.saveAndFlush(row);
        var second=resets.begin(user.getEmail());
        assertNotEquals(first.resetId(),second.resetId());
        assertThrows(IllegalArgumentException.class,()->resets.complete(first.resetId(),otp,"NewPassword123"));
        row=pending.findById(user.getEmail()).orElseThrow(); row.lastSentAt=Instant.now().minusSeconds(61); row.sendCount=5; pending.saveAndFlush(row);
        assertThrows(ResponseStatusException.class,()->resets.begin(user.getEmail()));
        row.createdAt=Instant.now().minusSeconds(3601); pending.saveAndFlush(row);
        assertDoesNotThrow(()->resets.begin(user.getEmail()));
    }
    @Test void unknownAndGoogleAccountsReceiveSameResponseButNoEmailOrPassword() {
        var actual=resets.begin(user.getEmail()); code();
        String unknown=UUID.randomUUID()+"@example.com";
        var missing=resets.begin(unknown); assertEquals(actual.message(),missing.message());
        verify(mail,never()).sendResetCode(eq(unknown),anyString());
        var row=pending.findById(unknown).orElseThrow(); assertNull(row.userId);
        user.setGoogleSubject("google-"+UUID.randomUUID()); users.saveAndFlush(user);
        row=pending.findById(user.getEmail()).orElseThrow(); row.lastSentAt=Instant.now().minusSeconds(61); pending.saveAndFlush(row);
        clearInvocations(mail);
        var google=resets.begin(user.getEmail()); assertEquals(actual.message(),google.message());
        verify(mail,never()).sendResetCode(anyString(),anyString());
        assertNull(pending.findById(user.getEmail()).orElseThrow().userId);
    }
    @Test void mailFailureDoesNotChangePasswordOrRevealAccount() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"provider unavailable")).when(mail).sendResetCode(anyString(),anyString());
        var challenge=resets.begin(user.getEmail());
        assertNotNull(challenge.resetId()); code();
        assertDoesNotThrow(()->accounts.loginUser(user.getEmail(),"OldPassword123"));
    }
    @Test void stalePasswordSnapshotCannotResetNewerCredentials() {
        var challenge=resets.begin(user.getEmail()); var otp=code();
        user.setPassword("different-hash"); users.saveAndFlush(user);
        assertThrows(IllegalArgumentException.class,()->resets.complete(challenge.resetId(),otp,"NewPassword123"));
    }
    @Test void resetInvalidatesExistingSessionsAndAllowsFreshLogin() throws Exception {
        var session=(MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\""+user.getEmail()+"\",\"password\":\"OldPassword123\"}"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession();
        var challenge=resets.begin(user.getEmail()); var otp=code();
        mvc.perform(post("/api/auth/password/reset").contentType("application/json")
            .content("{\"resetId\":\""+challenge.resetId()+"\",\"code\":\""+otp+"\",\"password\":\"NewPassword123\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\""+user.getEmail()+"\",\"password\":\"NewPassword123\"}"))
            .andExpect(status().isOk());
    }
    @Test void apiValidatesPasswordAndCodeBeforeChanges() throws Exception {
        var challenge=resets.begin(user.getEmail());
        mvc.perform(post("/api/auth/password/reset").contentType("application/json")
            .content("{\"resetId\":\""+challenge.resetId()+"\",\"code\":\"123\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password/forgot").contentType("application/json")
            .content("{\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
        assertDoesNotThrow(()->accounts.loginUser(user.getEmail(),"OldPassword123"));
    }
}
