package ru.aigul.tasktimetracker.mapper;

import org.junit.jupiter.api.Test;
import ru.aigul.tasktimetracker.dto.LoginResponseDto;
import ru.aigul.tasktimetracker.dto.TaskDto;
import ru.aigul.tasktimetracker.dto.TimeRecordDto;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    @Test
    void mapsTaskToDto() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime updatedAt = createdAt.plusHours(1);
        Task task = new Task(1L, "Task", "Description", Status.REVIEW, 2L, 3L, createdAt, updatedAt);

        TaskDto dto = new TaskMapper().toDto(task);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Task");
        assertThat(dto.description()).isEqualTo("Description");
        assertThat(dto.status()).isEqualTo(Status.REVIEW);
        assertThat(dto.assigneeId()).isEqualTo(2L);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsTimeRecordToDto() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime createdAt = start.plusHours(3);
        TimeRecord record = new TimeRecord(1L, 2L, 3L, start, start.plusHours(2), "Work", createdAt);

        TimeRecordDto dto = new TimeRecordMapper().toDto(record);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.employeeId()).isEqualTo(2L);
        assertThat(dto.taskId()).isEqualTo(3L);
        assertThat(dto.startTime()).isEqualTo(start);
        assertThat(dto.endTime()).isEqualTo(start.plusHours(2));
        assertThat(dto.workDescription()).isEqualTo("Work");
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void mapsAccessTokenToLoginResponse() {
        LoginResponseDto dto = new EmployeeMapper().toLoginResponse("jwt-token");

        assertThat(dto.accessToken()).isEqualTo("jwt-token");
    }
}
