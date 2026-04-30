package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ru.aigul.tasktimetracker.entity.Employee;

import java.util.List;

@Mapper
public interface EmployeeRepository {
    int insert(Employee employee);

    Employee findById(@Param("id") long id);

    Employee findByUsername(@Param("username") String username);

    boolean existsById(@Param("id") long id);

    List<Employee> findAll();

    int deleteById(@Param("id") long id);
}
