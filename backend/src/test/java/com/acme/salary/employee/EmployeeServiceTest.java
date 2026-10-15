package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeRepository repository;

	@InjectMocks
	private EmployeeService service;

	@Test
	void createTrimsInputAndSaves() {
		when(repository.existsByEmployeeCode("EMP-1")).thenReturn(false);
		when(repository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EmployeeResponse created = service.create(createRequest(" EMP-1 ", "  Asha Patel "));

		assertThat(created.employeeCode()).isEqualTo("EMP-1");
		assertThat(created.fullName()).isEqualTo("Asha Patel");
		assertThat(created.currency()).isEqualTo("INR");
	}

	@Test
	void createRejectsAnEmployeeCodeThatIsAlreadyTaken() {
		when(repository.existsByEmployeeCode("EMP-1")).thenReturn(true);

		assertThatThrownBy(() -> service.create(createRequest("EMP-1", "Asha Patel")))
				.isInstanceOf(DuplicateEmployeeCodeException.class)
				.hasMessageContaining("EMP-1");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void createReportsADuplicateWhenAConcurrentInsertWinsTheRace() {
		when(repository.existsByEmployeeCode("EMP-1")).thenReturn(false);
		when(repository.saveAndFlush(any(Employee.class)))
				.thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed"));

		assertThatThrownBy(() -> service.create(createRequest("EMP-1", "Asha Patel")))
				.isInstanceOf(DuplicateEmployeeCodeException.class);
	}

	@Test
	void updateChangesEverythingExceptTheEmployeeCode() {
		Employee existing = new Employee("EMP-1", "Asha Patel", "Software Engineer", "Engineering", Country.IN,
				new BigDecimal("1800000"), EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 4));
		when(repository.findById(7L)).thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(existing)).thenReturn(existing);

		EmployeeResponse updated = service.update(7L, new UpdateEmployeeRequest("Asha Patel", "Staff Engineer",
				"Engineering", Country.SG, new BigDecimal("150000"), EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 4)));

		assertThat(updated.employeeCode()).isEqualTo("EMP-1");
		assertThat(updated.jobTitle()).isEqualTo("Staff Engineer");
		assertThat(updated.country()).isEqualTo(Country.SG);
		assertThat(updated.currency()).isEqualTo("SGD");
		assertThat(updated.annualSalary()).isEqualByComparingTo("150000");
	}

	@Test
	void updateOfAMissingEmployeeFails() {
		when(repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(42L, new UpdateEmployeeRequest("A", "B", "C", Country.US,
				BigDecimal.TEN, EmploymentType.CONTRACT, LocalDate.of(2020, 1, 1))))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("42");
	}

	@Test
	void deleteOfAMissingEmployeeFails() {
		when(repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(EmployeeNotFoundException.class);
		verify(repository, never()).delete(any(Employee.class));
	}

	private static CreateEmployeeRequest createRequest(String code, String name) {
		return new CreateEmployeeRequest(code, name, "Software Engineer", "Engineering", Country.IN,
				new BigDecimal("1800000"), EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 4));
	}

}
