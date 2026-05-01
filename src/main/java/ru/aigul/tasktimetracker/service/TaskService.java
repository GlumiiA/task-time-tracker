package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.exception.BadRequestException;
import ru.aigul.tasktimetracker.exception.ForbiddenException;
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

    public Task createTask(String title, String description, Long assigneeId, JwtPrincipal principal) {
        validateText(title, "title", 100, true);
        validateText(description, "description", 500, false);
        validatePositive(assigneeId, "assigneeId");
        validatePrincipal(principal);
        requireAdmin(principal, "Only admin can create tasks");

        if (assigneeId != null && !employeeRepository.existsById(assigneeId)) {
            throw new NotFoundException("Employee not found: " + assigneeId);
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setStatus(Status.NEW);
        task.setAssigneeId(assigneeId);
        task.setCreatedBy(principal.getId());

        int inserted = taskRepository.insert(task);
        if (inserted == 0) {
            throw new InternalServerException("Task was not created");
        }

        return getTaskOrThrow(task.getId());
    }

    public Task getTaskOrThrow(Long id) {
        validatePositive(id, "id");

        Task task = taskRepository.findById(id);
        if (task == null) {
            throw new NotFoundException("Task not found: " + id);
        }
        return task;
    }

    public List<Task> getTasksForUser(JwtPrincipal principal, Long assigneeId, String status) {
        validatePrincipal(principal);
        validatePositive(assigneeId, "assigneeId");

        if (principal.getRole() == Role.ADMIN) {
            return getTasks(assigneeId, status);
        }
        return getTasks(principal.getId(), status);
    }

    public List<Task> getTasks(Long assigneeId, String status) {
        validatePositive(assigneeId, "assigneeId");
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
        updateStatus(taskId, status, null);
    }

    public void updateStatus(Long taskId, Status status, JwtPrincipal principal) {
        validatePositive(taskId, "taskId");
        if (status == null) {
            throw new BadRequestException("status is required");
        }

        Task task = getTaskOrThrow(taskId);
        validatePrincipal(principal);
        if (principal.getRole() != Role.ADMIN && !principal.getId().equals(task.getAssigneeId())) {
            throw new ForbiddenException("Cannot change status of another employee's task");
        }

        int updated = taskRepository.updateStatus(taskId, status);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }
    }

    public void assignTask(Long taskId, Long employeeId, JwtPrincipal principal) {
        validatePositive(taskId, "taskId");
        validatePositive(employeeId, "employeeId");
        validatePrincipal(principal);
        requireAdmin(principal, "Only admin can assign tasks");
        getTaskOrThrow(taskId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new NotFoundException("Employee not found: " + employeeId);
        }

        int updated = taskRepository.assignEmployee(taskId, employeeId);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }
    }

    public Task updateTask(Long taskId, String title, String description, JwtPrincipal principal) {
        validatePositive(taskId, "taskId");
        validateText(title, "title", 100, true);
        validateText(description, "description", 500, false);
        validatePrincipal(principal);
        requireAdmin(principal, "Only admin can edit tasks");

        Task task = getTaskOrThrow(taskId);
        task.setTitle(title.trim());
        task.setDescription(description);

        int updated = taskRepository.update(task);
        if (updated == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }

        return getTaskOrThrow(taskId);
    }

    public void deleteTask(Long taskId, JwtPrincipal principal) {
        validatePositive(taskId, "taskId");
        validatePrincipal(principal);
        requireAdmin(principal, "Only admin can delete tasks");
        getTaskOrThrow(taskId);

        int deleted = taskRepository.deleteById(taskId);
        if (deleted == 0) {
            throw new NotFoundException("Task not found: " + taskId);
        }
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

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getRole() == null) {
            throw new BadRequestException("Invalid authenticated user");
        }
    }

    private void requireAdmin(JwtPrincipal principal, String message) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException(message);
        }
    }

    private void validatePositive(Long value, String field) {
        if (value != null && value <= 0) {
            throw new BadRequestException(field + " must be positive");
        }
    }

    private void validateText(String value, String field, int max, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BadRequestException(field + " is required");
            }
            return;
        }
        if (value.length() > max) {
            throw new BadRequestException(field + " length must be less than or equal to " + max);
        }
    }
}
