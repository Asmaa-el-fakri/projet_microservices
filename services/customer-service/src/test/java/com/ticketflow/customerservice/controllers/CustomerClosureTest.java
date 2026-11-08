package com.ticketflow.customerservice.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CustomerClosureTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminSeesWhoRequestedClosure() throws Exception {
        String subject = "sub-" + UUID.randomUUID();
        String email = UUID.randomUUID() + "@example.com";

        RequestPostProcessor client = jwt()
                .jwt(j -> j.subject(subject)
                        .claim("given_name", "Test")
                        .claim("family_name", "User")
                        .claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
        RequestPostProcessor admin = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // profil cree a la premiere connexion : pas de demande de fermeture
        mockMvc.perform(get("/api/customers/me").with(client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closureRequested").value(false));

        // le client demande la fermeture
        mockMvc.perform(post("/api/customers/me/close").with(client))
                .andExpect(status().isAccepted());

        // l'administrateur voit la demande dans la liste
        mockMvc.perform(get("/api/customers").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '" + email + "')].closureRequested").value(true));

        // le client voit aussi l'etat de sa demande
        mockMvc.perform(get("/api/customers/me").with(client))
                .andExpect(jsonPath("$.closureRequested").value(true));
    }
}