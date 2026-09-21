package dev.jacid.hrApplication.adapter.in.http.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeRequestDTO;
import dev.jacid.hrApplication.domain.model.Employee;

@Mapper(componentModel = "spring")
public interface EmployeeDtoMapper {

    EmployeeDTO toDto(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Employee toDomain(EmployeeRequestDTO dto);
}
