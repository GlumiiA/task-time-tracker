package ru.aigul.tasktimetracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.exception.BadRequestException;
import ru.aigul.tasktimetracker.exception.ConflictException;
import ru.aigul.tasktimetracker.exception.ForbiddenException;
import ru.aigul.tasktimetracker.exception.InternalServerException;
import ru.aigul.tasktimetracker.exception.NotFoundException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.repository.TaskRepository;
import ru.aigul.tasktimetracker.repository.TimeRecordRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceTest {

    @Mock
    TimeRecordRepository timeRecordRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    TaskRepository taskRepository;

    @InjectMocks
    TimeRecordService timeRecordService;

    @Test
    void createTimeRecordPersistsRecordForDoneTask() {
        CreateTimeRecordDto request = request(2L, 5L);
        JwtPrincipal principal = new JwtPrincipal(2L, "employee", Role.EMPLOYEE);
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findById(5L)).thenReturn(task(5L, Status.DONE));
        when(timeRecordRepository.insertIfTaskDone(any(TimeRecord.class))).thenAnswer(invocation -> {
            TimeRecord record = invocation.getArgument(0);
            record.setId(15L);
            return 1;
        });

        TimeRecord saved = new TimeRecord(
                15L,
                request.employeeId(),
                request.taskId(),
                request.startTime(),
                request.endTime(),
                request.workDescription(),
                LocalDateTime.now()
        );
        when(timeRecordRepository.findById(15L)).thenReturn(saved);

        TimeRecord result = timeRecordService.createTimeRecord(request, principal);

        assertThat(result).isSameAs(saved);
        verify(timeRecordRepository).insertIfTaskDone(any(TimeRecord.class));
    }

    @Test
    void createTimeRecordForAnotherEmployeeIsForbidden() {
        CreateTimeRecordDto request = request(3L, 5L);
        JwtPrincipal principal = new JwtPrincipal(2L, "employee", Role.EMPLOYEE);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request, principal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot create time record");

        verify(employeeRepository, never()).existsById(3L);
        verify(timeRecordRepository, never()).insertIfTaskDone(any(TimeRecord.class));
    }

    @Test
    void createTimeRecordFailsWhenRepositoryRejectsTaskStatus() {
        CreateTimeRecordDto request = request(2L, 5L);
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findById(5L)).thenReturn(task(5L, Status.NEW));
        when(timeRecordRepository.insertIfTaskDone(any(TimeRecord.class))).thenReturn(0);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Task must be DONE");
    }

    @Test
    void createTimeRecordFailsWhenEmployeeDoesNotExist() {
        CreateTimeRecordDto request = request(2L, 5L);
        when(employeeRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Employee not found: 2");

        verify(taskRepository, never()).findById(5L);
    }

    @Test
    void createTimeRecordFailsWhenTaskDoesNotExist() {
        CreateTimeRecordDto request = request(2L, 5L);
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findById(5L)).thenReturn(null);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Task not found: 5");
    }

    @Test
    void createTimeRecordFailsWhenSavedRecordCannotBeLoaded() {
        CreateTimeRecordDto request = request(2L, 5L);
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findById(5L)).thenReturn(task(5L, Status.DONE));
        when(timeRecordRepository.insertIfTaskDone(any(TimeRecord.class))).thenAnswer(invocation -> {
            TimeRecord record = invocation.getArgument(0);
            record.setId(15L);
            return 1;
        });
        when(timeRecordRepository.findById(15L)).thenReturn(null);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Time record not found after insert");
    }

    @Test
    void createTimeRecordRejectsInvalidRequestData() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("request is required");

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(
                new CreateTimeRecordDto(null, 5L, start, start.plusHours(1), "work")
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("employeeId is required");

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(
                new CreateTimeRecordDto(2L, 5L, start, start, "work")
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("endTime must be after startTime");

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(
                new CreateTimeRecordDto(2L, 5L, start, start.plusHours(1), "x".repeat(1001))
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("workDescription length");
    }

    @Test
    void createTimeRecordRejectsInvalidPrincipal() {
        CreateTimeRecordDto request = request(2L, 5L);
        JwtPrincipal principal = new JwtPrincipal(null, "employee", Role.EMPLOYEE);

        assertThatThrownBy(() -> timeRecordService.createTimeRecord(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid authenticated user");
    }

    private CreateTimeRecordDto request(Long employeeId, Long taskId) {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);
        return new CreateTimeRecordDto(
                employeeId,
                taskId,
                start,
                start.plusHours(2),
                "Implementation"
        );
    }

    private Task task(Long id, Status status) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(status);
        return task;
    }
}
