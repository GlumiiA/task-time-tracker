package ru.aigul.tasktimetracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.aigul.tasktimetracker.auth.JwtProvider;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    AuthService authService;

    @Test
    void loginReturnsAccessTokenForValidCredentials() {
        Employee employee = new Employee(1L, "Admin", "admin", "$2a$hash", Role.ADMIN);
        when(employeeRepository.findByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.matches("password", "$2a$hash")).thenReturn(true);
        when(jwtProvider.generateAccessToken(employee)).thenReturn("jwt-token");

        String token = authService.login(" admin ", "password");

        assertThat(token).isEqualTo("jwt-token");
        verify(employeeRepository).findByUsername("admin");
    }

    @Test
    void loginRejectsInvalidPassword() {
        Employee employee = new Employee(1L, "Admin", "admin", "$2a$hash", Role.ADMIN);
        when(employeeRepository.findByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
