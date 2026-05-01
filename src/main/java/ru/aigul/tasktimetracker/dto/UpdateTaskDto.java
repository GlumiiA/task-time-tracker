package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskDto(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description
) {
}
