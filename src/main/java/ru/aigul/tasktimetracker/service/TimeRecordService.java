package ru.aigul.tasktimetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.exception.ConflictException;
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

    public TimeRecord createTimeRecord(CreateTimeRecordDto request) {
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
}

