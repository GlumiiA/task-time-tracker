package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTaskDto(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description,
        @Positive Long assigneeId
) {
}
