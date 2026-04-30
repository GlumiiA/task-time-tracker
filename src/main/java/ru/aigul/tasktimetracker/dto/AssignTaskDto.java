package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotNull;

/** Запрос на назначение исполнителя. */
public record AssignTaskDto(
        @NotNull Long assigneeId
) {
}

