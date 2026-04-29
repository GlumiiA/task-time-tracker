package ru.aigul.tasktimetracker.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeRecord {

    private Long id;

    /** FK на employees.id */
    private Long employeeId;

    /** FK на tasks.id */
    private Long taskId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String workDescription;

    private LocalDateTime createdAt;
}

