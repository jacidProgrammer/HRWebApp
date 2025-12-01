package dev.jacid.hrApplication.domain.model.dto;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    EmployeeMapper INSTANCE = Mappers.getMapper(EmployeeMapper.class);

    EmployeeDTO toDto(EmployeeJpaEntity employee);

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "salary", ignore = true)
    EmployeeDTO toDtoPublic(EmployeeJpaEntity employee);

    EmployeeJpaEntity toEntity(EmployeeDTO dto);
}
