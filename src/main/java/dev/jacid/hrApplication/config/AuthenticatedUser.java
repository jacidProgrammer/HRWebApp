package dev.jacid.hrApplication.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public String getUserName() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
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
