package com.acme.salary.insights;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.salary.employee.Country;
import com.acme.salary.insights.SalaryInsightRepository.GroupSalary;

/**
 * Answers "how does the org pay people?". All maths goes through {@link SalaryStatistics} so every
 * figure (median included) is computed one way. At 10k employees this is a single in-memory pass;
 * at much larger scale the aggregation would move into the database.
 */
@Service
@Transactional(readOnly = true)
public class InsightService {

	private final SalaryInsightRepository repository;

	public InsightService(SalaryInsightRepository repository) {
		this.repository = repository;
	}

	/** One entry per country that has employees, in {@link Country} declaration order. */
	public List<CountryInsight> byCountry() {
		Map<Country, List<BigDecimal>> salariesByCountry = new EnumMap<>(Country.class);
		for (var row : repository.findAllCountrySalaries()) {
			salariesByCountry.computeIfAbsent(row.country(), c -> new ArrayList<>()).add(row.salary());
		}
		return salariesByCountry.entrySet().stream()
				.map(entry -> CountryInsight.of(entry.getKey(), SalaryStatistics.of(entry.getValue())))
				.toList();
	}

	/** Job titles in a country, alphabetical. Empty when the country has no employees. */
	public CountryGroupInsights byJobTitle(Country country) {
		return CountryGroupInsights.of(country, group(repository.findJobTitleSalaries(country)));
	}

	/** Departments in a country, alphabetical. Empty when the country has no employees. */
	public CountryGroupInsights byDepartment(Country country) {
		return CountryGroupInsights.of(country, group(repository.findDepartmentSalaries(country)));
	}

	private static List<GroupInsight> group(List<GroupSalary> rows) {
		Map<String, List<BigDecimal>> salariesByGroup = new TreeMap<>();
		for (GroupSalary row : rows) {
			salariesByGroup.computeIfAbsent(row.group(), g -> new ArrayList<>()).add(row.salary());
		}
		return salariesByGroup.entrySet().stream()
				.map(entry -> GroupInsight.of(entry.getKey(), SalaryStatistics.of(entry.getValue())))
				.toList();
	}

}
