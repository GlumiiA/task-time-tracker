package ru.aigul.tasktimetracker.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.tasktimetracker.auth.JwtPrincipal;
import ru.aigul.tasktimetracker.dto.CreateTimeRecordDto;
import ru.aigul.tasktimetracker.dto.TimeRecordDto;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.mapper.TimeRecordMapper;
import ru.aigul.tasktimetracker.service.TimeRecordService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/time-records")
@RequiredArgsConstructor
public class TimeRecordController {

    private final TimeRecordService timeRecordService;
    private final TimeRecordMapper timeRecordMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeRecordDto createTimeRecord(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTimeRecordDto request
    ) {
        TimeRecord record = timeRecordService.createTimeRecord(request, principal);
        return timeRecordMapper.toDto(record);
    }

    @GetMapping
    public List<TimeRecordDto> getTimeRecords(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return timeRecordService.getTimeRecords(principal, employeeId, from, to).stream()
                .map(timeRecordMapper::toDto)
                .toList();
    }
}
