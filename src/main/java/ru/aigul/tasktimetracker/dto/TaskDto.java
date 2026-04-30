package ru.aigul.tasktimetracker.dto;

import ru.aigul.tasktimetracker.entity.Status;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        String title,
        String description,
        Status status,
        Long assigneeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

