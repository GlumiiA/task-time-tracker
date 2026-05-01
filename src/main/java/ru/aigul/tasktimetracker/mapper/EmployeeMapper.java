package ru.aigul.tasktimetracker.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.tasktimetracker.dto.LoginResponseDto;

@Component
public class EmployeeMapper {

    public LoginResponseDto toLoginResponse(String accessToken) {
        return new LoginResponseDto(accessToken);
    }
}
