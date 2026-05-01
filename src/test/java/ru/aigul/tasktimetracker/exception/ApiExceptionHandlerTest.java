package ru.aigul.tasktimetracker.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.aigul.tasktimetracker.dto.CreateTaskDto;
import ru.aigul.tasktimetracker.dto.ErrorResponseDto;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks/404");

    @Test
    void handlesApplicationExceptionsWithExpectedStatuses() {
        assertResponse(handler.handleNotFound(new NotFoundException("missing"), request), HttpStatus.NOT_FOUND, "missing");
        assertResponse(handler.handleBadRequest(new BadRequestException("bad"), request), HttpStatus.BAD_REQUEST, "bad");
        assertResponse(handler.handleUnauthorized(new UnauthorizedException("unauthorized"), request), HttpStatus.UNAUTHORIZED, "unauthorized");
        assertResponse(handler.handleForbidden(new ForbiddenException("forbidden"), request), HttpStatus.FORBIDDEN, "forbidden");
        assertResponse(handler.handleConflict(new ConflictException("conflict"), request), HttpStatus.CONFLICT, "conflict");
        assertResponse(handler.handleInternal(new InternalServerException("internal"), request), HttpStatus.INTERNAL_SERVER_ERROR, "internal");
        assertResponse(handler.handleGeneric(new RuntimeException("boom"), request), HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @Test
    void handlesValidationErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new CreateTaskDto("", null, null),
                "createTaskDto"
        );
        bindingResult.addError(new FieldError("createTaskDto", "title", "must not be blank"));
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("validatedMethod", CreateTaskDto.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        ResponseEntity<ErrorResponseDto> response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter, bindingResult),
                request
        );

        assertResponse(response, HttpStatus.BAD_REQUEST, "title: must not be blank");
    }

    @Test
    void handlesBadRequestTypes() {
        ResponseEntity<ErrorResponseDto> response = handler.handleBadRequestTypes(
                new IllegalArgumentException("type mismatch"),
                request
        );

        assertResponse(response, HttpStatus.BAD_REQUEST, "type mismatch");
    }

    @SuppressWarnings("unused")
    private void validatedMethod(CreateTaskDto request) {
    }

    private void assertResponse(ResponseEntity<ErrorResponseDto> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().error()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo("/api/tasks/404");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
