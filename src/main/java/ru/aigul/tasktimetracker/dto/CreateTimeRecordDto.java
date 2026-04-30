package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateTimeRecordDto(
        @NotNull Long employeeId,
        @NotNull Long taskId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String workDescription
) {
}

