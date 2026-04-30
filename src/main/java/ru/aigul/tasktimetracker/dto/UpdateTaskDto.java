package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotBlank;

/** Запрос на обновление задачи (PUT). */
public record UpdateTaskDto(
        @NotBlank String title,
        String description
) {
}

