package ru.aigul.tasktimetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeRecordSummaryDto(
        Long employeeId,
        LocalDateTime from,
        LocalDateTime to,
        BigDecimal totalHours
) {
}
