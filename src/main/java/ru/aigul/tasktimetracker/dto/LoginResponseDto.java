package ru.aigul.tasktimetracker.dto;

import ru.aigul.tasktimetracker.entity.Role;

public record LoginResponseDto(
        Long id,
        String fullName,
        String username,
        Role role
) {
}

