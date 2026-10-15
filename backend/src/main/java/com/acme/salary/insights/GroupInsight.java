package com.acme.salary.insights;

import java.math.BigDecimal;

/** Salary picture for one job title or department within a country. */
public record GroupInsight(
		String name,
		long headcount,
		BigDecimal min,
		BigDecimal max,
		BigDecimal average,
		BigDecimal median,
		BigDecimal totalPayroll) {

	static GroupInsight of(String name, SalaryStatistics stats) {
		return new GroupInsight(name, stats.headcount(), stats.min(), stats.max(), stats.average(), stats.median(),
				stats.total());
	}

}
