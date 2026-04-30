package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;

import java.util.List;

@Mapper
public interface TaskRepository {
    int insert(Task task);

    Task findById(@Param("id") long id);

    List<Task> findByStatus(@Param("status") Status status);

    List<Task> findByAssigneeId(@Param("assigneeId") long assigneeId);

    List<Task> findByAssigneeIdAndStatus(
            @Param("assigneeId") long assigneeId,
            @Param("status") Status status
    );

    List<Task> findAll();

    int update(Task task);

    int updateStatus(@Param("taskId") long taskId, @Param("status") Status status);

    /** Назначение исполнителя (и перевод в IN_PROGRESS согласно логике диаграмм) */
	int assignEmployee(@Param("taskId") long taskId, @Param("assigneeId") long assigneeId);

    int deleteById(@Param("id") long id);

}
