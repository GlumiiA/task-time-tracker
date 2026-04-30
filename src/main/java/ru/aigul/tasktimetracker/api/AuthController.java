package ru.aigul.tasktimetracker.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.tasktimetracker.dto.LoginRequestDto;
import ru.aigul.tasktimetracker.dto.LoginResponseDto;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.mapper.EmployeeMapper;
import ru.aigul.tasktimetracker.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmployeeMapper employeeMapper;

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        Employee employee = authService.login(request.username(), request.password());
        return employeeMapper.toLoginResponse(employee);
    }
}
