package ru.aigul.tasktimetracker.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.tasktimetracker.dto.LoginResponseDto;
import ru.aigul.tasktimetracker.entity.Employee;

@Component
public class EmployeeMapper {

    public LoginResponseDto toLoginResponse(Employee employee) {
        return new LoginResponseDto(
                employee.getId(),
                employee.getFullName(),
                employee.getUsername(),
                employee.getRole()
        );
    }
}

