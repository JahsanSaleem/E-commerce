package com.hardwarestore.hardwarestore.service;

import com.hardwarestore.hardwarestore.model.*;
import com.hardwarestore.hardwarestore.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowAcceptanceTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @org.springframework.test.context.bean.override.mockito.MockitoBean VerificationMailer mail;
    MockMvc mvc;
    @BeforeEach void setup() { mvc=MockMvcBuilders.webAppContextSetup(context).build(); }
    MockHttpSession session(Role role) {
        var user=new User();user.setName("Test user");user.setEmail(UUID.randomUUID()+"@example.com");user.setPassword("test hash");user.setRole(role);user=users.save(user);
        var session=new MockHttpSession();session.setAttribute("userId",user.getId());session.setAttribute("role",role);return session;
    }
    @Test void healthAndCorsAreAvailable() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(options("/api/products").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","PUT"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials","true"));
    }
    @Test void staffOperatesOrdersButCannotManageAccountsOrCatalogue() throws Exception {
        var session=session(Role.STAFF);
        mvc.perform(get("/api/orders/status/PENDING").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Denied\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void customerCannotReadAnotherCustomersCartOrOrders() throws Exception {
        var one=session(Role.CUSTOMER);var two=session(Role.CUSTOMER);long id=(Long)two.getAttribute("userId");
        mvc.perform(get("/api/cart/"+id).session(one)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/customer/"+id).session(one)).andExpect(status().isUnauthorized());
    }
    @Test void registrationCannotAssignAdminAndResponsesExcludePasswords() throws Exception {
        var session=new MockHttpSession();
        var result=mvc.perform(post("/api/auth/register").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Customer\",\"email\":\""+UUID.randomUUID()+"@example.com\",\"password\":\"Testpass123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute("userId"));
        var code=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(mail).sendCode(org.mockito.ArgumentMatchers.anyString(),code.capture());
        String id=new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("registrationId").asText();
        mvc.perform(post("/api/auth/register/verify").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationId\":\""+id+"\",\"code\":\""+code.getValue()+"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void demotionRevokesExistingSessionPermissions() throws Exception {
        var session=session(Role.ADMIN);long id=(Long)session.getAttribute("userId");
        mvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Admin category\"}"))
                .andExpect(status().isOk());
        var user=users.findById(id).orElseThrow();user.setRole(Role.CUSTOMER);users.save(user);
        mvc.perform(delete("/api/categories/999999").session(session)).andExpect(status().isForbidden());
    }
    @Test void invalidCatalogueFieldsReturnValidationErrors() throws Exception {
        mvc.perform(post("/api/products").session(session(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"price\":0,\"quantity\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists()).andExpect(jsonPath("$.quantity").exists());
    }
}
