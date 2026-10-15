package com.acme.salary.seed;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmploymentType;
import com.acme.salary.insights.SalaryStatistics;

class EmployeeGeneratorTest {

	private static List<Employee> employees;

	@BeforeAll
	static void generateOnce() {
		employees = new EmployeeGenerator(42).generate(10_000);
	}

	@Test
	void generatesExactlyTheRequestedNumberWithUniqueSequentialCodes() {
		assertThat(employees).hasSize(10_000);
		assertThat(employees.getFirst().getEmployeeCode()).isEqualTo("EMP-00001");
		assertThat(employees.getLast().getEmployeeCode()).isEqualTo("EMP-10000");
		assertThat(employees).extracting(Employee::getEmployeeCode).doesNotHaveDuplicates();
	}

	@Test
	void sameSeedProducesIdenticalEmployees() {
		List<Employee> again = new EmployeeGenerator(42).generate(10_000);

		assertThat(again).extracting(EmployeeGeneratorTest::fingerprint)
				.containsExactlyElementsOf(employees.stream().map(EmployeeGeneratorTest::fingerprint).toList());
	}

	@Test
	void differentSeedProducesDifferentEmployees() {
		List<Employee> other = new EmployeeGenerator(7).generate(100);

		assertThat(other.stream().map(EmployeeGeneratorTest::fingerprint).toList())
				.isNotEqualTo(employees.subList(0, 100).stream().map(EmployeeGeneratorTest::fingerprint).toList());
	}

	@Test
	void everyEmployeeIsValid() {
		assertThat(employees).allSatisfy(employee -> {
			assertThat(employee.getFullName()).isNotBlank().hasSizeLessThanOrEqualTo(120);
			assertThat(employee.getJobTitle()).isNotBlank();
			assertThat(employee.getDepartment()).isNotBlank();
			assertThat(employee.getAnnualSalary()).isPositive();
			assertThat(employee.getAnnualSalary().scale()).isEqualTo(2);
			assertThat(employee.getHireDate()).isBetween(EmployeeGenerator.EARLIEST_HIRE, EmployeeGenerator.LATEST_HIRE);
		});
	}

	@Test
	void countriesFollowTheConfiguredMixWithinTwoPercentagePoints() {
		Map<Country, Long> perCountry = employees.stream().collect(groupingBy(Employee::getCountry, counting()));

		assertThat(perCountry).containsOnlyKeys(Country.values());
		assertThat(share(perCountry.get(Country.IN))).isCloseTo(40.0, org.assertj.core.data.Offset.offset(2.0));
		assertThat(share(perCountry.get(Country.US))).isCloseTo(25.0, org.assertj.core.data.Offset.offset(2.0));
		assertThat(share(perCountry.get(Country.AE))).isCloseTo(7.0, org.assertj.core.data.Offset.offset(2.0));
	}

	@Test
	void producesTheExpectedCatalogueOfDepartmentsAndTitles() {
		assertThat(employees).extracting(Employee::getDepartment).containsOnly(
				"Engineering", "Data", "Product", "People", "Finance", "Sales", "Customer Success", "Operations");
		assertThat(employees.stream().map(Employee::getJobTitle).distinct()).hasSize(20);
	}

	@Test
	void usSoftwareEngineerSalariesLookLikeTheMarket() {
		SalaryStatistics stats = fullTimeStats(Country.US, "Software Engineer");

		assertThat(stats.median()).isBetween(new BigDecimal("135000"), new BigDecimal("165000"));
		assertThat(stats.min()).isGreaterThan(new BigDecimal("110000"));
		assertThat(stats.max()).isLessThan(new BigDecimal("200000"));
	}

	@Test
	void seniorRolesArePaidMoreThanJuniorOnesInEveryCountry() {
		for (Country country : Country.values()) {
			BigDecimal engineer = fullTimeStats(country, "Software Engineer").median();
			BigDecimal senior = fullTimeStats(country, "Senior Software Engineer").median();
			BigDecimal staff = fullTimeStats(country, "Staff Engineer").median();

			assertThat(senior).as("%s senior vs engineer", country).isGreaterThan(engineer);
			assertThat(staff).as("%s staff vs senior", country).isGreaterThan(senior);
		}
	}

	@Test
	void partTimersEarnRoughlyHalfOfFullTimersInTheSameRole() {
		Map<EmploymentType, List<BigDecimal>> byType = employees.stream()
				.filter(e -> e.getCountry() == Country.IN && e.getJobTitle().equals("Customer Support Specialist"))
				.collect(groupingBy(Employee::getEmploymentType, mapping(Employee::getAnnualSalary, toList())));

		BigDecimal fullTime = SalaryStatistics.of(byType.get(EmploymentType.FULL_TIME)).median();
		BigDecimal partTime = SalaryStatistics.of(byType.get(EmploymentType.PART_TIME)).median();
		double ratio = partTime.doubleValue() / fullTime.doubleValue();

		assertThat(ratio).isBetween(0.4, 0.6);
	}

	private static SalaryStatistics fullTimeStats(Country country, String title) {
		return SalaryStatistics.of(employees.stream()
				.filter(e -> e.getCountry() == country && e.getJobTitle().equals(title)
						&& e.getEmploymentType() == EmploymentType.FULL_TIME)
				.map(Employee::getAnnualSalary)
				.toList());
	}

	private static double share(long count) {
		return count * 100.0 / employees.size();
	}

	private static String fingerprint(Employee e) {
		return String.join("|", e.getEmployeeCode(), e.getFullName(), e.getJobTitle(), e.getCountry().name(),
				e.getAnnualSalary().toPlainString(), e.getEmploymentType().name(), e.getHireDate().toString());
	}

}
