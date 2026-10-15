package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Everything HR can change about an employee. The employee code is deliberately absent: it is the
 * business identifier other systems (payroll, HRIS) refer to, so it is immutable once created.
 */
public record UpdateEmployeeRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotBlank @Size(max = 80) String jobTitle,
		@NotBlank @Size(max = 80) String department,
		@NotNull Country country,
		@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal annualSalary,
		@NotNull EmploymentType employmentType,
		@NotNull @PastOrPresent LocalDate hireDate) {

	void applyTo(Employee employee) {
		employee.changeDetails(fullName.trim(), jobTitle.trim(), department.trim(), country, annualSalary,
				employmentType, hireDate);
	}

}
