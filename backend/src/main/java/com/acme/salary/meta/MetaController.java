package com.acme.salary.meta;

import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.EmployeeRepository;

/** Reference data for the UI's dropdowns. */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

	private final EmployeeRepository repository;

	public MetaController(EmployeeRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/filters")
	@Transactional(readOnly = true)
	public FilterOptions filters() {
		List<CountryOption> countries = Arrays.stream(Country.values())
				.map(c -> new CountryOption(c, c.displayName(), c.currency().getCurrencyCode()))
				.toList();
		return new FilterOptions(countries, repository.findDistinctDepartments(), repository.findDistinctJobTitles());
	}

	public record FilterOptions(List<CountryOption> countries, List<String> departments, List<String> jobTitles) {
	}

	public record CountryOption(Country code, String name, String currency) {
	}

}
