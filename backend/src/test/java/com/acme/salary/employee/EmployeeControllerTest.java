package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

	private static final String VALID_CREATE = """
			{"employeeCode":"EMP-00001","fullName":"Asha Patel","jobTitle":"Software Engineer",
			 "department":"Engineering","country":"IN","annualSalary":1800000,
			 "employmentType":"FULL_TIME","hireDate":"2021-01-04"}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private EmployeeService service;

	@Test
	void createReturns201WithLocationAndBody() {
		when(service.create(any())).thenReturn(response(15L));

		var result = mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE).exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/employees/15");
		assertThat(result).bodyJson().extractingPath("$.currency").isEqualTo("INR");
	}

	@Test
	void createWithInvalidFieldsReturnsProblemDetailWithFieldErrors() {
		String invalid = """
				{"employeeCode":"bad code!","fullName":"","jobTitle":"Engineer","department":"Engineering",
				 "country":"IN","annualSalary":-5,"employmentType":"FULL_TIME","hireDate":"2999-01-01"}
				""";

		var result = mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(invalid).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Validation failed");
		assertThat(result).bodyJson().extractingPath("$.errors").asMap()
				.containsOnlyKeys("employeeCode", "fullName", "annualSalary", "hireDate");
		verifyNoInteractions(service);
	}

	@Test
	void createWithUnknownCountryIsABadRequest() {
		String unknownCountry = VALID_CREATE.replace("\"IN\"", "\"XX\"");

		assertThat(mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(unknownCountry))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(service);
	}

	@Test
	void duplicateEmployeeCodeIsAConflict() {
		when(service.create(any())).thenThrow(new DuplicateEmployeeCodeException("EMP-00001"));

		var result = mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_CREATE).exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.detail").asString().contains("EMP-00001");
	}

	@Test
	void missingEmployeeIsNotFound() {
		when(service.get(99L)).thenThrow(new EmployeeNotFoundException(99L));

		var result = mvc.get().uri("/api/employees/99").exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(404);
	}

	@Test
	void listPassesFiltersAndPagingToTheService() {
		when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of(response(1L)), PageRequest.of(2, 10), 21));

		var result = mvc.get().uri("/api/employees?search=asha&country=IN&department=Engineering"
				+ "&jobTitle=Software Engineer&page=2&size=10&sort=annualSalary,desc").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(21);
		assertThat(result).bodyJson().extractingPath("$.content[0].employeeCode").isEqualTo("EMP-00001");

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).list(eq(new EmployeeFilter("asha", Country.IN, "Engineering", "Software Engineer")),
				pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "annualSalary"));
	}

	@Test
	void listDefaultsToTwentyPerPageSortedByName() {
		when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of()));

		assertThat(mvc.get().uri("/api/employees")).hasStatus(HttpStatus.OK);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).list(eq(EmployeeFilter.none()), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("fullName"));
	}

	@Test
	void pageSizeIsCappedAtOneHundred() {
		when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of()));

		mvc.get().uri("/api/employees?size=5000").exchange();

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).list(any(), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
	}

	@Test
	void sortingByAnUnsupportedFieldIsABadRequest() {
		var result = mvc.get().uri("/api/employees?sort=createdAt,desc").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail").asString().contains("createdAt");
		verifyNoInteractions(service);
	}

	@Test
	void deleteReturns204() {
		assertThat(mvc.delete().uri("/api/employees/3")).hasStatus(HttpStatus.NO_CONTENT);
		verify(service).delete(3L);
	}

	@Test
	void deleteOfMissingEmployeeIsNotFound() {
		doThrow(new EmployeeNotFoundException(3L)).when(service).delete(3L);

		assertThat(mvc.delete().uri("/api/employees/3")).hasStatus(HttpStatus.NOT_FOUND);
	}

	private static EmployeeResponse response(long id) {
		return new EmployeeResponse(id, "EMP-00001", "Asha Patel", "Software Engineer", "Engineering", Country.IN,
				"India", "INR", new BigDecimal("1800000"), EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 4),
				Instant.parse("2026-10-15T10:00:00Z"));
	}

}
