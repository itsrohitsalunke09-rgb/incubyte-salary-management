package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
		Long id,
		String employeeCode,
		String fullName,
		String jobTitle,
		String department,
		Country country,
		String countryName,
		String currency,
		BigDecimal annualSalary,
		EmploymentType employmentType,
		LocalDate hireDate,
		Instant updatedAt) {

	static EmployeeResponse from(Employee employee) {
		return new EmployeeResponse(
				employee.getId(),
				employee.getEmployeeCode(),
				employee.getFullName(),
				employee.getJobTitle(),
				employee.getDepartment(),
				employee.getCountry(),
				employee.getCountry().displayName(),
				employee.currency().getCurrencyCode(),
				employee.getAnnualSalary(),
				employee.getEmploymentType(),
				employee.getHireDate(),
				employee.getUpdatedAt());
	}

}
