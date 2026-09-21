package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.EmployeePersistenceMapper;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.model.Employee;

@Repository
@Profile({"h2","postgres","test"})
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private static final Sort BY_NAME = Sort.by("name", "username");

    private final EmployeeRepositoryJpa employeeRepositoryJpa;
    private final FeedbackRepositoryJpa feedbackRepositoryJpa;
    private final EmployeePersistenceMapper mapper;

    public EmployeeRepositoryAdapter(EmployeeRepositoryJpa employeeRepositoryJpa,
                                     FeedbackRepositoryJpa feedbackRepositoryJpa,
                                     EmployeePersistenceMapper mapper) {
        this.employeeRepositoryJpa = employeeRepositoryJpa;
        this.feedbackRepositoryJpa = feedbackRepositoryJpa;
        this.mapper = mapper;
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepositoryJpa.findAll(BY_NAME).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return employeeRepositoryJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        return employeeRepositoryJpa.findByUsernameIgnoreCase(username).map(mapper::toDomain);
    }

    @Override
    public long count() {
        return employeeRepositoryJpa.count();
    }

    @Override
    public Employee save(Employee employee) {
        return mapper.toDomain(employeeRepositoryJpa.save(mapper.toEntity(employee)));
    }

    /** The foreign keys cascade the same way; doing it here keeps the persistence context consistent. */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        feedbackRepositoryJpa.deleteByRecipientId(id);
        feedbackRepositoryJpa.detachAuthor(id);
        employeeRepositoryJpa.deleteById(id);
    }
}
