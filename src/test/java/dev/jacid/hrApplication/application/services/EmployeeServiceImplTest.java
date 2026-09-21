package dev.jacid.hrApplication.application.services;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Role;

class EmployeeServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:15:30Z");

    private static final CurrentUser MANAGER = new CurrentUser("manager", Set.of(Role.MANAGER));
    private static final CurrentUser EMPLOYEE_JOSE = new CurrentUser("JOSE", Set.of(Role.EMPLOYEE));
    private static final CurrentUser NO_ROLES = new CurrentUser("guest", Set.of());

    private EmployeeRepository employeeRepository;
    private CurrentUserProvider currentUserProvider;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        service = new EmployeeServiceImpl(employeeRepository, currentUserProvider, () -> NOW);
    }

    private void callerIs(CurrentUser user) {
        given(currentUserProvider.currentUser()).willReturn(user);
    }

    private static Employee request(String username, String name, String department, String role, String email,
                                    Double salary, String address) {
        return new Employee(null, username, name, department, role, email, salary, address, null);
    }

    @Test
    void managerSeesAllDataOfEveryEmployee() {
        callerIs(MANAGER);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE, LOUISA));

        assertThat(service.getAllEmployees()).containsExactly(JOSE, LOUISA);
    }

    @Test
    void employeeSeesSensitiveDataOnlyOnTheirOwnRecordMatchedByUsername() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE, LOUISA));

        assertThat(service.getAllEmployees()).containsExactly(JOSE, LOUISA.withoutSensitiveData());
    }

    @Test
    void userWithoutRolesSeesNoSensitiveData() {
        callerIs(NO_ROLES);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE));

        assertThat(service.getAllEmployees()).containsExactly(JOSE.withoutSensitiveData());
    }

    @Test
    void getEmployeeHidesSensitiveDataOfOthers() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findById(LOUISA.id())).willReturn(Optional.of(LOUISA));
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));

        assertThat(service.getEmployee(LOUISA.id()).salary()).isNull();
        assertThat(service.getEmployee(JOSE.id())).isEqualTo(JOSE);
    }

    @Test
    void getEmployeeFailsWhenMissing() {
        UUID unknown = UUID.randomUUID();
        given(employeeRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEmployee(unknown))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee '" + unknown + "' not found");
    }

    @Test
    void currentEmployeeIsFoundByTheTokenUsername() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findByUsername("JOSE")).willReturn(Optional.of(JOSE));

        assertThat(service.getCurrentEmployee()).isEqualTo(JOSE);
    }

    @Test
    void currentEmployeeIsMissingForUsersWithoutRecord() {
        callerIs(MANAGER);
        given(employeeRepository.findByUsername("manager")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentEmployee())
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("No employee record is linked to user 'manager'");
    }

    @Test
    void createEmployeeNormalisesTheUsernameAndSetsTheCreationDate() {
        Employee maria = request(" Maria ", "María García", "Sales", "AE", "maria@test.com", 52000.0, "Berlin");
        Employee expected = new Employee(null, "maria", "María García", "Sales", "AE", "maria@test.com", 52000.0, "Berlin", NOW);
        Employee saved = new Employee(UUID.randomUUID(), "maria", "María García", "Sales", "AE", "maria@test.com", 52000.0, "Berlin", NOW);
        given(employeeRepository.findByUsername("maria")).willReturn(Optional.empty());
        given(employeeRepository.save(expected)).willReturn(saved);

        assertThat(service.createEmployee(maria)).isEqualTo(saved);
    }

    @Test
    void createEmployeeRejectsATakenUsername() {
        given(employeeRepository.findByUsername("jose")).willReturn(Optional.of(JOSE));

        assertThatThrownBy(() -> service.createEmployee(request("Jose", "Another José", "IT", "Dev", "j2@test.com", 1.0, "x")))
                .isInstanceOf(EmployeeAlreadyExistsException.class)
                .hasMessage("An employee with username 'jose' already exists");
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void createEmployeeRejectsIncompleteData() {
        Employee incomplete = request(null, "Maria", "HR", null, "maria@test.com", null, "Berlin");

        assertThatThrownBy(() -> service.createEmployee(incomplete))
                .isInstanceOf(InvalidEmployeeDataException.class)
                .hasMessage("Missing required fields: username, role, salary");
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void managerUpdatesEveryFieldExceptTheUsername() {
        callerIs(MANAGER);
        Employee changes = request(null, "Louisa B.", "Product", "Scrum Master", "l@test.com", 80000.0, "Berlin");
        Employee expected = new Employee(LOUISA.id(), "louisa", "Louisa B.", "Product", "Scrum Master", "l@test.com",
                80000.0, "Berlin", LOUISA.createdAt());
        given(employeeRepository.findById(LOUISA.id())).willReturn(Optional.of(LOUISA));
        given(employeeRepository.save(expected)).willReturn(expected);

        assertThat(service.updateEmployee(LOUISA.id(), changes)).isEqualTo(expected);
    }

    @Test
    void employeeUpdatesTheContactDetailsOfTheirOwnRecord() {
        callerIs(EMPLOYEE_JOSE);
        Employee changes = request(null, null, null, null, "new@test.com", null, "Berlin");
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));
        given(employeeRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Employee updated = service.updateEmployee(JOSE.id(), changes);

        assertThat(updated.email()).isEqualTo("new@test.com");
        assertThat(updated.address()).isEqualTo("Berlin");
        assertThat(updated.salary()).isEqualTo(JOSE.salary());
    }

    @Test
    void employeeCanNotChangeTheirOwnName() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));

        assertThatThrownBy(() -> service.updateEmployee(JOSE.id(), request(null, "Pepe", null, null, null, null, null)))
                .isInstanceOf(OperationNotAllowedException.class);
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void employeeCanNotUpdateSomeoneElse() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findById(LOUISA.id())).willReturn(Optional.of(LOUISA));

        assertThatThrownBy(() -> service.updateEmployee(LOUISA.id(), request(null, null, null, null, "x@test.com", null, null)))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessage("You can only update your own profile");
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void updateFailsWhenEmployeeDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        given(employeeRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEmployee(unknown, request(null, "N", "IT", "Dev", "n@test.com", 1.0, "x")))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deleteRemovesTheEmployee() {
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));

        service.deleteEmployee(JOSE.id());

        then(employeeRepository).should().deleteById(JOSE.id());
    }

    @Test
    void deleteFailsWhenEmployeeDoesNotExist() {
        UUID unknown = UUID.randomUUID();
        given(employeeRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEmployee(unknown)).isInstanceOf(EmployeeNotFoundException.class);
        then(employeeRepository).should(never()).deleteById(any());
    }
}
