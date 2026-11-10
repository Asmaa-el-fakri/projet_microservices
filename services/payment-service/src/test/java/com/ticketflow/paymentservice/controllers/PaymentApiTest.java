package com.ticketflow.paymentservice.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
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
class PaymentApiTest {

    private static final String BODY = "{\"bookingId\":%d,\"amount\":%s,\"currency\":\"MAD\"}";

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor client(String subject) {
        return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted(100, "150.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizerCannotPay() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted(100, "150.00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAmountIsRejected() throws Exception {
        mockMvc.perform(post("/api/payments").with(client("client-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted(100, "-5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientPaysAndOnlyOwnerSeesIt() throws Exception {
        String location = mockMvc.perform(post("/api/payments").with(client("client-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted(101, "150.00")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getHeader("Location");
        String path = URI.create(location).getPath();

        mockMvc.perform(post(path + "/simulate").with(client("client-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        // un autre client ne voit pas ce paiement
        mockMvc.perform(get(path).with(client("client-2")))
                .andExpect(status().isNotFound());

        // un paiement reussi ne peut plus changer
        mockMvc.perform(post(path + "/simulate").with(client("client-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"FAILURE\"}"))
                .andExpect(status().isConflict());
    }
}