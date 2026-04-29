package ru.aigul.tasktimetracker.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private Long id;

    private String title;

    private String description;

    private Status status;

    /** FK на employees.id (назначенный исполнитель), может быть null */
    private Long assigneeId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
