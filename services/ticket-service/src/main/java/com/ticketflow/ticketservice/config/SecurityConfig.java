
package com.ticketflow.ticketservice.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Convert Keycloak realm roles into Spring Security authorities
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess == null ||
                    !(realmAccess.get("roles") instanceof Collection<?> roles)) {

                return List.<GrantedAuthority>of();
            }

            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> (GrantedAuthority)
                            new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });

        return converter;
    }

    // Configure HTTP security
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // Disable CSRF because this API is stateless
                .csrf(csrf -> csrf.disable())

                // No HTTP sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define endpoint permissions
                .authorizeHttpRequests(auth -> auth

                        // Public API documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Ticket verification: ADMIN or ORGANIZER
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tickets/verify/**"
                        ).hasAnyRole("ADMIN", "ORGANIZER")

                        // Issue tickets: ADMIN only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tickets/issue"
                        ).hasRole("ADMIN")

                        // Mark ticket as used
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tickets/*/use"
                        ).hasAnyRole("ADMIN", "ORGANIZER")

                        // Cancel ticket
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tickets/*/cancel"
                        ).hasAnyRole("ADMIN", "ORGANIZER")

                        // Read tickets
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tickets/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "CUSTOMER",
                                "CLIENT",
                                "ORGANIZER"
                        )

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Enable JWT authentication with Keycloak roles
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }
}
