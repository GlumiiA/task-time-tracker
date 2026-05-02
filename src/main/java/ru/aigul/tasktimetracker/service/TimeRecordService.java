package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.dto.TimeRecordSummaryDto;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
        if (!isRecordableStatus(task.getStatus())) {
            throw new ConflictException("Task must be IN_PROGRESS or REVIEW to create time record");
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

        int inserted = timeRecordRepository.insertIfTaskInProgressOrReview(record);
        if (inserted == 0) {
            throw new ConflictException("Task must be IN_PROGRESS or REVIEW to create time record");
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

    public List<TimeRecord> getTimeRecords(JwtPrincipal principal, Long employeeId, LocalDateTime from, LocalDateTime to) {
        validatePrincipal(principal);
        validatePeriod(from, to);

        if (principal.getRole() != Role.ADMIN) {
            if (employeeId != null && !principal.getId().equals(employeeId)) {
                throw new ForbiddenException("Cannot view time records of another employee");
            }
            return filterByPeriod(timeRecordRepository.findByEmployeeId(principal.getId()), from, to);
        }

        if (employeeId != null) {
            return filterByPeriod(timeRecordRepository.findByEmployeeId(employeeId), from, to);
        }

        return filterByPeriod(timeRecordRepository.findAll(), from, to);
    }

    public TimeRecordSummaryDto getTimeSummary(
            JwtPrincipal principal,
            Long employeeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        validatePrincipal(principal);
        validatePositive(employeeId, "employeeId");
        validatePeriod(from, to);

        if (principal.getRole() != Role.ADMIN && !principal.getId().equals(employeeId)) {
            throw new ForbiddenException("Cannot view time records of another employee");
        }

        List<TimeRecord> records = timeRecordRepository.findByEmployeeAndPeriod(employeeId, from, to);
        BigDecimal totalHours = records.stream()
                .map(this::toHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new TimeRecordSummaryDto(employeeId, from, to, totalHours);
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

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getRole() == null) {
            throw new BadRequestException("Invalid authenticated user");
        }
    }

    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("to must be after from");
        }
    }

    private List<TimeRecord> filterByPeriod(List<TimeRecord> records, LocalDateTime from, LocalDateTime to) {
        return records.stream()
                .filter(record -> from == null || !record.getStartTime().isBefore(from))
                .filter(record -> to == null || !record.getEndTime().isAfter(to))
                .toList();
    }

    private BigDecimal toHours(TimeRecord record) {
        long minutes = Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private boolean isRecordableStatus(Status status) {
        return status == Status.IN_PROGRESS || status == Status.REVIEW;
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
