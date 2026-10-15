package com.acme.salary.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmploymentType;

/**
 * Generates realistic, fully deterministic employees: the same seed always yields exactly the same
 * people, so every reviewer sees the same data and tests can rely on it.
 *
 * <p>Salary = country market rate for a mid-level software engineer x role factor x seniority-in-company
 * bump x random spread (+/-15%), halved for part-timers, rounded to 100. That keeps salaries comparable
 * within a country and role, which is exactly what the insights screens are for.
 */
public final class EmployeeGenerator {

	/** Hire dates are generated in a fixed window so output never depends on "today". */
	static final LocalDate EARLIEST_HIRE = LocalDate.of(2012, 1, 1);
	static final LocalDate LATEST_HIRE = LocalDate.of(2026, 6, 30);

	/** Annual market rate for a mid-level Software Engineer, in local currency. */
	private static final Map<Country, Integer> MARKET_RATE = new EnumMap<>(Map.of(
			Country.IN, 1_800_000,
			Country.US, 140_000,
			Country.GB, 70_000,
			Country.DE, 75_000,
			Country.SG, 110_000,
			Country.AE, 300_000));

	private static final List<Weighted<Country>> COUNTRIES = List.of(
			new Weighted<>(Country.IN, 40),
			new Weighted<>(Country.US, 25),
			new Weighted<>(Country.GB, 10),
			new Weighted<>(Country.DE, 10),
			new Weighted<>(Country.SG, 8),
			new Weighted<>(Country.AE, 7));

	private static final List<Weighted<Role>> ROLES = List.of(
			new Weighted<>(new Role("Software Engineer", "Engineering", 1.00), 18),
			new Weighted<>(new Role("Senior Software Engineer", "Engineering", 1.40), 12),
			new Weighted<>(new Role("Staff Engineer", "Engineering", 1.85), 3),
			new Weighted<>(new Role("Engineering Manager", "Engineering", 1.90), 3),
			new Weighted<>(new Role("QA Engineer", "Engineering", 0.80), 6),
			new Weighted<>(new Role("DevOps Engineer", "Engineering", 1.05), 4),
			new Weighted<>(new Role("Data Analyst", "Data", 0.80), 5),
			new Weighted<>(new Role("Data Scientist", "Data", 1.20), 3),
			new Weighted<>(new Role("Product Manager", "Product", 1.30), 4),
			new Weighted<>(new Role("Product Designer", "Product", 1.00), 3),
			new Weighted<>(new Role("HR Generalist", "People", 0.60), 3),
			new Weighted<>(new Role("HR Manager", "People", 0.95), 1),
			new Weighted<>(new Role("Recruiter", "People", 0.65), 2),
			new Weighted<>(new Role("Accountant", "Finance", 0.70), 3),
			new Weighted<>(new Role("Finance Manager", "Finance", 1.10), 1),
			new Weighted<>(new Role("Sales Executive", "Sales", 0.70), 8),
			new Weighted<>(new Role("Account Manager", "Sales", 0.90), 5),
			new Weighted<>(new Role("Sales Manager", "Sales", 1.20), 2),
			new Weighted<>(new Role("Customer Support Specialist", "Customer Success", 0.45), 10),
			new Weighted<>(new Role("Operations Manager", "Operations", 1.00), 4));

	private static final List<Weighted<EmploymentType>> EMPLOYMENT_TYPES = List.of(
			new Weighted<>(EmploymentType.FULL_TIME, 85),
			new Weighted<>(EmploymentType.CONTRACT, 8),
			new Weighted<>(EmploymentType.PART_TIME, 7));

	private static final List<String> FIRST_NAMES = List.of(
			"Aarav", "Ananya", "Rohan", "Priya", "Vikram", "Sneha", "Arjun", "Kavya", "James", "Olivia",
			"Michael", "Emma", "David", "Sophia", "Daniel", "Grace", "Oliver", "Amelia", "Harry", "Isla",
			"Lukas", "Mia", "Felix", "Hannah", "Wei", "Mei", "Jun", "Hui", "Omar", "Fatima",
			"Ahmed", "Layla", "Carlos", "Lucia", "Kenji", "Yuki", "Noah", "Ava", "Liam", "Zara");

	private static final List<String> LAST_NAMES = List.of(
			"Sharma", "Patel", "Iyer", "Reddy", "Nair", "Gupta", "Salunke", "Kulkarni", "Smith", "Johnson",
			"Williams", "Brown", "Jones", "Taylor", "Davies", "Evans", "Muller", "Schmidt", "Fischer", "Weber",
			"Tan", "Lim", "Ng", "Wong", "Khan", "Hassan", "Rahman", "Ali", "Garcia", "Martinez",
			"Lopez", "Silva", "Sato", "Suzuki", "Kim", "Park", "Chen", "Wang", "OConnor", "Murphy");

	private final Random random;

	public EmployeeGenerator(long seed) {
		this.random = new Random(seed);
	}

	public List<Employee> generate(int count) {
		List<Employee> employees = new ArrayList<>(count);
		for (int i = 1; i <= count; i++) {
			employees.add(next(i));
		}
		return employees;
	}

	private Employee next(int sequence) {
		Country country = pickWeighted(COUNTRIES);
		Role role = pickWeighted(ROLES);
		EmploymentType employmentType = pickWeighted(EMPLOYMENT_TYPES);
		LocalDate hireDate = EARLIEST_HIRE.plusDays(
				random.nextLong(ChronoUnit.DAYS.between(EARLIEST_HIRE, LATEST_HIRE) + 1));
		String name = pick(FIRST_NAMES) + " " + pick(LAST_NAMES);

		return new Employee("EMP-%05d".formatted(sequence), name, role.title(), role.department(), country,
				salary(country, role, employmentType, hireDate), employmentType, hireDate);
	}

	private BigDecimal salary(Country country, Role role, EmploymentType employmentType, LocalDate hireDate) {
		double yearsInCompany = ChronoUnit.DAYS.between(hireDate, LATEST_HIRE) / 365.0;
		double tenureBump = 1 + Math.min(yearsInCompany, 10) * 0.02; // up to +20% after 10 years
		double spread = 0.85 + random.nextDouble() * 0.30; // +/-15%
		double partTime = employmentType == EmploymentType.PART_TIME ? 0.5 : 1.0;

		double amount = MARKET_RATE.get(country) * role.factor() * tenureBump * spread * partTime;
		return BigDecimal.valueOf(amount).setScale(-2, RoundingMode.HALF_UP).setScale(2);
	}

	private <T> T pick(List<T> values) {
		return values.get(random.nextInt(values.size()));
	}

	private <T> T pickWeighted(List<Weighted<T>> weighted) {
		int total = weighted.stream().mapToInt(Weighted::weight).sum();
		int roll = random.nextInt(total);
		for (Weighted<T> option : weighted) {
			roll -= option.weight();
			if (roll < 0) {
				return option.value();
			}
		}
		throw new IllegalStateException("unreachable");
	}

	record Role(String title, String department, double factor) {
	}

	private record Weighted<T>(T value, int weight) {
	}

}
