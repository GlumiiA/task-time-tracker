package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ru.aigul.tasktimetracker.entity.TimeRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TimeRecordRepository {

    /**
     * Создаёт запись времени только если связанная задача находится в статусе DONE.
     *
     * @return число вставленных строк (0 если задача не DONE)
     */
    int insertIfTaskDone(TimeRecord timeRecord);

    TimeRecord findById(@Param("id") long id);

    List<TimeRecord> findByEmployeeId(@Param("employeeId") long employeeId);

    List<TimeRecord> findByTaskId(@Param("taskId") long taskId);

    List<TimeRecord> findByEmployeeAndPeriod(
            @Param("employeeId") long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}

