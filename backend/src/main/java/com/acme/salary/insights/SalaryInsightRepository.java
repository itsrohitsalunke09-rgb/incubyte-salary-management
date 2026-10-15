package com.acme.salary.insights;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.Employee;

/**
 * Read-only projections for insights: just (group, salary) pairs, never whole entities.
 * Each query is one indexed scan; the statistics are computed by {@link SalaryStatistics}.
 */
public interface SalaryInsightRepository extends Repository<Employee, Long> {

	@Query("select new com.acme.salary.insights.SalaryInsightRepository$CountrySalary(e.country, e.annualSalary) "
			+ "from Employee e")
	List<CountrySalary> findAllCountrySalaries();

	@Query("select new com.acme.salary.insights.SalaryInsightRepository$GroupSalary(e.jobTitle, e.annualSalary) "
			+ "from Employee e where e.country = :country")
	List<GroupSalary> findJobTitleSalaries(@Param("country") Country country);

	@Query("select new com.acme.salary.insights.SalaryInsightRepository$GroupSalary(e.department, e.annualSalary) "
			+ "from Employee e where e.country = :country")
	List<GroupSalary> findDepartmentSalaries(@Param("country") Country country);

	record CountrySalary(Country country, BigDecimal salary) {
	}

	record GroupSalary(String group, BigDecimal salary) {
	}

}
