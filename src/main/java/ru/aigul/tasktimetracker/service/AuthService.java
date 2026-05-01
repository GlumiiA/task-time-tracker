package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.auth.JwtProvider;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public String login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new UnauthorizedException("Invalid credentials");
        }

        Employee employee = employeeRepository.findByUsername(username.trim());
        if (employee == null || !passwordEncoder.matches(password, employee.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (employee.getId() == null || employee.getRole() == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return jwtProvider.generateAccessToken(employee);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
