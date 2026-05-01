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
	void shouldInsertTimeRecordOnlyForDoneTasks() {
		Employee employee = new Employee(null, "Test User", "test.user", "secret", Role.EMPLOYEE);
		employeeRepository.insert(employee);
		assertThat(employee.getId()).isNotNull();

		Task doneTask = new Task();
		doneTask.setTitle("Done task");
		doneTask.setDescription("desc");
		doneTask.setStatus(Status.DONE);
		taskRepository.insert(doneTask);
		assertThat(doneTask.getId()).isNotNull();

		Task newTask = new Task();
		newTask.setTitle("New task");
		newTask.setDescription("desc");
		newTask.setStatus(Status.NEW);
		taskRepository.insert(newTask);
		assertThat(newTask.getId()).isNotNull();

		LocalDateTime now = LocalDateTime.now();

		TimeRecord ok = new TimeRecord();
		ok.setEmployeeId(employee.getId());
		ok.setTaskId(doneTask.getId());
		ok.setStartTime(now.minusHours(2));
		ok.setEndTime(now.minusHours(1));
		ok.setWorkDescription("work");
		int insertedOk = timeRecordRepository.insertIfTaskDone(ok);
		assertThat(insertedOk).isEqualTo(1);

		TimeRecord notOk = new TimeRecord();
		notOk.setEmployeeId(employee.getId());
		notOk.setTaskId(newTask.getId());
		notOk.setStartTime(now.minusHours(2));
		notOk.setEndTime(now.minusHours(1));
		notOk.setWorkDescription("work");
		int insertedNotOk = timeRecordRepository.insertIfTaskDone(notOk);
		assertThat(insertedNotOk).isEqualTo(0);
	}
}


