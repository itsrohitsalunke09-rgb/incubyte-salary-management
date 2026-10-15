CREATE TABLE employee (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_code   VARCHAR(20)    NOT NULL,
    full_name       VARCHAR(120)   NOT NULL,
    job_title       VARCHAR(80)    NOT NULL,
    department      VARCHAR(80)    NOT NULL,
    country_code    VARCHAR(2)     NOT NULL CHECK (country_code IN ('IN', 'US', 'GB', 'DE', 'SG', 'AE')),
    annual_salary   NUMERIC(15, 2) NOT NULL CHECK (annual_salary > 0),
    employment_type VARCHAR(20)    NOT NULL CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT')),
    hire_date       DATE           NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL
);

CREATE UNIQUE INDEX ux_employee_code ON employee (employee_code);
CREATE INDEX ix_employee_country ON employee (country_code);
CREATE INDEX ix_employee_country_title ON employee (country_code, job_title);
CREATE INDEX ix_employee_country_department ON employee (country_code, department);
