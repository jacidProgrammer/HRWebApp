package dev.jacid.hrApplication.domain.model;

import java.util.Set;

/** The user performing the current request: their login name (Keycloak {@code preferred_username}) and roles. */
public record CurrentUser(String username, Set<Role> roles) {

    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static CurrentUser anonymous() {
        return new CurrentUser(null, Set.of());
    }

    public boolean isManager() {
        return roles.contains(Role.MANAGER);
    }

    public boolean isEmployee() {
        return roles.contains(Role.EMPLOYEE);
    }

    /** Whether the given employee record is linked to this user (usernames are compared ignoring case). */
    public boolean owns(Employee employee) {
        return employee != null && employee.isLinkedTo(username);
    }
}
