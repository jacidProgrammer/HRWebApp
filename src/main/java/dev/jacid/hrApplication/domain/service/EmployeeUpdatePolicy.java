package dev.jacid.hrApplication.domain.service;

import java.util.Objects;

import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;

/**
 * Rules for updating an existing employee. The identity (id and name) never changes through an update.
 */
public final class EmployeeUpdatePolicy {

    private EmployeeUpdatePolicy() {
    }

    /** Full update by a manager: every other field is replaced and must be present. */
    public static Employee updateByManager(Employee current, Employee changes) {
        return new Employee(current.id(), current.name(), changes.department(), changes.role(),
                changes.email(), changes.salary(), changes.address()).requireComplete();
    }

    /**
     * Update of an employee's own profile: only the contact details (email, address) may change;
     * omitted contact details are kept. Department, role and salary may be omitted or sent unchanged,
     * but only a manager can modify them.
     *
     * @throws OperationNotAllowedException if a manager-only field would change
     */
    public static Employee updateBySelf(Employee current, Employee changes) {
        if (modifies(changes.department(), current.department())
                || modifies(changes.role(), current.role())
                || modifies(changes.salary(), current.salary())) {
            throw new OperationNotAllowedException("Only managers can change department, role or salary");
        }
        return new Employee(current.id(), current.name(), current.department(), current.role(),
                keepIfBlank(changes.email(), current.email()), current.salary(),
                keepIfBlank(changes.address(), current.address())).requireComplete();
    }

    private static boolean modifies(Object requested, Object current) {
        return requested != null && !Objects.equals(requested, current);
    }

    private static String keepIfBlank(String requested, String current) {
        return requested == null || requested.isBlank() ? current : requested;
    }
}
