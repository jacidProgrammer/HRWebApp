package dev.jacid.hrApplication.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Role;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    private static final Employee JOSE =
            new Employee(1L, "Jose", "IT", "Backend", "jose@test.com", 75600.0, "Mainz");
    private static final Employee LOUISA =
            new Employee(2L, "Louisa", "IT", "Agile Coach", "louisa@test.com", 79600.0, "Mainz");

    private static final CurrentUser MANAGER = new CurrentUser("boss", Set.of(Role.MANAGER));
    private static final CurrentUser EMPLOYEE_JOSE = new CurrentUser("jose", Set.of(Role.EMPLOYEE));
    private static final CurrentUser NO_ROLES = new CurrentUser("guest", Set.of());

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private EmployeeServiceImpl service;

    private void callerIs(CurrentUser user) {
        given(currentUserProvider.currentUser()).willReturn(user);
    }

    @Test
    void managerSeesAllDataOfEveryEmployee() {
        callerIs(MANAGER);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE, LOUISA));

        assertThat(service.getAllEmployees()).containsExactly(JOSE, LOUISA);
    }

    @Test
    void employeeSeesSensitiveDataOnlyOnTheirOwnProfile() {
        callerIs(EMPLOYEE_JOSE);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE, LOUISA));

        List<Employee> result = service.getAllEmployees();

        assertThat(result.get(0)).isEqualTo(JOSE);
        assertThat(result.get(1)).isEqualTo(LOUISA.withoutSensitiveData());
        assertThat(result.get(1).salary()).isNull();
        assertThat(result.get(1).address()).isNull();
    }

    @Test
    void userWithoutRolesSeesNoSensitiveData() {
        callerIs(NO_ROLES);
        given(employeeRepository.findAll()).willReturn(List.of(JOSE));

        assertThat(service.getAllEmployees()).containsExactly(JOSE.withoutSensitiveData());
    }

    @Test
    void getEmployeeByNameReturnsTheEmployee() {
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));

        assertThat(service.getEmployeeByName("Jose")).isEqualTo(JOSE);
    }

    @Test
    void getEmployeeByNameFailsWhenMissing() {
        given(employeeRepository.findByName("Nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEmployeeByName("Nobody"))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee 'Nobody' not found");
    }

    @Test
    void createEmployeeSavesNewEmployee() {
        Employee maria = new Employee(null, "Maria", "HR", "Recruiter", "maria@test.com", 52000.0, "Berlin");
        Employee saved = new Employee(3L, "Maria", "HR", "Recruiter", "maria@test.com", 52000.0, "Berlin");
        given(employeeRepository.findByName("Maria")).willReturn(Optional.empty());
        given(employeeRepository.save(maria)).willReturn(saved);

        assertThat(service.createEmployee(maria)).isEqualTo(saved);
    }

    @Test
    void createEmployeeRejectsDuplicateName() {
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));

        assertThatThrownBy(() -> service.createEmployee(JOSE))
                .isInstanceOf(EmployeeAlreadyExistsException.class);
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void createEmployeeRejectsIncompleteData() {
        Employee incomplete = new Employee(null, "Maria", "HR", null, "maria@test.com", null, "Berlin");

        assertThatThrownBy(() -> service.createEmployee(incomplete))
                .isInstanceOf(InvalidEmployeeDataException.class)
                .hasMessage("Missing required fields: role, salary");
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void managerUpdatesEveryFieldExceptTheName() {
        callerIs(MANAGER);
        Employee changes = new Employee(null, null, "Product", "Scrum Master", "l@test.com", 80000.0, "Berlin");
        Employee expected = new Employee(2L, "Louisa", "Product", "Scrum Master", "l@test.com", 80000.0, "Berlin");
        given(employeeRepository.findByName("louisa")).willReturn(Optional.of(LOUISA));
        given(employeeRepository.save(expected)).willReturn(expected);

        assertThat(service.updateEmployee("louisa", changes)).isEqualTo(expected);
    }

    @Test
    void employeeUpdatesTheContactDetailsOfTheirOwnProfile() {
        callerIs(EMPLOYEE_JOSE);
        Employee changes = new Employee(null, "Jose", null, null, "new@test.com", null, "Berlin");
        Employee expected = new Employee(1L, "Jose", "IT", "Backend", "new@test.com", 75600.0, "Berlin");
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));
        given(employeeRepository.save(expected)).willReturn(expected);

        assertThat(service.updateEmployee("Jose", changes)).isEqualTo(expected);
    }

    @Test
    void employeeCanNotRaiseTheirOwnSalary() {
        callerIs(EMPLOYEE_JOSE);
        Employee changes = new Employee(null, "Jose", "IT", "Backend", "jose@test.com", 99999.0, "Mainz");
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));

        assertThatThrownBy(() -> service.updateEmployee("Jose", changes))
                .isInstanceOf(OperationNotAllowedException.class);
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void employeeCanNotUpdateSomeoneElse() {
        callerIs(EMPLOYEE_JOSE);

        assertThatThrownBy(() -> service.updateEmployee("Louisa", LOUISA))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessage("You can only update your own profile");
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void updateRejectsADifferentNameInTheBody() {
        assertThatThrownBy(() -> service.updateEmployee("Jose", LOUISA))
                .isInstanceOf(InvalidEmployeeDataException.class);
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    void updateFailsWhenEmployeeDoesNotExist() {
        callerIs(MANAGER);
        given(employeeRepository.findByName("Nobody")).willReturn(Optional.empty());

        Employee changes = new Employee(null, null, "IT", "Dev", "n@test.com", 1.0, "x");
        assertThatThrownBy(() -> service.updateEmployee("Nobody", changes))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deleteUsesTheStoredNameSoLookupAndDeleteAgreeOnCase() {
        given(employeeRepository.findByName("jose")).willReturn(Optional.of(JOSE));

        service.deleteEmployeeByName("jose");

        then(employeeRepository).should().deleteByName("Jose");
    }

    @Test
    void deleteFailsWhenEmployeeDoesNotExist() {
        given(employeeRepository.findByName("Nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEmployeeByName("Nobody"))
                .isInstanceOf(EmployeeNotFoundException.class);
        then(employeeRepository).should(never()).deleteByName(any());
    }
}
