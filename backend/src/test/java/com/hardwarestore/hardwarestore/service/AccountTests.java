package com.hardwarestore.hardwarestore.service;
import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AccountTests {
 @Autowired MockMvc mvc; @Autowired UserService accounts; @Autowired SavedAddressRepository addresses;
 @MockitoBean VerificationMailer mail;
 User user(){var u=new User();u.setName("Customer");u.setEmail(UUID.randomUUID()+"@example.com");u.setPassword("OldPassword123");return accounts.registerUser(u);}
 MockHttpSession session(User u){var s=new MockHttpSession();s.setAttribute("userId",u.getId());s.setAttribute("role",u.getRole());return s;}
 @Test void addressOwnershipAndProfileValidation()throws Exception {
  var a=user();var b=user();var row=new SavedAddress();row.userId=a.getId();row.label="Home";row.recipientName="Customer";row.phone="0771234567";row.address="123 Test Street";row=addresses.save(row);
  mvc.perform(get("/api/account/addresses").session(session(b))).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(delete("/api/account/addresses/"+row.id).session(session(b))).andExpect(status().isNotFound());assertTrue(addresses.existsById(row.id));
  mvc.perform(put("/api/account").session(session(a)).contentType("application/json").content("{\"name\":\"Updated\"}")).andExpect(status().isOk());assertEquals("Updated",accounts.getUserById(a.getId()).getName());
  mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
 }
 @Test void changePasswordRequiresCurrentPasswordAndKeepsCurrentSession()throws Exception {
  var u=user();var s=session(u);
  mvc.perform(post("/api/account/password").session(s).contentType("application/json").content("{\"currentPassword\":\"wrong\",\"password\":\"NewPassword123\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/account/password").session(s).contentType("application/json").content("{\"currentPassword\":\"OldPassword123\",\"password\":\"NewPassword123\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isOk());assertNotNull(accounts.loginUser(u.getEmail(),"NewPassword123"));
  mvc.perform(get("/api/auth/me").session(session(u))).andExpect(status().isUnauthorized());
 }
 @Test void dashboardIsAdminOnlyAndReturnsAggregates()throws Exception {
  var u=user();mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/dashboard").session(session(u))).andExpect(status().isForbidden());
  u.setRole(Role.ADMIN);accounts.updateRole(u.getId(),Role.ADMIN);
  mvc.perform(get("/api/admin/dashboard").session(session(u))).andExpect(status().isOk()).andExpect(jsonPath("$.deliveredOrderValue").isNumber()).andExpect(jsonPath("$.lowStock").isArray());
 }
}
