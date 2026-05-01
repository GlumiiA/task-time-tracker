package ru.aigul.tasktimetracker.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.AssignTaskDto;
import ru.aigul.tasktimetracker.dto.CreateTaskDto;
import ru.aigul.tasktimetracker.dto.TaskDto;
import ru.aigul.tasktimetracker.dto.UpdateStatusDto;
import ru.aigul.tasktimetracker.dto.UpdateTaskDto;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.mapper.TaskMapper;
import ru.aigul.tasktimetracker.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTaskDto request
    ) {
        Task task = taskService.createTask(request.title(), request.description(), request.assigneeId(), principal);
        return taskMapper.toDto(task);
    }

    @GetMapping("/{id}")
    public TaskDto getTaskById(@PathVariable Long id) {
        return taskMapper.toDto(taskService.getTaskOrThrow(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        taskService.deleteTask(id, principal);
    }

    @GetMapping
    public List<TaskDto> getTasks(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status
    ) {
        return taskService.getTasksForUser(principal, assigneeId, status).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateStatusDto request
    ) {
        taskService.updateStatus(id, request.status(), principal);
    }

    @PatchMapping("/{id}/assignee")
    public void assignTask(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AssignTaskDto request
    ) {
        taskService.assignTask(id, request.assigneeId(), principal);
    }

    @PutMapping("/{id}")
    public TaskDto updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateTaskDto request
    ) {
        Task task = taskService.updateTask(id, request.title(), request.description(), principal);
        return taskMapper.toDto(task);
    }
}
