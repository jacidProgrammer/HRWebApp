package dev.jacid.hrApplication.domain.service;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;

class EmployeeUpdatePolicyTest {

    private static Employee changes(String username, String name, String department, String role, String email,
                                    Double salary, String address) {
        return new Employee(null, username, name, department, role, email, salary, address, null);
    }

    @Test
    void managerReplacesEveryFieldButKeepsIdUsernameAndCreationDate() {
        Employee changes = new Employee(UUID.randomUUID(), null, "José A. Cid", "HR", "Recruiter", "r@test.com", 1.0,
                "Berlin", null);

        assertThat(EmployeeUpdatePolicy.updateByManager(JOSE, changes))
                .isEqualTo(new Employee(JOSE.id(), "jose", "José A. Cid", "HR", "Recruiter", "r@test.com", 1.0, "Berlin",
                        JOSE.createdAt()));
    }

    @Test
    void managerMaySendTheUnchangedUsername() {
        Employee changes = changes("JOSE", "Jose", "HR", "Recruiter", "r@test.com", 1.0, "Berlin");

        assertThat(EmployeeUpdatePolicy.updateByManager(JOSE, changes).username()).isEqualTo("jose");
    }

    @Test
    void managerCanNotChangeTheUsername() {
        Employee changes = changes("pepe", "Jose", "HR", "Recruiter", "r@test.com", 1.0, "Berlin");

        assertThatThrownBy(() -> EmployeeUpdatePolicy.updateByManager(JOSE, changes))
                .isInstanceOf(InvalidEmployeeDataException.class)
                .hasMessage("username cannot be changed");
    }

    @Test
    void managerUpdateMustBeComplete() {
        Employee changes = changes(null, "Jose", "HR", null, "r@test.com", 1.0, "Berlin");

        assertThatThrownBy(() -> EmployeeUpdatePolicy.updateByManager(JOSE, changes))
                .isInstanceOf(InvalidEmployeeDataException.class)
                .hasMessage("Missing required fields: role");
    }

    @Test
    void selfUpdateChangesContactDetailsAndKeepsOmittedOnes() {
        Employee changes = changes(null, null, null, null, "new@test.com", null, null);

        Employee updated = EmployeeUpdatePolicy.updateBySelf(JOSE, changes);

        assertThat(updated.email()).isEqualTo("new@test.com");
        assertThat(updated.address()).isEqualTo(JOSE.address());
        assertThat(updated).usingRecursiveComparison().ignoringFields("email").isEqualTo(JOSE);
    }

    @Test
    void selfUpdateAcceptsOtherFieldsSentUnchanged() {
        Employee changes = changes("jose", JOSE.name(), JOSE.department(), JOSE.role(), JOSE.email(), JOSE.salary(), "Berlin");

        assertThat(EmployeeUpdatePolicy.updateBySelf(JOSE, changes).address()).isEqualTo("Berlin");
    }

    @Test
    void selfUpdateCanNotChangeAnythingButContactDetails() {
        Employee[] forbidden = {
                changes("pepe", null, null, null, null, null, null),
                changes(null, "Pepe", null, null, null, null, null),
                changes(null, null, "Sales", null, null, null, null),
                changes(null, null, null, "Lead", null, null, null),
                changes(null, null, null, null, null, 80000.0, null),
        };
        for (Employee changes : forbidden) {
            assertThatThrownBy(() -> EmployeeUpdatePolicy.updateBySelf(JOSE, changes))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Employees can only change their own email and address");
        }
    }
}
