package dev.jacid.hrApplication.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;

class EmployeeUpdatePolicyTest {

    private static final Employee JOSE =
            new Employee(1L, "Jose", "IT", "Backend", "jose@test.com", 75600.0, "Mainz");

    @Test
    void managerReplacesEveryFieldButKeepsIdAndName() {
        Employee changes = new Employee(99L, "Renamed", "HR", "Recruiter", "r@test.com", 1.0, "Berlin");

        assertThat(EmployeeUpdatePolicy.updateByManager(JOSE, changes))
                .isEqualTo(new Employee(1L, "Jose", "HR", "Recruiter", "r@test.com", 1.0, "Berlin"));
    }

    @Test
    void managerUpdateMustBeComplete() {
        Employee changes = new Employee(null, null, "HR", null, "r@test.com", 1.0, "Berlin");

        assertThatThrownBy(() -> EmployeeUpdatePolicy.updateByManager(JOSE, changes))
                .isInstanceOf(InvalidEmployeeDataException.class)
                .hasMessage("Missing required fields: role");
    }

    @Test
    void selfUpdateChangesContactDetailsAndKeepsOmittedOnes() {
        Employee changes = new Employee(null, null, null, null, "new@test.com", null, null);

        assertThat(EmployeeUpdatePolicy.updateBySelf(JOSE, changes))
                .isEqualTo(new Employee(1L, "Jose", "IT", "Backend", "new@test.com", 75600.0, "Mainz"));
    }

    @Test
    void selfUpdateAcceptsManagerFieldsSentUnchanged() {
        Employee changes = new Employee(null, "Jose", "IT", "Backend", "jose@test.com", 75600.0, "Berlin");

        assertThat(EmployeeUpdatePolicy.updateBySelf(JOSE, changes).address()).isEqualTo("Berlin");
    }

    @Test
    void selfUpdateCanNotChangeSalaryRoleOrDepartment() {
        Employee salary = new Employee(null, null, null, null, null, 80000.0, null);
        Employee role = new Employee(null, null, null, "Lead", null, null, null);
        Employee department = new Employee(null, null, "Sales", null, null, null, null);

        for (Employee changes : new Employee[] {salary, role, department}) {
            assertThatThrownBy(() -> EmployeeUpdatePolicy.updateBySelf(JOSE, changes))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Only managers can change department, role or salary");
        }
    }
}
