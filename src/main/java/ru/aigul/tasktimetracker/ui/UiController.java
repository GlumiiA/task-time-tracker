package ru.aigul.tasktimetracker.ui;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.service.AuthService;
import ru.aigul.tasktimetracker.service.TaskService;
import ru.aigul.tasktimetracker.service.TimeRecordService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UiController {
    private static final String PRINCIPAL_SESSION_KEY = "uiPrincipal";

    private final AuthService authService;
    private final TaskService taskService;
    private final TimeRecordService timeRecordService;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/ui";
    }

    @GetMapping("/ui")
    public String index(HttpSession session) {
        return currentPrincipal(session) == null ? "redirect:/ui/login" : "redirect:/ui/tasks";
    }

    @GetMapping("/ui/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/ui/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.login(username, password);
            Employee employee = employeeRepository.findByUsername(username.trim());
            session.setAttribute(
                    PRINCIPAL_SESSION_KEY,
                    new JwtPrincipal(employee.getId(), employee.getUsername(), employee.getRole())
            );
            return "redirect:/ui/tasks";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/ui/login";
        }
    }

    @PostMapping("/ui/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/ui/login";
    }

    @GetMapping("/ui/tasks")
    public String tasks(
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model
    ) {
        JwtPrincipal principal = requirePrincipal(session);
        List<Employee> employees = employeeRepository.findAll();
        Map<Long, Employee> employeesById = employees.stream().collect(Collectors.toMap(Employee::getId, employee -> employee));
        List<TaskRowView> taskRows = taskService.getTasksForUser(principal, assigneeId, status).stream()
                .map(task -> toTaskRowView(task, employeesById))
                .toList();

        model.addAttribute("principal", principal);
        model.addAttribute("tasks", taskRows);
        model.addAttribute("employees", employees);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("selectedAssigneeId", assigneeId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", principal.isAdmin() ? "Панель администратора" : "Мои задачи");
        return "tasks";
    }

    @PostMapping("/ui/tasks")
    public String createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        JwtPrincipal principal = requirePrincipal(session);
        try {
            taskService.createTask(title, description, assigneeId, principal);
            redirectAttributes.addFlashAttribute("message", "Задача создана");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ui/tasks";
    }

    @PostMapping("/ui/tasks/status")
    public String updateStatus(
            @RequestParam Long taskId,
            @RequestParam Status status,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        JwtPrincipal principal = requirePrincipal(session);
        try {
            taskService.updateStatus(taskId, status, principal);
            redirectAttributes.addFlashAttribute("message", "Статус обновлен");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ui/tasks";
    }

    @PostMapping("/ui/tasks/assignee")
    public String assignTask(
            @RequestParam Long taskId,
            @RequestParam Long assigneeId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        JwtPrincipal principal = requirePrincipal(session);
        try {
            taskService.assignTask(taskId, assigneeId, principal);
            redirectAttributes.addFlashAttribute("message", "Исполнитель назначен");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ui/tasks";
    }

    @PostMapping("/ui/time-records")
    public String createTimeRecord(
            @RequestParam Long employeeId,
            @RequestParam Long taskId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String workDescription,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        JwtPrincipal principal = requirePrincipal(session);
        try {
            timeRecordService.createTimeRecord(
                    new CreateTimeRecordDto(employeeId, taskId, startTime, endTime, workDescription),
                    principal
            );
            redirectAttributes.addFlashAttribute("message", "Запись времени создана");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ui/tasks";
    }

    private JwtPrincipal currentPrincipal(HttpSession session) {
        Object principal = session.getAttribute(PRINCIPAL_SESSION_KEY);
        return principal instanceof JwtPrincipal jwtPrincipal ? jwtPrincipal : null;
    }

    private JwtPrincipal requirePrincipal(HttpSession session) {
        JwtPrincipal principal = currentPrincipal(session);
        if (principal == null) {
            throw new IllegalStateException("Login required");
        }
        return principal;
    }

    private TaskRowView toTaskRowView(Task task, Map<Long, Employee> employeesById) {
        return new TaskRowView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getAssigneeId(),
                employeeDisplayName(task.getAssigneeId(), employeesById),
                employeeDisplayName(task.getCreatedBy(), employeesById),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private String employeeDisplayName(Long employeeId, Map<Long, Employee> employeesById) {
        if (employeeId == null) {
            return "Не назначен";
        }

        Employee employee = employeesById.get(employeeId);
        if (employee == null) {
            return "Сотрудник #" + employeeId;
        }

        if (employee.getFullName() != null && !employee.getFullName().isBlank()) {
            return employee.getFullName() + " (" + employee.getUsername() + ")";
        }

        return employee.getUsername();
    }

    private record TaskRowView(
            Long id,
            String title,
            String description,
            Status status,
            Long assigneeId,
            String assigneeName,
            String createdByName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleLoginRequired() {
        return "redirect:/ui/login";
    }
}
