package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignTaskDto(
        @NotNull @Positive Long assigneeId
) {
}
