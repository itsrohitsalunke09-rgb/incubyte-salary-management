package com.acme.salary.insights;

import java.math.BigDecimal;
import java.util.Collection;

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

	public static SalaryStatistics of(Collection<BigDecimal> salaries) {
		throw new UnsupportedOperationException("not implemented yet");
	}

}
