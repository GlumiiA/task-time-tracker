package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;

    public Employee login(String username, String password) {
        Employee employee = employeeRepository.findByUsername(username);
        if (employee == null || !employee.getPassword().equals(password)) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return employee;
    }
}

