package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotNull;
import ru.aigul.tasktimetracker.entity.Status;

/** Запрос на смену статуса задачи. */
public record UpdateStatusDto(
        @NotNull Status status
) {
}

