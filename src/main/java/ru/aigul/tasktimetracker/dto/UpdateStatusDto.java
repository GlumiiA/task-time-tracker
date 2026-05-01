package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotNull;
import ru.aigul.tasktimetracker.entity.Status;

public record UpdateStatusDto(
        @NotNull Status status
) {
}
