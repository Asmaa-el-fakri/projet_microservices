package com.ticketflow.customerservice.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class CustomerControllerTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Filtre de sécurité appliqué explicitement : fonctionne quel que soit le nom des starters de test
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor client(String subject, String email) {
        return jwt()
                .jwt(j -> j.subject(subject)
                        .claim("given_name", "Test")
                        .claim("family_name", "User")
                        .claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static String body(String email) {
        return """
                {"firstName":"John","lastName":"Doe","email":"%s","phone":"+212600000000"}
                """.formatted(email);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    @Test
    void createThenGetThenDelete() throws Exception {
        String location = mockMvc.perform(post("/api/customers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(URI.create(location)).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        mockMvc.perform(delete(URI.create(location)).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URI.create(location)).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/customers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/customers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotListOrCreateCustomers() throws Exception {
        mockMvc.perform(get("/api/customers").with(client("sub-1", uniqueEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/customers").with(client("sub-1", uniqueEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotReadAnotherProfileById() throws Exception {
        mockMvc.perform(get("/api/customers/1").with(client("sub-2", uniqueEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void meCreatesProfileFromTokenThenUpdatesAllowedFieldsOnly() throws Exception {
        String subject = "sub-" + UUID.randomUUID();
        String email = uniqueEmail();

        mockMvc.perform(get("/api/customers/me").with(client(subject, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"));

        mockMvc.perform(put("/api/customers/me").with(client(subject, email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asmaa","lastName":"Test","phone":"+212611111111"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Asmaa"));
    }
}