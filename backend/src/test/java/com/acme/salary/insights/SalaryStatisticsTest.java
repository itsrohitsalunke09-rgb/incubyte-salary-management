package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class SalaryStatisticsTest {

	@Test
	void singleSalaryIsItsOwnMinMaxAverageMedianAndTotal() {
		SalaryStatistics stats = SalaryStatistics.of(salaries("50000"));

		assertThat(stats.headcount()).isEqualTo(1);
		assertThat(stats.min()).isEqualByComparingTo("50000");
		assertThat(stats.max()).isEqualByComparingTo("50000");
		assertThat(stats.average()).isEqualByComparingTo("50000");
		assertThat(stats.median()).isEqualByComparingTo("50000");
		assertThat(stats.total()).isEqualByComparingTo("50000");
	}

	@Test
	void medianOfOddCountIsTheMiddleValueRegardlessOfInputOrder() {
		SalaryStatistics stats = SalaryStatistics.of(salaries("90000", "30000", "60000"));

		assertThat(stats.median()).isEqualByComparingTo("60000");
	}

	@Test
	void medianOfEvenCountIsTheMeanOfTheTwoMiddleValues() {
		SalaryStatistics stats = SalaryStatistics.of(salaries("40000", "10000", "30000", "20000"));

		assertThat(stats.median()).isEqualByComparingTo("25000");
	}

	@Test
	void computesHeadcountMinMaxAndTotal() {
		SalaryStatistics stats = SalaryStatistics.of(salaries("70000", "120000", "45000.50"));

		assertThat(stats.headcount()).isEqualTo(3);
		assertThat(stats.min()).isEqualByComparingTo("45000.50");
		assertThat(stats.max()).isEqualByComparingTo("120000");
		assertThat(stats.total()).isEqualByComparingTo("235000.50");
	}

	@Test
	void averageAndMedianAreRoundedToTwoDecimalsHalfUp() {
		SalaryStatistics stats = SalaryStatistics.of(salaries("10000", "10000", "10001"));

		// 30001 / 3 = 10000.333...
		assertThat(stats.average()).isEqualByComparingTo("10000.33");
		assertThat(stats.average().scale()).isEqualTo(2);

		SalaryStatistics evenStats = SalaryStatistics.of(salaries("10000.01", "10000.02"));
		// (10000.01 + 10000.02) / 2 = 10000.015 -> 10000.02
		assertThat(evenStats.median()).isEqualByComparingTo("10000.02");
	}

	@Test
	void doesNotModifyTheCallersCollection() {
		List<BigDecimal> input = salaries("3", "1", "2");

		SalaryStatistics.of(input);

		assertThat(input).containsExactly(new BigDecimal("3"), new BigDecimal("1"), new BigDecimal("2"));
	}

	@Test
	void rejectsAnEmptyGroup() {
		assertThatThrownBy(() -> SalaryStatistics.of(List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one salary");
	}

	@Test
	void rejectsNullSalaries() {
		List<BigDecimal> withNull = new ArrayList<>(salaries("1000"));
		withNull.add(null);

		assertThatThrownBy(() -> SalaryStatistics.of(withNull))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static List<BigDecimal> salaries(String... values) {
		return new ArrayList<>(Arrays.stream(values).map(BigDecimal::new).toList());
	}

}
