package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // run on SQLite, like production
class EmployeeRepositoryTest {

	@Autowired
	private EmployeeRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void seed() {
		repository.saveAll(List.of(
				employee("EMP-00001", "Asha Patel", "Software Engineer", "Engineering", Country.IN, "1800000"),
				employee("EMP-00002", "Rahul Mehta", "Software Engineer", "Engineering", Country.IN, "2400000"),
				employee("EMP-00003", "Priya Nair", "HR Generalist", "People", Country.IN, "900000"),
				employee("EMP-00004", "John Carter", "Software Engineer", "Engineering", Country.US, "145000"),
				employee("EMP-00005", "Emily Stone", "Product Manager", "Product", Country.US, "160000")));
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void persistsAndReadsBackAllFieldsWithExactSalary() {
		Employee saved = repository.saveAndFlush(
				employee("EMP-00099", "Lena Vogel", "Data Analyst", "Finance", Country.DE, "1234567.89"));
		entityManager.clear();

		Employee found = repository.findById(saved.getId()).orElseThrow();

		assertThat(found.getEmployeeCode()).isEqualTo("EMP-00099");
		assertThat(found.getFullName()).isEqualTo("Lena Vogel");
		assertThat(found.getCountry()).isEqualTo(Country.DE);
		assertThat(found.currency().getCurrencyCode()).isEqualTo("EUR");
		assertThat(found.getAnnualSalary()).isEqualByComparingTo("1234567.89");
		assertThat(found.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
		assertThat(found.getHireDate()).isEqualTo(LocalDate.of(2022, 4, 1));
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
	}

	@Test
	void rejectsDuplicateEmployeeCode() {
		Employee duplicate = employee("EMP-00001", "Someone Else", "Designer", "Product", Country.GB, "50000");

		assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsNonPositiveSalaryAsALastLineOfDefence() {
		Employee unpaid = employee("EMP-00100", "Zero Pay", "Intern", "Engineering", Country.IN, "0");

		assertThatThrownBy(() -> repository.saveAndFlush(unpaid))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void reportsWhetherAnEmployeeCodeIsTaken() {
		assertThat(repository.existsByEmployeeCode("EMP-00001")).isTrue();
		assertThat(repository.existsByEmployeeCode("EMP-99999")).isFalse();
	}

	@Test
	void emptyFilterReturnsEveryone() {
		assertThat(repository.findAll(EmployeeFilter.none().toSpecification())).hasSize(5);
	}

	@Test
	void filtersByCountry() {
		List<Employee> result = repository.findAll(new EmployeeFilter(null, Country.US, null, null).toSpecification());

		assertThat(result).extracting(Employee::getEmployeeCode).containsExactlyInAnyOrder("EMP-00004", "EMP-00005");
	}

	@Test
	void combinesCountryDepartmentAndJobTitle() {
		List<Employee> result = repository.findAll(
				new EmployeeFilter(null, Country.IN, "Engineering", "Software Engineer").toSpecification());

		assertThat(result).extracting(Employee::getEmployeeCode).containsExactlyInAnyOrder("EMP-00001", "EMP-00002");
	}

	@Test
	void searchMatchesNameCaseInsensitively() {
		List<Employee> result = repository.findAll(new EmployeeFilter("  pRiYa ", null, null, null).toSpecification());

		assertThat(result).extracting(Employee::getFullName).containsExactly("Priya Nair");
	}

	@Test
	void searchMatchesEmployeeCode() {
		List<Employee> result = repository.findAll(new EmployeeFilter("emp-00004", null, null, null).toSpecification());

		assertThat(result).extracting(Employee::getFullName).containsExactly("John Carter");
	}

	@Test
	void blankFilterValuesAreIgnored() {
		List<Employee> result = repository.findAll(new EmployeeFilter(" ", null, "", "").toSpecification());

		assertThat(result).hasSize(5);
	}

	@Test
	void pagesAndSortsBySalaryDescending() {
		Page<Employee> page = repository.findAll(
				new EmployeeFilter(null, Country.IN, null, null).toSpecification(),
				PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "annualSalary")));

		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).extracting(Employee::getEmployeeCode).containsExactly("EMP-00002", "EMP-00001");
	}

	@Test
	void listsDistinctDepartmentsAndJobTitlesAlphabetically() {
		assertThat(repository.findDistinctDepartments()).containsExactly("Engineering", "People", "Product");
		assertThat(repository.findDistinctJobTitles())
				.containsExactly("HR Generalist", "Product Manager", "Software Engineer");
	}

	@Test
	void updatingDetailsRefreshesUpdatedAtButKeepsCreatedAt() throws InterruptedException {
		Employee employee = repository.findAll(new EmployeeFilter("EMP-00003", null, null, null).toSpecification())
				.getFirst();
		var createdAt = employee.getCreatedAt();
		Thread.sleep(5); // timestamps must differ; Instant has sub-millisecond precision on modern JDKs

		employee.changeDetails("Priya Nair", "HR Manager", "People", Country.IN, new BigDecimal("1300000"),
				EmploymentType.FULL_TIME, employee.getHireDate());
		repository.saveAndFlush(employee);
		entityManager.clear();

		Employee reloaded = repository.findById(employee.getId()).orElseThrow();
		assertThat(reloaded.getJobTitle()).isEqualTo("HR Manager");
		assertThat(reloaded.getAnnualSalary()).isEqualByComparingTo("1300000");
		assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
		assertThat(reloaded.getUpdatedAt()).isAfter(createdAt);
	}

	private static Employee employee(String code, String name, String title, String department, Country country,
			String salary) {
		return new Employee(code, name, title, department, country, new BigDecimal(salary), EmploymentType.FULL_TIME,
				LocalDate.of(2022, 4, 1));
	}

}
