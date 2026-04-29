package ru.aigul.tasktimetracker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("ru.aigul.tasktimetracker.repository")
public class TaskTimeTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskTimeTrackerApplication.class, args);
    }

}
