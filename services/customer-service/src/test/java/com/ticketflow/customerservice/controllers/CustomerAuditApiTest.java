package com.ticketflow.customerservice.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CustomerAuditApiTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin-sub").claim("preferred_username", "admin1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor client(String subject, String email) {
        return jwt()
                .jwt(j -> j.subject(subject)
                        .claim("given_name", "Test")
                        .claim("family_name", "User")
                        .claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    @Test
    void adminActionsAreAudited() throws Exception {
        String location = mockMvc.perform(post("/api/customers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Audit","lastName":"Test","email":"%s","phone":"+212600000000"}
                                """.formatted(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(delete(URI.create(location)).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/audit?size=50").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.resourceId == '" + id + "' && @.action == 'CUSTOMER_CREATED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.resourceId == '" + id + "' && @.action == 'CUSTOMER_DELETED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.resourceId == '" + id + "' && @.actor == 'admin1')]").isNotEmpty());
    }

    @Test
    void clientCannotReadTheAuditLog() throws Exception {
        mockMvc.perform(get("/api/customers/audit").with(client("sub-x", uniqueEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customers/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void closureRequestIsVisibleToAdminAndAudited() throws Exception {
        String requesterEmail = uniqueEmail();
        String otherEmail = uniqueEmail();

        // un client qui demande la fermeture, et un autre qui ne demande rien
        mockMvc.perform(get("/api/customers/me").with(client("sub-" + UUID.randomUUID(), otherEmail)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customers/me/close")
                        .with(client("sub-" + UUID.randomUUID(), requesterEmail)))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/customers?closureRequested=true").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '" + requesterEmail + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.email == '" + otherEmail + "')]").isEmpty());

        mockMvc.perform(get("/api/customers/audit?size=50").with(admin()))
                .andExpect(jsonPath("$.content[?(@.action == 'CLOSURE_REQUESTED')]").isNotEmpty());
    }
}