package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmploymentType;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseSeederTest {

	@Autowired
	private EmployeeRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private DatabaseSeeder seeder;

	@BeforeEach
	void setUp() {
		repository.deleteAll(); // rolled back with the test transaction
		seeder = new DatabaseSeeder(repository, entityManager, new TransactionTemplate(transactionManager), 1_200, 42);
	}

	@Test
	void seedsAnEmptyDatabaseInBatches() {
		int inserted = seeder.seedIfEmpty();

		assertThat(inserted).isEqualTo(1_200);
		assertThat(repository.count()).isEqualTo(1_200);
		assertThat(repository.existsByEmployeeCode("EMP-01200")).isTrue();
	}

	@Test
	void neverTouchesADatabaseThatAlreadyHasEmployees() {
		repository.saveAndFlush(new Employee("HR-1", "Entered By HR", "Recruiter", "People", Country.GB,
				new BigDecimal("40000"), EmploymentType.FULL_TIME, LocalDate.of(2024, 3, 1)));

		int inserted = seeder.seedIfEmpty();

		assertThat(inserted).isZero();
		assertThat(repository.count()).isEqualTo(1);
	}

}
