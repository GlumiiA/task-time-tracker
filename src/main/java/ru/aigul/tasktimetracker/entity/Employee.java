package ru.aigul.tasktimetracker.entity;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    private Long id;

    private String fullName;
    private String username;
    private String password;
    private Role role;

}
