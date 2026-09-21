package dev.jacid.hrApplication.application.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;
import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.TimeProvider;
import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.service.EmployeeUpdatePolicy;
import jakarta.transaction.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeesUseCases {

    private final EmployeeRepository employeeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TimeProvider timeProvider;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, CurrentUserProvider currentUserProvider,
                               TimeProvider timeProvider) {
        this.employeeRepository = employeeRepository;
        this.currentUserProvider = currentUserProvider;
        this.timeProvider = timeProvider;
    }

    @Override
    public List<Employee> getAllEmployees() {
        CurrentUser caller = currentUserProvider.currentUser();
        return employeeRepository.findAll().stream().map(employee -> visibleTo(caller, employee)).toList();
    }

    @Override
    public Employee getCurrentEmployee() {
        String username = currentUserProvider.currentUser().username();
        if (username == null) {
            throw EmployeeNotFoundException.linkedTo(null);
        }
        return employeeRepository.findByUsername(username).orElseThrow(() -> EmployeeNotFoundException.linkedTo(username));
    }

    @Override
    public Employee getEmployee(UUID id) {
        return visibleTo(currentUserProvider.currentUser(), findExisting(id));
    }

    @Override
    @Transactional
    public Employee createEmployee(Employee employee) {
        Employee candidate = Employee.newFrom(employee, timeProvider.now()).requireComplete();
        if (employeeRepository.findByUsername(candidate.username()).isPresent()) {
            throw new EmployeeAlreadyExistsException(candidate.username());
        }
        return employeeRepository.save(candidate);
    }

    /**
     * Managers may change every field except the username. Employees may only change the contact
     * details (email, address) of their own record.
     */
    @Override
    @Transactional
    public Employee updateEmployee(UUID id, Employee changes) {
        Employee current = findExisting(id);
        CurrentUser caller = currentUserProvider.currentUser();
        if (caller.isManager()) {
            return employeeRepository.save(EmployeeUpdatePolicy.updateByManager(current, changes));
        }
        if (caller.owns(current)) {
            return employeeRepository.save(EmployeeUpdatePolicy.updateBySelf(current, changes));
        }
        throw new OperationNotAllowedException("You can only update your own profile");
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID id) {
        employeeRepository.deleteById(findExisting(id).id());
    }

    /** Managers see every field; everybody else sees salary and address only on their own record. */
    private static Employee visibleTo(CurrentUser caller, Employee employee) {
        return caller.isManager() || caller.owns(employee) ? employee : employee.withoutSensitiveData();
    }

    private Employee findExisting(UUID id) {
        return employeeRepository.findById(id).orElseThrow(() -> EmployeeNotFoundException.withId(id));
    }
}
