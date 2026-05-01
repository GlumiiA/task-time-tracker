INSERT INTO employees (id, full_name, username, password, role)
VALUES (1, 'Admin User', 'admin', '$2a$10$i2KacDsUffUWvomhzIdrl.VujUVzeOEJ1hgf455veAdAMjFodf1pS', 'ADMIN')
ON CONFLICT (username) DO UPDATE
SET full_name = EXCLUDED.full_name,
    password = EXCLUDED.password,
    role = EXCLUDED.role;

INSERT INTO employees (id, full_name, username, password, role)
VALUES (2, 'Employee User', 'employee', '$2a$10$i2KacDsUffUWvomhzIdrl.VujUVzeOEJ1hgf455veAdAMjFodf1pS', 'EMPLOYEE')
ON CONFLICT (username) DO UPDATE
SET full_name = EXCLUDED.full_name,
    password = EXCLUDED.password,
    role = EXCLUDED.role;

SELECT setval(
    pg_get_serial_sequence('employees', 'id'),
    COALESCE((SELECT MAX(id) FROM employees), 1),
    true
);
