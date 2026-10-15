package com.acme.salary.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;

import jakarta.persistence.EntityManager;

/**
 * Loads the deterministic demo dataset on startup, but only into an empty database, so restarts
 * never duplicate or overwrite data HR has entered. Disable with {@code salary.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "salary.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
	private static final int BATCH_SIZE = 500;

	private final EmployeeRepository repository;
	private final EntityManager entityManager;
	private final TransactionTemplate transaction;
	private final int count;
	private final long randomSeed;

	public DatabaseSeeder(EmployeeRepository repository, EntityManager entityManager, TransactionTemplate transaction,
			@Value("${salary.seed.count:10000}") int count, @Value("${salary.seed.random-seed:42}") long randomSeed) {
		this.repository = repository;
		this.entityManager = entityManager;
		this.transaction = transaction;
		this.count = count;
		this.randomSeed = randomSeed;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedIfEmpty();
	}

	/** @return how many employees were inserted (0 when the database already had data) */
	public int seedIfEmpty() {
		if (repository.count() > 0) {
			log.info("Database already has employees; skipping seed");
			return 0;
		}
		long started = System.nanoTime();
		List<Employee> employees = new EmployeeGenerator(randomSeed).generate(count);
		// One transaction for all rows: SQLite commits are the expensive part, not the inserts.
		transaction.executeWithoutResult(status -> {
			for (int from = 0; from < employees.size(); from += BATCH_SIZE) {
				repository.saveAll(employees.subList(from, Math.min(from + BATCH_SIZE, employees.size())));
				entityManager.flush();
				entityManager.clear(); // keep the persistence context small
			}
		});
		log.info("Seeded {} employees in {} ms", employees.size(), (System.nanoTime() - started) / 1_000_000);
		return employees.size();
	}

}
