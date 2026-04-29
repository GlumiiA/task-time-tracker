package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.*;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;

import java.util.List;

@Mapper
public interface TaskRepository {

    @Insert("""
            INSERT INTO tasks (title, description, status, assignee_id, created_at, updated_at)
            VALUES (#{title}, #{description}, #{status}, #{assigneeId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    @Select("""
            SELECT id, title, description, status, assignee_id, created_at, updated_at
            FROM tasks
            WHERE id = #{id}
            """)
    Task findById(long id);

    @Select("""
            SELECT id, title, description, status, assignee_id, created_at, updated_at
            FROM tasks
            WHERE status = #{status}
            ORDER BY id
            """)
    List<Task> findByStatus(Status status);

    @Select("""
            SELECT id, title, description, status, assignee_id, created_at, updated_at
            FROM tasks
            ORDER BY id
            """)
    List<Task> findAll();

    @Update("""
            UPDATE tasks
            SET title = #{title},
                description = #{description},
                status = #{status},
                assignee_id = #{assigneeId},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(Task task);

    @Update("""
            UPDATE tasks
            SET status = #{status}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId}
            """)
    int updateStatus(@Param("taskId") long taskId, @Param("status") Status status);

    /** Назначение исполнителя (и перевод в IN_PROGRESS согласно логике диаграмм) */
    @Update("""
            UPDATE tasks
            SET assignee_id = #{assigneeId}, status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId}
            """)
    int assignEmployee(@Param("taskId") long taskId, @Param("assigneeId") long assigneeId);

    @Delete("DELETE FROM tasks WHERE id = #{id}")
    int deleteById(long id);
}

