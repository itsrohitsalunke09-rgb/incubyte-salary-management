package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmploymentType;

/** Insight endpoints end-to-end: HTTP, JPQL projections on real SQLite, statistics and JSON shape. */
@SpringBootTest
@AutoConfigureMockMvc
class InsightApiIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private EmployeeRepository employees;

	@BeforeEach
	void seed() {
		employees.deleteAll();
		employees.saveAll(List.of(
				employee("E1", "Software Engineer", "Engineering", Country.IN, "1800000"),
				employee("E2", "Software Engineer", "Engineering", Country.IN, "2400000"),
				employee("E3", "HR Generalist", "People", Country.IN, "900000"),
				employee("E4", "Software Engineer", "Engineering", Country.US, "145000")));
	}

	@AfterEach
	void clean() {
		employees.deleteAll();
	}

	@Test
	void countryOverview() {
		var result = mvc.get().uri("/api/insights/countries").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$[0].country").isEqualTo("IN");
		assertThat(result).bodyJson().extractingPath("$[0].currency").isEqualTo("INR");
		assertThat(result).bodyJson().extractingPath("$[0].headcount").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$[0].median").asNumber()
				.satisfies(median -> assertThat(new BigDecimal(median.toString())).isEqualByComparingTo("1800000"));
		assertThat(result).bodyJson().extractingPath("$[1].country").isEqualTo("US");
	}

	@Test
	void whatDoWePayEachRoleInACountry() {
		var result = mvc.get().uri("/api/insights/countries/IN/job-titles").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.currency").isEqualTo("INR");
		assertThat(result).bodyJson().extractingPath("$.groups[*].name")
				.asArray().containsExactly("HR Generalist", "Software Engineer");
		assertThat(result).bodyJson().extractingPath("$.groups[1].headcount").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.groups[1].median").asNumber()
				.satisfies(median -> assertThat(new BigDecimal(median.toString())).isEqualByComparingTo("2100000"));
	}

	@Test
	void departmentBreakdownForACountry() {
		var result = mvc.get().uri("/api/insights/countries/IN/departments").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.groups[*].name").asArray()
				.containsExactly("Engineering", "People");
		assertThat(result).bodyJson().extractingPath("$.groups[0].totalPayroll").asNumber()
				.satisfies(total -> assertThat(new BigDecimal(total.toString())).isEqualByComparingTo("4200000"));
	}

	@Test
	void countryWithNoEmployeesHasAnEmptyBreakdown() {
		var result = mvc.get().uri("/api/insights/countries/AE/job-titles").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.groups").asArray().isEmpty();
	}

	@Test
	void unknownCountryIsABadRequest() {
		assertThat(mvc.get().uri("/api/insights/countries/XX/job-titles")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private static Employee employee(String code, String title, String department, Country country, String salary) {
		return new Employee(code, "Name " + code, title, department, country, new BigDecimal(salary),
				EmploymentType.FULL_TIME, LocalDate.of(2022, 1, 1));
	}

}
