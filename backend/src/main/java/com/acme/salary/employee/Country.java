package com.acme.salary.employee;

import java.util.Currency;

/**
 * Countries ACME employs people in. The currency is derived from the country rather than
 * stored per employee, so a salary can never be recorded in the wrong currency for its country.
 * Adding a country is a code change on purpose: it implies new payroll rules for HR.
 */
public enum Country {

	IN("India", "INR"),
	US("United States", "USD"),
	GB("United Kingdom", "GBP"),
	DE("Germany", "EUR"),
	SG("Singapore", "SGD"),
	AE("United Arab Emirates", "AED");

	private final String displayName;
	private final Currency currency;

	Country(String displayName, String currencyCode) {
		this.displayName = displayName;
		this.currency = Currency.getInstance(currencyCode);
	}

	public String displayName() {
		return displayName;
	}

	public Currency currency() {
		return currency;
	}

}
