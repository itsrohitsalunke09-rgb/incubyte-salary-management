package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee") // schema and indexes: db/migration/V1__create_employee.sql
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_code", nullable = false, length = 20)
	private String employeeCode;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(name = "job_title", nullable = false, length = 80)
	private String jobTitle;

	@Column(nullable = false, length = 80)
	private String department;

	@Enumerated(EnumType.STRING)
	@Column(name = "country_code", nullable = false, length = 2)
	private Country country;

	@Column(name = "annual_salary", nullable = false, precision = 15, scale = 2)
	private BigDecimal annualSalary;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", nullable = false, length = 20)
	private EmploymentType employmentType;

	@Column(name = "hire_date", nullable = false)
	private LocalDate hireDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Employee() {
		// for JPA
	}

	public Employee(String employeeCode, String fullName, String jobTitle, String department, Country country,
			BigDecimal annualSalary, EmploymentType employmentType, LocalDate hireDate) {
		this.employeeCode = employeeCode;
		changeDetails(fullName, jobTitle, department, country, annualSalary, employmentType, hireDate);
	}

	/**
	 * Replaces everything except the identity (id and employee code). v1 keeps only the current
	 * salary, so this overwrites it; salary history is explicitly out of scope.
	 */
	public void changeDetails(String fullName, String jobTitle, String department, Country country,
			BigDecimal annualSalary, EmploymentType employmentType, LocalDate hireDate) {
		this.fullName = fullName;
		this.jobTitle = jobTitle;
		this.department = department;
		this.country = country;
		this.annualSalary = annualSalary;
		this.employmentType = employmentType;
		this.hireDate = hireDate;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Currency currency() {
		return country.currency();
	}

	public Long getId() {
		return id;
	}

	public String getEmployeeCode() {
		return employeeCode;
	}

	public String getFullName() {
		return fullName;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public String getDepartment() {
		return department;
	}

	public Country getCountry() {
		return country;
	}

	public BigDecimal getAnnualSalary() {
		return annualSalary;
	}

	public EmploymentType getEmploymentType() {
		return employmentType;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
