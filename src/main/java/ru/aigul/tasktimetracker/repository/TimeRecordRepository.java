package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.*;
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
    @Insert("""
            INSERT INTO time_records (employee_id, task_id, start_time, end_time, work_description, created_at)
            SELECT #{employeeId}, #{taskId}, #{startTime}, #{endTime}, #{workDescription}, CURRENT_TIMESTAMP
            WHERE EXISTS (
                SELECT 1 FROM tasks t
                WHERE t.id = #{taskId} AND t.status = 'DONE'
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfTaskDone(TimeRecord timeRecord);

    @Select("""
            SELECT id, employee_id, task_id, start_time, end_time, work_description, created_at
            FROM time_records
            WHERE id = #{id}
            """)
    TimeRecord findById(long id);

    @Select("""
            SELECT id, employee_id, task_id, start_time, end_time, work_description, created_at
            FROM time_records
            WHERE employee_id = #{employeeId}
            ORDER BY start_time
            """)
    List<TimeRecord> findByEmployeeId(long employeeId);

    @Select("""
            SELECT id, employee_id, task_id, start_time, end_time, work_description, created_at
            FROM time_records
            WHERE task_id = #{taskId}
            ORDER BY start_time
            """)
    List<TimeRecord> findByTaskId(long taskId);

    @Select("""
            SELECT id, employee_id, task_id, start_time, end_time, work_description, created_at
            FROM time_records
            WHERE employee_id = #{employeeId}
              AND start_time >= #{from}
              AND end_time <= #{to}
            ORDER BY start_time
            """)
    List<TimeRecord> findByEmployeeAndPeriod(
            @Param("employeeId") long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}

