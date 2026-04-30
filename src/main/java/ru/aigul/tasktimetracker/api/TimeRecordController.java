package ru.aigul.tasktimetracker.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.dto.TimeRecordDto;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.mapper.TimeRecordMapper;
import ru.aigul.tasktimetracker.service.TimeRecordService;

@RestController
@RequestMapping("/api/time-records")
@RequiredArgsConstructor
public class TimeRecordController {

    private final TimeRecordService timeRecordService;
    private final TimeRecordMapper timeRecordMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeRecordDto createTimeRecord(@Valid @RequestBody CreateTimeRecordDto request) {
        TimeRecord record = timeRecordService.createTimeRecord(request);
        return timeRecordMapper.toDto(record);
    }
}
