package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** End-to-end through HTTP, service, JPA and the real SQLite schema. */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private EmployeeRepository repository;

	// The in-memory database is shared by every Spring context in the test run,
	// so this test leaves it empty for others whether it passes or fails.
	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		repository.deleteAll();
	}

	@Test
	void fullEmployeeLifecycle() {
		// create
		var created = mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content("""
				{"employeeCode":"EMP-00001","fullName":"Asha Patel","jobTitle":"Software Engineer",
				 "department":"Engineering","country":"IN","annualSalary":1800000.50,
				 "employmentType":"FULL_TIME","hireDate":"2021-01-04"}
				""").exchange();
		assertThat(created).hasStatus(HttpStatus.CREATED);
		long id = repository.findAll().getFirst().getId();

		// duplicate code is rejected by the real database + service
		assertThat(mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content("""
				{"employeeCode":"EMP-00001","fullName":"Someone Else","jobTitle":"Designer",
				 "department":"Product","country":"GB","annualSalary":50000,
				 "employmentType":"CONTRACT","hireDate":"2022-06-01"}
				""")).hasStatus(HttpStatus.CONFLICT);

		// update
		var updated = mvc.put().uri("/api/employees/" + id).contentType(MediaType.APPLICATION_JSON).content("""
				{"fullName":"Asha Patel","jobTitle":"Senior Software Engineer","department":"Engineering",
				 "country":"IN","annualSalary":2400000,"employmentType":"FULL_TIME","hireDate":"2021-01-04"}
				""").exchange();
		assertThat(updated).hasStatus(HttpStatus.OK);
		assertThat(updated).bodyJson().extractingPath("$.jobTitle").isEqualTo("Senior Software Engineer");

		// read back through the list endpoint with a filter
		var list = mvc.get().uri("/api/employees?country=IN&search=asha").exchange();
		assertThat(list).hasStatus(HttpStatus.OK);
		assertThat(list).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
		assertThat(list).bodyJson().extractingPath("$.content[0].annualSalary").asNumber()
				.satisfies(salary -> assertThat(new BigDecimal(salary.toString())).isEqualByComparingTo("2400000"));

		// filter dropdowns reflect the data
		var filters = mvc.get().uri("/api/meta/filters").exchange();
		assertThat(filters).bodyJson().extractingPath("$.departments").asArray().containsExactly("Engineering");
		assertThat(filters).bodyJson().extractingPath("$.countries.length()").isEqualTo(Country.values().length);

		// delete
		assertThat(mvc.delete().uri("/api/employees/" + id)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/employees/" + id)).hasStatus(HttpStatus.NOT_FOUND);
	}

}
