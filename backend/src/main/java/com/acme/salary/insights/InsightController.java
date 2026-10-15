package com.acme.salary.insights;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acme.salary.employee.Country;

@RestController
@RequestMapping("/api/insights/countries")
public class InsightController {

	private final InsightService service;

	public InsightController(InsightService service) {
		this.service = service;
	}

	@GetMapping
	public List<CountryInsight> countries() {
		return service.byCountry();
	}

	@GetMapping("/{country}/job-titles")
	public CountryGroupInsights jobTitles(@PathVariable Country country) {
		return service.byJobTitle(country);
	}

	@GetMapping("/{country}/departments")
	public CountryGroupInsights departments(@PathVariable Country country) {
		return service.byDepartment(country);
	}

}
