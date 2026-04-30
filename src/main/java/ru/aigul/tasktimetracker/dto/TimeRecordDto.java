package ru.aigul.tasktimetracker.dto;

import java.time.LocalDateTime;

public record TimeRecordDto(
        Long id,
        Long employeeId,
        Long taskId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String workDescription,
        LocalDateTime createdAt
) {
}

