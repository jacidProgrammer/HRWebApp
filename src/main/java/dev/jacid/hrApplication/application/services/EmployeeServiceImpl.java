package dev.jacid.hrApplication.application.services;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;
import dev.jacid.hrApplication.application.mappers.EmployeeMapper;
import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.infrastructure.security.AuthenticatedUser;
import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;
import jakarta.transaction.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeesUseCases {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AuthenticatedUser auth;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper,
                               AuthenticatedUser auth) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.auth = auth;
    }
    
    @Override
    public List<EmployeeDTO> getAllEmployees() {
        return filterDataVisibility(employeeRepository.findAll());
    }
    
    private boolean hasPermissionToViewOrEditAllData() {
        return auth.isManager();
    }

    private boolean hasPermissionToViewOrEditThisProfile(String employeeName) {
        return auth.isEmployee() && employeeName.equals(auth.getUserName());
    }
    
    private List<EmployeeDTO> filterSensitiveData(List<EmployeeJpaEntity> employeeList) {
        return employeeList.stream().map(employee -> {
            if(hasPermissionToViewOrEditThisProfile(employee.getName())) {
                return employeeMapper.toDto(employee);
            } else {
                return employeeMapper.toDtoPublic(employee);
            }
        }).toList();
    }

    public List<EmployeeDTO> filterDataVisibility(List<EmployeeJpaEntity> employeeList) {
        if(employeeList.isEmpty()) {
            return List.of();
        }
        
        if(hasPermissionToViewOrEditAllData()) {
            return employeeList.stream().map(employeeMapper::toDto).toList();
        } else {
            return filterSensitiveData(employeeList);
        }
    }

    public EmployeeDTO getEmployeeByName(String name) {
        EmployeeJpaEntity employeeEntity = employeeRepository.findByName(name);
        if(employeeEntity == null) {
            throw new IllegalArgumentException("Employee with this name doesn't exist");
        }
        return employeeMapper.toDto(employeeEntity);
    }

    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        EmployeeJpaEntity employeeEntity = employeeRepository.findByName(employeeDTO.name());
        if(employeeEntity != null) {
            throw new IllegalArgumentException("Employee with this name already exists");
        }
        employeeRepository.save(employeeMapper.toEntity(employeeDTO));
        return employeeDTO;
    }

    @Transactional
    public EmployeeDTO updateEmployee(EmployeeDTO employeeDTO) {
        if(!hasPermissionToViewOrEditAllData() && !hasPermissionToViewOrEditThisProfile(employeeDTO.name())) {
            throw new IllegalArgumentException("You don't have permission to edit this employee");
        }
        
        EmployeeJpaEntity employeeEntity = employeeRepository.findByName(employeeDTO.name());
        if(employeeEntity == null) {
            throw new IllegalArgumentException("Employee with this name doesn't exist");
        }
        EmployeeJpaEntity newEntity = employeeMapper.toEntity(employeeDTO);
        newEntity.setId(employeeEntity.getId());
        employeeRepository.save(newEntity);
        return employeeDTO;
    }

    @Transactional
    public void deleteEmployeeByName(String name) {
        EmployeeJpaEntity employeeEntity = employeeRepository.findByName(name);
        if(employeeEntity == null) {
            throw new IllegalArgumentException("Employee with this name doesn't exist");
        }
        employeeRepository.deleteByName(name);
    }
}
