-- Insert sample pets
INSERT INTO employees (name, department, role, email, salary, address) VALUES ('Jose', 'IT', 'java Senior Backend', 'joseantoniocid.programmer@gmail.com', 75600, 'Mainz, Germany');
INSERT INTO employees (name, department, role, email, salary, address) VALUES ('Louisa', 'IT', 'Senior Agile Coach', 'louisa@gmail.com', 79600, 'Mainz, Germany');

INSERT INTO feedbacks (reporter_id, employee_id, message) VALUES (1, 2, 'Louisa is doing a great job as an Agile Coach!');
INSERT INTO feedbacks (reporter_id, employee_id, message) VALUES (2, 1, 'Jose is an excellent Java Backend Developer!');