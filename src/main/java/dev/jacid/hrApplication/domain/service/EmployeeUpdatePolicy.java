package dev.jacid.hrApplication.domain.service;

import java.util.Objects;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;

/**
 * Rules for updating an existing employee. The identity (id, username) and the creation date never
 * change through an update.
 */
public final class EmployeeUpdatePolicy {

    static final String SELF_UPDATE_NOT_ALLOWED = "Employees can only change their own email and address";

    private EmployeeUpdatePolicy() {
    }

    /**
     * Full update by a manager: every field except the username is replaced and must be present.
     *
     * @throws InvalidEmployeeDataException if the body tries to change the username or misses a field
     */
    public static Employee updateByManager(Employee current, Employee changes) {
        if (changes.username() != null && !current.isLinkedTo(changes.username().strip())) {
            throw new InvalidEmployeeDataException("username cannot be changed");
        }
        return new Employee(current.id(), current.username(), changes.name(), changes.department(), changes.role(),
                changes.email(), changes.salary(), changes.address(), current.createdAt()).requireComplete();
    }

    /**
     * Update of an employee's own profile: only the contact details (email, address) may change;
     * omitted contact details are kept. Every other field may be omitted or sent unchanged.
     *
     * @throws OperationNotAllowedException if any other field would change
     */
    public static Employee updateBySelf(Employee current, Employee changes) {
        if ((changes.username() != null && !current.isLinkedTo(changes.username().strip()))
                || modifies(changes.name(), current.name())
                || modifies(changes.department(), current.department())
                || modifies(changes.role(), current.role())
                || modifies(changes.salary(), current.salary())) {
            throw new OperationNotAllowedException(SELF_UPDATE_NOT_ALLOWED);
        }
        return new Employee(current.id(), current.username(), current.name(), current.department(), current.role(),
                keepIfBlank(changes.email(), current.email()), current.salary(),
                keepIfBlank(changes.address(), current.address()), current.createdAt()).requireComplete();
    }

    private static boolean modifies(Object requested, Object current) {
        return requested != null && !Objects.equals(requested, current);
    }

    private static String keepIfBlank(String requested, String current) {
        return requested == null || requested.isBlank() ? current : requested;
    }
}
