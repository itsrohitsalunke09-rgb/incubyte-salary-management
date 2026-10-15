package com.acme.salary.insights;

import java.util.List;

import com.acme.salary.employee.Country;

/** Breakdown of one country by job title or department. Amounts are in {@code currency}. */
public record CountryGroupInsights(Country country, String countryName, String currency, List<GroupInsight> groups) {

	static CountryGroupInsights of(Country country, List<GroupInsight> groups) {
		return new CountryGroupInsights(country, country.displayName(), country.currency().getCurrencyCode(), groups);
	}

}
