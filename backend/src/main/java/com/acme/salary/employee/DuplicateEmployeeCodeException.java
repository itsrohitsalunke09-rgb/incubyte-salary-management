package com.acme.salary.employee;

public class DuplicateEmployeeCodeException extends RuntimeException {

	public DuplicateEmployeeCodeException(String employeeCode) {
		super("Employee code " + employeeCode + " is already in use");
	}

}
