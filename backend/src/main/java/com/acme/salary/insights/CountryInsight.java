package com.acme.salary.insights;

import java.math.BigDecimal;

import com.acme.salary.employee.Country;

/** Salary picture for one country. Amounts are in that country's currency. */
public record CountryInsight(
		Country country,
		String countryName,
		String currency,
		long headcount,
		BigDecimal min,
		BigDecimal max,
		BigDecimal average,
		BigDecimal median,
		BigDecimal totalPayroll) {

	static CountryInsight of(Country country, SalaryStatistics stats) {
		return new CountryInsight(country, country.displayName(), country.currency().getCurrencyCode(),
				stats.headcount(), stats.min(), stats.max(), stats.average(), stats.median(), stats.total());
	}

}
