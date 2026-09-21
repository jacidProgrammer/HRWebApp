package dev.jacid.hrApplication.domain.model;

import java.util.Set;

/** The user performing the current request: their login name and business roles. */
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

    /** Whether this user is the employee with the given name (names are compared ignoring case). */
    public boolean isEmployeeNamed(String employeeName) {
        return isEmployee() && username != null && username.equalsIgnoreCase(employeeName);
    }
}
