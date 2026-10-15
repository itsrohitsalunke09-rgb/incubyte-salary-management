package com.acme.salary.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Summary statistics for a group of annual salaries, all in the same currency.
 * Average and median are rounded to 2 decimal places, half-up.
 */
public record SalaryStatistics(
		long headcount,
		BigDecimal min,
		BigDecimal max,
		BigDecimal average,
		BigDecimal median,
		BigDecimal total) {

	private static final int SCALE = 2;
	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	public static SalaryStatistics of(Collection<BigDecimal> salaries) {
		if (salaries == null || salaries.isEmpty()) {
			throw new IllegalArgumentException("Statistics need at least one salary");
		}
		if (salaries.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("Salaries must not contain null values");
		}

		List<BigDecimal> sorted = salaries.stream().sorted().toList();
		int count = sorted.size();
		BigDecimal total = sorted.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal average = total.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);

		return new SalaryStatistics(
				count,
				sorted.getFirst(),
				sorted.getLast(),
				average,
				median(sorted),
				total);
	}

	private static BigDecimal median(List<BigDecimal> sorted) {
		int middle = sorted.size() / 2;
		BigDecimal median = sorted.size() % 2 == 1
				? sorted.get(middle)
				: sorted.get(middle - 1).add(sorted.get(middle)).divide(TWO);
		return median.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
