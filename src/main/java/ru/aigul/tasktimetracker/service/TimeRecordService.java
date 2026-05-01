package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.exception.BadRequestException;
import ru.aigul.tasktimetracker.exception.ConflictException;
import ru.aigul.tasktimetracker.exception.ForbiddenException;
import ru.aigul.tasktimetracker.exception.InternalServerException;
import ru.aigul.tasktimetracker.exception.NotFoundException;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.repository.TaskRepository;
import ru.aigul.tasktimetracker.repository.TimeRecordRepository;

@Service
@RequiredArgsConstructor
public class TimeRecordService {

    private final TimeRecordRepository timeRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

    public TimeRecord createTimeRecord(CreateTimeRecordDto request, JwtPrincipal principal) {
        validateRequest(request);
        validatePrincipalAccess(request.employeeId(), principal);

        if (!employeeRepository.existsById(request.employeeId())) {
            throw new NotFoundException("Employee not found: " + request.employeeId());
        }

        Task task = taskRepository.findById(request.taskId());
        if (task == null) {
            throw new NotFoundException("Task not found: " + request.taskId());
        }

        TimeRecord record = new TimeRecord(
                null,
                request.employeeId(),
                request.taskId(),
                request.startTime(),
                request.endTime(),
                request.workDescription(),
                null
        );

        int inserted = timeRecordRepository.insertIfTaskDone(record);
        if (inserted == 0) {
            throw new ConflictException("Task must be DONE to create time record");
        }

        TimeRecord saved = timeRecordRepository.findById(record.getId());
        if (saved == null) {
            throw new InternalServerException("Time record not found after insert");
        }

        return saved;
    }

    public TimeRecord createTimeRecord(CreateTimeRecordDto request) {
        return createTimeRecord(request, null);
    }

    private void validateRequest(CreateTimeRecordDto request) {
        if (request == null) {
            throw new BadRequestException("request is required");
        }
        validatePositive(request.employeeId(), "employeeId");
        validatePositive(request.taskId(), "taskId");
        if (request.startTime() == null) {
            throw new BadRequestException("startTime is required");
        }
        if (request.endTime() == null) {
            throw new BadRequestException("endTime is required");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (request.workDescription() != null && request.workDescription().length() > 1000) {
            throw new BadRequestException("workDescription length must be less than or equal to 1000");
        }
    }

    private void validatePrincipalAccess(Long employeeId, JwtPrincipal principal) {
        if (principal == null) {
            return;
        }
        if (principal.getId() == null || principal.getRole() == null) {
            throw new BadRequestException("Invalid authenticated user");
        }
        if (principal.getRole() != Role.ADMIN && !principal.getId().equals(employeeId)) {
            throw new ForbiddenException("Cannot create time record for another employee");
        }
    }

    private void validatePositive(Long value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        if (value <= 0) {
            throw new BadRequestException(field + " must be positive");
        }
    }
}
