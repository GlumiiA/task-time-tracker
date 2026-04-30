
package ru.aigul.tasktimetracker.dto;

import jakarta.validation.constraints.NotBlank;

/** Запрос на создание задачи. */
public record CreateTaskDto(
		@NotBlank String title,
		String description
) {
}


