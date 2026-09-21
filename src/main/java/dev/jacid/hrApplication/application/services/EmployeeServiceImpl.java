package dev.jacid.hrApplication.application.services;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;
import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.service.EmployeeUpdatePolicy;
import jakarta.transaction.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeesUseCases {

    private final EmployeeRepository employeeRepository;
    private final CurrentUserProvider currentUserProvider;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, CurrentUserProvider currentUserProvider) {
        this.employeeRepository = employeeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Managers see every field of every employee; everyone else sees the sensitive fields
     * (salary, address) only on their own profile.
     */
    @Override
    public List<Employee> getAllEmployees() {
        CurrentUser caller = currentUserProvider.currentUser();
        List<Employee> employees = employeeRepository.findAll();
        if (caller.isManager()) {
            return employees;
        }
        return employees.stream()
                .map(employee -> caller.isEmployeeNamed(employee.name()) ? employee : employee.withoutSensitiveData())
                .toList();
    }

    @Override
    public Employee getEmployeeByName(String name) {
        return findExisting(name);
    }

    @Override
    @Transactional
    public Employee createEmployee(Employee employee) {
        employee.requireComplete();
        if (employeeRepository.findByName(employee.name()).isPresent()) {
            throw new EmployeeAlreadyExistsException(employee.name());
        }
        return employeeRepository.save(employee);
    }

    /**
     * Managers may change every field except the name. Employees may only change the contact
     * details (email, address) of their own profile.
     */
    @Override
    @Transactional
    public Employee updateEmployee(String name, Employee changes) {
        if (changes.name() != null && !changes.name().equalsIgnoreCase(name)) {
            throw new InvalidEmployeeDataException(
                    "Name in the body ('" + changes.name() + "') does not match the path ('" + name + "'); renaming is not supported");
        }
        CurrentUser caller = currentUserProvider.currentUser();
        if (caller.isManager()) {
            return employeeRepository.save(EmployeeUpdatePolicy.updateByManager(findExisting(name), changes));
        }
        if (caller.isEmployeeNamed(name)) {
            return employeeRepository.save(EmployeeUpdatePolicy.updateBySelf(findExisting(name), changes));
        }
        throw new OperationNotAllowedException("You can only update your own profile");
    }

    @Override
    @Transactional
    public void deleteEmployeeByName(String name) {
        Employee existing = findExisting(name);
        employeeRepository.deleteByName(existing.name());
    }

    private Employee findExisting(String name) {
        return employeeRepository.findByName(name).orElseThrow(() -> new EmployeeNotFoundException(name));
    }
}
