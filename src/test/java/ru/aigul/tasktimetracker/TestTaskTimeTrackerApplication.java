package ru.aigul.tasktimetracker;

import org.springframework.boot.SpringApplication;

public class TestTaskTimeTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.from(TaskTimeTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
