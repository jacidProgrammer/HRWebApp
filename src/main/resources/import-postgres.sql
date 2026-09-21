-- Sample data for the postgres profile.
-- Runs on every startup (spring.sql.init.mode=always) and is idempotent: rows are only inserted
-- while the corresponding table is still empty, so restarts never duplicate or overwrite data.
INSERT INTO employees (name, department, role, email, salary, address)
SELECT v.name, v.department, v.role, v.email, v.salary, v.address
FROM (VALUES
    ('Jose', 'IT', 'Java Senior Backend', 'joseantoniocid.programmer@gmail.com', 75600, 'Mainz, Germany'),
    ('Louisa', 'IT', 'Senior Agile Coach', 'louisa@gmail.com', 79600, 'Mainz, Germany')
) AS v(name, department, role, email, salary, address)
WHERE NOT EXISTS (SELECT 1 FROM employees);

INSERT INTO feedbacks (reporter_id, employee_id, message)
SELECT reporter.id, employee.id, v.message
FROM (VALUES
    ('Jose', 'Louisa', 'Louisa is doing a great job as an Agile Coach!'),
    ('Louisa', 'Jose', 'Jose is an excellent Java Backend Developer!')
) AS v(reporter_name, employee_name, message)
JOIN employees reporter ON reporter.name = v.reporter_name
JOIN employees employee ON employee.name = v.employee_name
WHERE NOT EXISTS (SELECT 1 FROM feedbacks);
