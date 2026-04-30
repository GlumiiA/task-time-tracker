package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.exception.BadRequestException;
import ru.aigul.tasktimetracker.exception.InternalServerException;
import ru.aigul.tasktimetracker.exception.NotFoundException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.repository.TaskRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;

    public Task createTask(String title, String description) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(Status.NEW);

        int inserted = taskRepository.insert(task);
        if (inserted == 0) {
            throw new InternalServerException("Task was not created");
        }

        return getTaskOrThrow(task.getId());
    }

    public Task getTaskOrThrow(Long id) {
        Task task = taskRepository.findById(id);
        if (task == null) {
            throw new NotFoundException("Task not found: " + id);
        }
        return task;
    }

    public List<Task> getTasks(Long assigneeId, String status) {
        Status parsedStatus = parseStatusOrNull(status);

        if (assigneeId != null && parsedStatus != null) {
            return taskRepository.findByAssigneeIdAndStatus(assigneeId, parsedStatus);
        }

        if (assigneeId != null) {
            return taskRepository.findByAssigneeId(assigneeId);
        }

        if (parsedStatus != null) {
            return taskRepository.findByStatus(parsedStatus);
        }

        return taskRepository.findAll();
    }

    public void updateStatus(Long taskId, Status status) {
        getTaskOrThrow(taskId);
        int updated = taskRepository.updateStatus(taskId, status);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }
    }

    public void assignTask(Long taskId, Long employeeId) {
        getTaskOrThrow(taskId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new NotFoundException("Employee not found: " + employeeId);
        }

        int updated = taskRepository.assignEmployee(taskId, employeeId);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }
    }

    public Task updateTask(Long taskId, String title, String description) {
        Task task = getTaskOrThrow(taskId);
        task.setTitle(title);
        task.setDescription(description);

        int updated = taskRepository.update(task);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }

        return getTaskOrThrow(taskId);
    }

    private Status parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown status: " + status);
        }
    }
}