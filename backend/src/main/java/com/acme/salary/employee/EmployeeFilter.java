package com.acme.salary.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * Optional criteria for the employee list. Blank values mean "no filter".
 *
 * @param search matches employee code or full name, case-insensitive, anywhere in the text
 */
public record EmployeeFilter(String search, Country country, String department, String jobTitle) {

	public static EmployeeFilter none() {
		return new EmployeeFilter(null, null, null, null);
	}

	public Specification<Employee> toSpecification() {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(search)) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("fullName")), pattern),
						cb.like(cb.lower(root.get("employeeCode")), pattern)));
			}
			if (country != null) {
				predicates.add(cb.equal(root.get("country"), country));
			}
			if (StringUtils.hasText(department)) {
				predicates.add(cb.equal(root.get("department"), department));
			}
			if (StringUtils.hasText(jobTitle)) {
				predicates.add(cb.equal(root.get("jobTitle"), jobTitle));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
