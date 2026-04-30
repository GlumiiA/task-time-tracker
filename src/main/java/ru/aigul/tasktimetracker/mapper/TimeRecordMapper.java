package ru.aigul.tasktimetracker.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.tasktimetracker.dto.TimeRecordDto;
import ru.aigul.tasktimetracker.entity.TimeRecord;

@Component
public class TimeRecordMapper {

    public TimeRecordDto toDto(TimeRecord record) {
        return new TimeRecordDto(
                record.getId(),
                record.getEmployeeId(),
                record.getTaskId(),
                record.getStartTime(),
                record.getEndTime(),
                record.getWorkDescription(),
                record.getCreatedAt()
        );
    }
}

