package ru.aigul.tasktimetracker.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.tasktimetracker.dto.TaskDto;
import ru.aigul.tasktimetracker.entity.Task;

@Component
public class TaskMapper {

    public TaskDto toDto(Task task) {
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getAssigneeId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}

