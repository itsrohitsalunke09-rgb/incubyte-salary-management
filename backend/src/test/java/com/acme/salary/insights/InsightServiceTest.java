package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acme.salary.employee.Country;
import com.acme.salary.insights.SalaryInsightRepository.CountrySalary;
import com.acme.salary.insights.SalaryInsightRepository.GroupSalary;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

	@Mock
	private SalaryInsightRepository repository;

	@InjectMocks
	private InsightService service;

	@Test
	void groupsSalariesByCountryWithLocalCurrency() {
		when(repository.findAllCountrySalaries()).thenReturn(List.of(
				new CountrySalary(Country.US, money("150000")),
				new CountrySalary(Country.IN, money("1000000")),
				new CountrySalary(Country.IN, money("3000000")),
				new CountrySalary(Country.IN, money("2000000"))));

		List<CountryInsight> insights = service.byCountry();

		// Country declaration order (IN before US), not input order
		assertThat(insights).extracting(CountryInsight::country).containsExactly(Country.IN, Country.US);

		CountryInsight india = insights.getFirst();
		assertThat(india.currency()).isEqualTo("INR");
		assertThat(india.headcount()).isEqualTo(3);
		assertThat(india.min()).isEqualByComparingTo("1000000");
		assertThat(india.max()).isEqualByComparingTo("3000000");
		assertThat(india.median()).isEqualByComparingTo("2000000");
		assertThat(india.totalPayroll()).isEqualByComparingTo("6000000");

		assertThat(insights.get(1).currency()).isEqualTo("USD");
		assertThat(insights.get(1).headcount()).isEqualTo(1);
	}

	@Test
	void countriesWithoutEmployeesAreLeftOut() {
		when(repository.findAllCountrySalaries()).thenReturn(List.of(new CountrySalary(Country.DE, money("70000"))));

		assertThat(service.byCountry()).extracting(CountryInsight::country).containsExactly(Country.DE);
	}

	@Test
	void noEmployeesMeansNoInsightsRatherThanAnError() {
		when(repository.findAllCountrySalaries()).thenReturn(List.of());

		assertThat(service.byCountry()).isEmpty();
	}

	@Test
	void breaksACountryDownByJobTitleAlphabetically() {
		when(repository.findJobTitleSalaries(Country.IN)).thenReturn(List.of(
				new GroupSalary("Software Engineer", money("1800000")),
				new GroupSalary("HR Generalist", money("900000")),
				new GroupSalary("Software Engineer", money("2400000"))));

		CountryGroupInsights result = service.byJobTitle(Country.IN);

		assertThat(result.country()).isEqualTo(Country.IN);
		assertThat(result.currency()).isEqualTo("INR");
		assertThat(result.groups()).extracting(GroupInsight::name).containsExactly("HR Generalist", "Software Engineer");
		GroupInsight engineers = result.groups().get(1);
		assertThat(engineers.headcount()).isEqualTo(2);
		assertThat(engineers.median()).isEqualByComparingTo("2100000");
		assertThat(engineers.average()).isEqualByComparingTo("2100000");
	}

	@Test
	void breaksACountryDownByDepartment() {
		when(repository.findDepartmentSalaries(Country.US)).thenReturn(List.of(
				new GroupSalary("Product", money("160000")),
				new GroupSalary("Engineering", money("145000")),
				new GroupSalary("Engineering", money("155000"))));

		CountryGroupInsights result = service.byDepartment(Country.US);

		assertThat(result.groups()).extracting(GroupInsight::name).containsExactly("Engineering", "Product");
		assertThat(result.groups().getFirst().totalPayroll()).isEqualByComparingTo("300000");
	}

	@Test
	void aCountryWithNoEmployeesHasAnEmptyBreakdown() {
		when(repository.findJobTitleSalaries(Country.AE)).thenReturn(List.of());

		CountryGroupInsights result = service.byJobTitle(Country.AE);

		assertThat(result.currency()).isEqualTo("AED");
		assertThat(result.groups()).isEmpty();
	}

	private static BigDecimal money(String amount) {
		return new BigDecimal(amount);
	}

}
