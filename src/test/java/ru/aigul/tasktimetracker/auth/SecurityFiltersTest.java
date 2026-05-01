package ru.aigul.tasktimetracker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.aigul.tasktimetracker.dto.ErrorResponseDto;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.exception.ForbiddenException;
import ru.aigul.tasktimetracker.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFiltersTest {

    @Mock
    JwtProvider jwtProvider;

    @Mock
    FilterChain filterChain;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtAuthenticationFilterAuthenticatesBearerToken() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        when(jwtProvider.parseToken("jwt-token")).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(jwtProvider).doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(principal);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void jwtAuthenticationFilterSkipsRequestWithoutBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(jwtProvider).doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).parseToken("jwt-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void exceptionHandlerFilterWritesUnauthorizedError() throws Exception {
        ErrorResponseDto body = runExceptionHandlerFilter(new UnauthorizedException("Authentication required"));

        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Authentication required");
        assertThat(body.path()).isEqualTo("/api/tasks");
    }

    @Test
    void exceptionHandlerFilterWritesForbiddenError() throws Exception {
        ErrorResponseDto body = runExceptionHandlerFilter(new ForbiddenException("Access denied"));

        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message()).isEqualTo("Access denied");
    }

    @Test
    void exceptionHandlerFilterWritesUnexpectedError() throws Exception {
        ErrorResponseDto body = runExceptionHandlerFilter(new IllegalStateException("boom"));

        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("Unexpected error");
    }

    private ErrorResponseDto runExceptionHandlerFilter(RuntimeException exception) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain throwingChain = (servletRequest, servletResponse) -> {
            throw exception;
        };

        new ExceptionHandlerFilter(objectMapper).doFilter(request, response, throwingChain);

        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        return objectMapper.readValue(response.getContentAsString(), ErrorResponseDto.class);
    }
}
