package com.acme.salary.api;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.acme.salary.employee.DuplicateEmployeeCodeException;
import com.acme.salary.employee.EmployeeNotFoundException;
import com.acme.salary.employee.InvalidSortException;

/**
 * Turns failures into RFC 7807 problem details. The base class already covers malformed JSON,
 * type mismatches (e.g. an unknown country code) and unsupported methods.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmployeeNotFoundException.class)
	ProblemDetail handleNotFound(EmployeeNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Employee not found", ex.getMessage());
	}

	@ExceptionHandler(DuplicateEmployeeCodeException.class)
	ProblemDetail handleDuplicate(DuplicateEmployeeCodeException ex) {
		return problem(HttpStatus.CONFLICT, "Duplicate employee code", ex.getMessage());
	}

	@ExceptionHandler(InvalidSortException.class)
	ProblemDetail handleInvalidSort(InvalidSortException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid sort", ex.getMessage());
	}

	/** Adds a field -> message map so the UI can show errors next to the right inputs. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new TreeMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().headers(headers).body(body);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
