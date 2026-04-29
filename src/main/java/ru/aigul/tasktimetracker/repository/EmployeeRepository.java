package ru.aigul.tasktimetracker.repository;

import org.apache.ibatis.annotations.*;
import ru.aigul.tasktimetracker.entity.Employee;

import java.util.List;

@Mapper
public interface EmployeeRepository {

    @Insert("""
            INSERT INTO employees (full_name, username, password, role)
            VALUES (#{fullName}, #{username}, #{password}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Employee employee);

    @Select("""
            SELECT id, full_name, username, password, role
            FROM employees
            WHERE id = #{id}
            """)
    Employee findById(long id);

    @Select("""
            SELECT id, full_name, username, password, role
            FROM employees
            WHERE username = #{username}
            """)
    Employee findByUsername(String username);

    @Select("""
            SELECT id, full_name, username, password, role
            FROM employees
            ORDER BY id
            """)
    List<Employee> findAll();

    @Delete("DELETE FROM employees WHERE id = #{id}")
    int deleteById(long id);
}

