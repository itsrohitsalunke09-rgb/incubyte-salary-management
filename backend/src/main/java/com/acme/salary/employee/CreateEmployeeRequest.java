package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
		@NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9-]+", message = "may only contain letters, digits and '-'")
		String employeeCode,
		@NotBlank @Size(max = 120) String fullName,
		@NotBlank @Size(max = 80) String jobTitle,
		@NotBlank @Size(max = 80) String department,
		@NotNull Country country,
		@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal annualSalary,
		@NotNull EmploymentType employmentType,
		@NotNull @PastOrPresent LocalDate hireDate) {

	Employee toEmployee() {
		return new Employee(employeeCode.trim(), fullName.trim(), jobTitle.trim(), department.trim(), country,
				annualSalary, employmentType, hireDate);
	}

}
