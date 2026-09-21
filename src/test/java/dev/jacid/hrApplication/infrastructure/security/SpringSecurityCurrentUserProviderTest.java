package dev.jacid.hrApplication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Role;

class SpringSecurityCurrentUserProviderTest {

    private final SpringSecurityCurrentUserProvider provider = new SpringSecurityCurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsPreferredUsernameAndRolesFromKeycloakJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("3f1c-uuid")
                .claim("preferred_username", "Jose")
                .issuedAt(Instant.now())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"), new SimpleGrantedAuthority("SCOPE_profile"))));

        assertThat(provider.currentUser()).isEqualTo(new CurrentUser("Jose", Set.of(Role.EMPLOYEE)));
    }

    @Test
    void fallsBackToPrincipalNameForOtherAuthentications() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "manager", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));

        CurrentUser user = provider.currentUser();

        assertThat(user.username()).isEqualTo("manager");
        assertThat(user.isManager()).isTrue();
        assertThat(user.isEmployee()).isFalse();
    }

    @Test
    void returnsAnonymousWithoutAuthentication() {
        assertThat(provider.currentUser()).isEqualTo(CurrentUser.anonymous());
    }
}
