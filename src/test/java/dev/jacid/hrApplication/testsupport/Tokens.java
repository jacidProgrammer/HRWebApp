package dev.jacid.hrApplication.testsupport;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/** Mocked Keycloak access tokens for MockMvc requests (demo users of the seed data). */
public final class Tokens {

    private Tokens() {
    }

    public static JwtRequestPostProcessor employee(String username) {
        return user(username, "EMPLOYEE");
    }

    public static JwtRequestPostProcessor manager() {
        return user("manager", "MANAGER");
    }

    public static JwtRequestPostProcessor user(String username, String role) {
        return jwt().jwt(token -> token.claim("preferred_username", username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
