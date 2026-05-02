package ru.aigul.tasktimetracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.aigul.tasktimetracker.entity.Employee;
import ru.aigul.tasktimetracker.entity.Role;
import ru.aigul.tasktimetracker.entity.Status;
import ru.aigul.tasktimetracker.entity.Task;
import ru.aigul.tasktimetracker.entity.TimeRecord;
import ru.aigul.tasktimetracker.repository.EmployeeRepository;
import ru.aigul.tasktimetracker.repository.TaskRepository;
import ru.aigul.tasktimetracker.repository.TimeRecordRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class TimeRecordMapperTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

	@Autowired
	EmployeeRepository employeeRepository;

	@Autowired
	TaskRepository taskRepository;

	@Autowired
	TimeRecordRepository timeRecordRepository;

	@Test
    void shouldInsertTimeRecordOnlyForInProgressOrReviewTasks() {
        Employee employee = new Employee(null, "Test User", "test.user", "secret", Role.EMPLOYEE);
        employeeRepository.insert(employee);
        assertThat(employee.getId()).isNotNull();

        Task inProgressTask = new Task();
        inProgressTask.setTitle("In progress task");
        inProgressTask.setDescription("desc");
        inProgressTask.setStatus(Status.IN_PROGRESS);
        taskRepository.insert(inProgressTask);
        assertThat(inProgressTask.getId()).isNotNull();

        Task reviewTask = new Task();
        reviewTask.setTitle("Review task");
        reviewTask.setDescription("desc");
        reviewTask.setStatus(Status.REVIEW);
        taskRepository.insert(reviewTask);
        assertThat(reviewTask.getId()).isNotNull();

        Task newTask = new Task();
        newTask.setTitle("New task");
        newTask.setDescription("desc");
        newTask.setStatus(Status.NEW);
        taskRepository.insert(newTask);
        assertThat(newTask.getId()).isNotNull();

        Task blockedTask = new Task();
        blockedTask.setTitle("Blocked task");
        blockedTask.setDescription("desc");
        blockedTask.setStatus(Status.BLOCKED);
        taskRepository.insert(blockedTask);
        assertThat(blockedTask.getId()).isNotNull();

        LocalDateTime now = LocalDateTime.now();

        TimeRecord ok = new TimeRecord();
        ok.setEmployeeId(employee.getId());
        ok.setTaskId(inProgressTask.getId());
        ok.setStartTime(now.minusHours(3));
        ok.setEndTime(now.minusHours(2));
        ok.setWorkDescription("work");
        int insertedOk = timeRecordRepository.insertIfTaskInProgressOrReview(ok);
        assertThat(insertedOk).isEqualTo(1);

        TimeRecord reviewOk = new TimeRecord();
        reviewOk.setEmployeeId(employee.getId());
        reviewOk.setTaskId(reviewTask.getId());
        reviewOk.setStartTime(now.minusHours(2));
        reviewOk.setEndTime(now.minusHours(1));
        reviewOk.setWorkDescription("work");
        int insertedReviewOk = timeRecordRepository.insertIfTaskInProgressOrReview(reviewOk);
        assertThat(insertedReviewOk).isEqualTo(1);

        TimeRecord notOk = new TimeRecord();
        notOk.setEmployeeId(employee.getId());
        notOk.setTaskId(newTask.getId());
        notOk.setStartTime(now.minusHours(1));
        notOk.setEndTime(now);
        notOk.setWorkDescription("work");
        int insertedNotOk = timeRecordRepository.insertIfTaskInProgressOrReview(notOk);
        assertThat(insertedNotOk).isEqualTo(0);

        TimeRecord blockedNotOk = new TimeRecord();
        blockedNotOk.setEmployeeId(employee.getId());
        blockedNotOk.setTaskId(blockedTask.getId());
        blockedNotOk.setStartTime(now.minusHours(1));
        blockedNotOk.setEndTime(now);
        blockedNotOk.setWorkDescription("work");
        int insertedBlockedNotOk = timeRecordRepository.insertIfTaskInProgressOrReview(blockedNotOk);
        assertThat(insertedBlockedNotOk).isEqualTo(0);
    }
}


