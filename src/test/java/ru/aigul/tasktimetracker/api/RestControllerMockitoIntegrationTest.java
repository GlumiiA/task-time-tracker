package ru.aigul.tasktimetracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.AssignTaskDto;
import ru.aigul.tasktimetracker.dto.CreateTaskDto;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.dto.LoginRequestDto;
import ru.aigul.tasktimetracker.dto.LoginResponseDto;
import ru.aigul.tasktimetracker.dto.TaskDto;
import ru.aigul.tasktimetracker.dto.TimeRecordDto;
import ru.aigul.tasktimetracker.dto.UpdateStatusDto;
import ru.aigul.tasktimetracker.dto.UpdateTaskDto;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.exception.ApiExceptionHandler;
import ru.aigul.tasktimetracker.mapper.EmployeeMapper;
import ru.aigul.tasktimetracker.mapper.TaskMapper;
import ru.aigul.tasktimetracker.mapper.TimeRecordMapper;
import ru.aigul.tasktimetracker.service.AuthService;
import ru.aigul.tasktimetracker.service.TaskService;
import ru.aigul.tasktimetracker.service.TimeRecordService;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestControllerMockitoIntegrationTest {

    private static final String CURRENT_PRINCIPAL_ATTRIBUTE = "currentPrincipal";

    @Mock
    AuthService authService;

    @Mock
    EmployeeMapper employeeMapper;

    @Mock
    TaskService taskService;

    @Mock
    TaskMapper taskMapper;

    @Mock
    TimeRecordService timeRecordService;

    @Mock
    TimeRecordMapper timeRecordMapper;

    MockMvc mockMvc;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, employeeMapper),
                        new TaskController(taskService, taskMapper),
                        new TimeRecordController(timeRecordService, timeRecordMapper)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new CurrentJwtPrincipalResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        when(authService.login("admin", "password")).thenReturn("jwt-token");
        when(employeeMapper.toLoginResponse("jwt-token")).thenReturn(new LoginResponseDto("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void createTaskReturnsCreatedTask() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        Task task = task(10L, "API tests", Status.NEW, 2L);
        TaskDto dto = taskDto(task);
        when(taskService.createTask("API tests", "Cover REST endpoints", 2L, principal)).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(dto);

        mockMvc.perform(post("/api/tasks")
                        .with(principal(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskDto("API tests", "Cover REST endpoints", 2L)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("API tests"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getTaskByIdReturnsTask() throws Exception {
        Task task = task(10L, "API tests", Status.NEW, 2L);
        when(taskService.getTaskOrThrow(10L)).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(taskDto(task));

        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("API tests"));
    }

    @Test
    void getTasksReturnsFilteredTasks() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        Task task = task(10L, "API tests", Status.DONE, 2L);
        when(taskService.getTasksForUser(principal, 2L, "DONE")).thenReturn(List.of(task));
        when(taskMapper.toDto(task)).thenReturn(taskDto(task));

        mockMvc.perform(get("/api/tasks")
                        .with(principal(principal))
                        .param("assigneeId", "2")
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    @Test
    void deleteTaskReturnsNoContent() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/tasks/10")
                        .with(principal(principal)))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(10L, principal);
    }

    @Test
    void updateTaskReturnsUpdatedTask() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        Task task = task(10L, "Updated", Status.IN_PROGRESS, 2L);
        when(taskService.updateTask(10L, "Updated", "New description", principal)).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(taskDto(task));

        mockMvc.perform(put("/api/tasks/10")
                        .with(principal(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTaskDto("Updated", "New description")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void updateStatusAndAssigneeDelegateToService() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        mockMvc.perform(patch("/api/tasks/10/status")
                        .with(principal(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusDto(Status.DONE))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tasks/10/assignee")
                        .with(principal(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTaskDto(2L))))
                .andExpect(status().isOk());

        verify(taskService).updateStatus(10L, Status.DONE, principal);
        verify(taskService).assignTask(10L, 2L, principal);
    }

    @Test
    void createTimeRecordReturnsCreatedRecord() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(2L, "employee", Role.EMPLOYEE);
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);
        CreateTimeRecordDto request = new CreateTimeRecordDto(
                2L,
                10L,
                start,
                start.plusHours(2),
                "Work log"
        );
        TimeRecord record = new TimeRecord(20L, 2L, 10L, start, start.plusHours(2), "Work log", start.plusHours(3));
        TimeRecordDto dto = new TimeRecordDto(20L, 2L, 10L, start, start.plusHours(2), "Work log", start.plusHours(3));
        when(timeRecordService.createTimeRecord(any(CreateTimeRecordDto.class), eq(principal))).thenReturn(record);
        when(timeRecordMapper.toDto(record)).thenReturn(dto);

        mockMvc.perform(post("/api/time-records")
                        .with(principal(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.employeeId").value(2))
                .andExpect(jsonPath("$.taskId").value(10));
    }

    @Test
    void getTimeRecordsReturnsFilteredRecords() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);
        TimeRecord record = new TimeRecord(20L, 2L, 10L, start, start.plusHours(2), "Work log", start.plusHours(3));
        when(timeRecordService.getTimeRecords(principal, 2L, start, start.plusHours(4))).thenReturn(List.of(record));
        when(timeRecordMapper.toDto(record)).thenReturn(new TimeRecordDto(20L, 2L, 10L, start, start.plusHours(2), "Work log", start.plusHours(3)));

        mockMvc.perform(get("/api/time-records")
                        .with(principal(principal))
                        .param("employeeId", "2")
                        .param("from", "2026-05-01T09:00:00")
                        .param("to", "2026-05-01T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].employeeId").value(2));
    }

    private RequestPostProcessor principal(JwtPrincipal principal) {
        return request -> {
            request.setAttribute(CURRENT_PRINCIPAL_ATTRIBUTE, principal);
            return request;
        };
    }

    private Task task(Long id, String title, Status status, Long assigneeId) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 12, 0);
        return new Task(id, title, "description", status, assigneeId, 1L, now, now);
    }

    private TaskDto taskDto(Task task) {
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getAssigneeId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private class CurrentJwtPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(JwtPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return webRequest.getAttribute(CURRENT_PRINCIPAL_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
