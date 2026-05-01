package ru.aigul.tasktimetracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.exception.BadRequestException;
import ru.aigul.tasktimetracker.exception.InternalServerException;
import ru.aigul.tasktimetracker.exception.NotFoundException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.repository.TaskRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @InjectMocks
    TaskService taskService;

    @Test
    void createTaskStoresNewTaskAndReturnsSavedTask() {
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        });

        Task saved = task(10L, "Implement tests", Status.NEW, 2L, 1L);
        when(taskRepository.findById(10L)).thenReturn(saved);

        Task result = taskService.createTask("  Implement tests  ", "JUnit 5", 2L, 1L);

        assertThat(result).isSameAs(saved);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Implement tests");
        assertThat(captor.getValue().getDescription()).isEqualTo("JUnit 5");
        assertThat(captor.getValue().getStatus()).isEqualTo(Status.NEW);
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(2L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
    }

    @Test
    void createTaskFailsWhenAssigneeDoesNotExist() {
        when(employeeRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask("Task", null, 2L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Employee not found: 2");

        verify(taskRepository, never()).insert(any(Task.class));
    }

    @Test
    void getTasksForEmployeeUsesAuthenticatedEmployeeId() {
        JwtPrincipal principal = new JwtPrincipal(7L, "employee", Role.EMPLOYEE);
        Task ownTask = task(1L, "Own task", Status.IN_PROGRESS, 7L, 1L);
        when(taskRepository.findByAssigneeId(7L)).thenReturn(List.of(ownTask));

        List<Task> result = taskService.getTasksForUser(principal, 99L, null);

        assertThat(result).containsExactly(ownTask);
        verify(taskRepository).findByAssigneeId(7L);
        verify(taskRepository, never()).findByAssigneeId(99L);
    }

    @Test
    void getTasksForAdminCanFilterByAssigneeAndStatus() {
        JwtPrincipal principal = new JwtPrincipal(1L, "admin", Role.ADMIN);
        Task task = task(3L, "Done task", Status.DONE, 2L, 1L);
        when(taskRepository.findByAssigneeIdAndStatus(2L, Status.DONE)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksForUser(principal, 2L, "done");

        assertThat(result).containsExactly(task);
    }

    @Test
    void getTasksCanFilterByStatusOnlyOrReturnAll() {
        Task doneTask = task(3L, "Done task", Status.DONE, 2L, 1L);
        Task newTask = task(4L, "New task", Status.NEW, null, 1L);
        when(taskRepository.findByStatus(Status.DONE)).thenReturn(List.of(doneTask));
        when(taskRepository.findAll()).thenReturn(List.of(doneTask, newTask));

        assertThat(taskService.getTasks(null, "DONE")).containsExactly(doneTask);
        assertThat(taskService.getTasks(null, null)).containsExactly(doneTask, newTask);
    }

    @Test
    void getTasksRejectsUnknownStatus() {
        assertThatThrownBy(() -> taskService.getTasks(null, "unknown"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown status: unknown");
    }

    @Test
    void getTasksForUserRejectsInvalidPrincipal() {
        assertThatThrownBy(() -> taskService.getTasksForUser(null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid authenticated user");
    }

    @Test
    void updateStatusRejectsMissingStatus() {
        assertThatThrownBy(() -> taskService.updateStatus(1L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("status is required");

        verify(taskRepository, never()).updateStatus(1L, null);
    }

    @Test
    void updateStatusUpdatesExistingTask() {
        when(taskRepository.findById(1L)).thenReturn(task(1L, "Task", Status.NEW, 2L, 1L));
        when(taskRepository.updateStatus(1L, Status.DONE)).thenReturn(1);

        taskService.updateStatus(1L, Status.DONE);

        verify(taskRepository).updateStatus(1L, Status.DONE);
    }

    @Test
    void updateStatusFailsWhenUpdateAffectsNoRows() {
        when(taskRepository.findById(1L)).thenReturn(task(1L, "Task", Status.NEW, 2L, 1L));
        when(taskRepository.updateStatus(1L, Status.DONE)).thenReturn(0);

        assertThatThrownBy(() -> taskService.updateStatus(1L, Status.DONE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Task not found: 1");
    }

    @Test
    void assignTaskUpdatesAssignee() {
        when(taskRepository.findById(1L)).thenReturn(task(1L, "Task", Status.NEW, null, 1L));
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.assignEmployee(1L, 2L)).thenReturn(1);

        taskService.assignTask(1L, 2L);

        verify(taskRepository).assignEmployee(1L, 2L);
    }

    @Test
    void assignTaskFailsWhenEmployeeDoesNotExist() {
        when(taskRepository.findById(1L)).thenReturn(task(1L, "Task", Status.NEW, null, 1L));
        when(employeeRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.assignTask(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Employee not found: 2");
    }

    @Test
    void updateTaskUpdatesTitleAndDescription() {
        Task existing = task(1L, "Old", Status.NEW, 2L, 1L);
        Task updated = task(1L, "New", Status.NEW, 2L, 1L);
        updated.setDescription("New description");
        when(taskRepository.findById(1L)).thenReturn(existing, updated);
        when(taskRepository.update(existing)).thenReturn(1);

        Task result = taskService.updateTask(1L, " New ", "New description");

        assertThat(result).isSameAs(updated);
        assertThat(existing.getTitle()).isEqualTo("New");
        assertThat(existing.getDescription()).isEqualTo("New description");
        verify(taskRepository).update(existing);
    }

    @Test
    void createTaskFailsWhenInsertAffectsNoRows() {
        when(employeeRepository.existsById(2L)).thenReturn(true);
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.insert(any(Task.class))).thenReturn(0);

        assertThatThrownBy(() -> taskService.createTask("Task", null, 2L, 1L))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Task was not created");
    }

    @Test
    void getTaskOrThrowFailsWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.getTaskOrThrow(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Task not found: 99");
    }

    @Test
    void createTaskRejectsBlankTitleAndTooLongDescription() {
        assertThatThrownBy(() -> taskService.createTask(" ", null, 2L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("title is required");

        assertThatThrownBy(() -> taskService.createTask("Task", "x".repeat(501), 2L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("description length");
    }

    private Task task(Long id, String title, Status status, Long assigneeId, Long createdBy) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setStatus(status);
        task.setAssigneeId(assigneeId);
        task.setCreatedBy(createdBy);
        return task;
    }
}
