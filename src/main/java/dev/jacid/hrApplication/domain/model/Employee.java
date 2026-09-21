package dev.jacid.hrApplication.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;

/**
 * An employee of the company.
 * <ul>
 *   <li>{@code username} links the employee to their Keycloak account (the token's
 *       {@code preferred_username}). It is unique, stored in lower case and never changes.</li>
 *   <li>{@code name} is only a display name: it can change and does not need to be unique.</li>
 *   <li>{@code salary} and {@code address} are sensitive and are only visible to managers and to
 *       the employee themselves.</li>
 * </ul>
 */
public record Employee(UUID id,
                       String username,
                       String name,
                       String department,
                       String role,
                       String email,
                       Double salary,
                       String address,
                       Instant createdAt) {

    /** Characters Keycloak accepts in usernames (it stores them in lower case). */
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._@-]{1,64}");

    /** Copy of this employee with the sensitive fields (salary, address) removed. */
    public Employee withoutSensitiveData() {
        return new Employee(id, username, name, department, role, email, null, null, createdAt);
    }

    /** Whether this employee's record belongs to the user with the given login name (case-insensitive). */
    public boolean isLinkedTo(String loginName) {
        return username != null && loginName != null && username.equalsIgnoreCase(loginName);
    }

    /**
     * A new employee from the requested data: no id yet, a normalised (trimmed, lower-case) username and the
     * given creation date. (Static so that MapStruct does not take it for a fluent setter.)
     */
    public static Employee newFrom(Employee request, Instant creationTime) {
        return new Employee(null, normalizeUsername(request.username()), request.name(), request.department(),
                request.role(), request.email(), request.salary(), request.address(), creationTime);
    }

    /** @throws InvalidEmployeeDataException if a mandatory field is missing or the username is malformed */
    public Employee requireComplete() {
        List<String> missing = new ArrayList<>();
        if (isBlank(username)) missing.add("username");
        if (isBlank(name)) missing.add("name");
        if (isBlank(department)) missing.add("department");
        if (isBlank(role)) missing.add("role");
        if (isBlank(email)) missing.add("email");
        if (salary == null) missing.add("salary");
        if (isBlank(address)) missing.add("address");
        if (!missing.isEmpty()) {
            throw new InvalidEmployeeDataException("Missing required fields: " + String.join(", ", missing));
        }
        if (!USERNAME.matcher(username).matches()) {
            throw new InvalidEmployeeDataException(
                    "username must be 1-64 lower-case letters, digits or . _ @ - (the Keycloak username)");
        }
        return this;
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
