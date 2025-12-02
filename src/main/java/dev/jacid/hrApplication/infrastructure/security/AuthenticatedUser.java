package dev.jacid.hrApplication.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object preferred = jwt.getClaim("preferred_username");
            if (preferred != null) {
                return preferred.toString(); 
            }
        }
        return authentication != null ? authentication.getName() : null;
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    
    public boolean isManager() {
        return hasRole("MANAGER");
    }
    
    public boolean isEmployee() {
        return hasRole("EMPLOYEE");
    }
}
