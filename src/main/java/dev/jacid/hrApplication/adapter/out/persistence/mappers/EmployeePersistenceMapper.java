package dev.jacid.hrApplication.adapter.out.persistence.mappers;

import org.mapstruct.Mapper;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;
import dev.jacid.hrApplication.domain.model.Employee;

@Mapper(componentModel = "spring")
public interface EmployeePersistenceMapper {

    Employee toDomain(EmployeeJpaEntity entity);

    EmployeeJpaEntity toEntity(Employee employee);
}
