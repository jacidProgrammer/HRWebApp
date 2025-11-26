package dev.jacid.hrApplication.domain.model.dto;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EmployeeMapper {

    EmployeeMapper INSTANCE = Mappers.getMapper(EmployeeMapper.class);

    EmployeeDTO toDto(EmployeeJpaEntity employee);

    EmployeeJpaEntity toEntity(EmployeeDTO dto);
}
