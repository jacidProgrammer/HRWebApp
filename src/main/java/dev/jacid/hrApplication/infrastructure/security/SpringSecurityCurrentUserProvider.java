package dev.jacid.hrApplication.infrastructure.security;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Role;

/**
 * Reads the caller from the Spring Security context: the Keycloak {@code preferred_username} claim
 * (or the principal name for non-JWT authentication) and the {@code ROLE_*} authorities.
 */
@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return CurrentUser.anonymous();
        }
        return new CurrentUser(username(authentication), roles(authentication));
    }

    private static String username(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object preferred = jwtAuth.getToken().getClaim("preferred_username");
            if (preferred != null) {
                return preferred.toString();
            }
        }
        return authentication.getName();
    }

    private static Set<Role> roles(Authentication authentication) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (Role role : Role.values()) {
                if ((ROLE_PREFIX + role.name()).equals(authority.getAuthority())) {
                    roles.add(role);
                }
            }
        }
        return roles;
    }
}
