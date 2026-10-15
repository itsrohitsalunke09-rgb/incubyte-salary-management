package com.acme.salary.employee;

public class EmployeeNotFoundException extends RuntimeException {

	public EmployeeNotFoundException(long id) {
		super("Employee " + id + " does not exist");
	}

}
