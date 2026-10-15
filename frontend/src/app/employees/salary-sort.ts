import { CountryCode, EmployeeQuery } from '../core/api/models';

export type EmployeeSort = NonNullable<EmployeeQuery['sort']>;

export const DEFAULT_SORT: EmployeeSort = { field: 'fullName', direction: 'asc' };

/**
 * Salaries are stored in each country's local currency, so ordering by salary across countries
 * would rank 1,800,000 INR above 150,000 USD. Sorting by salary is therefore only meaningful
 * (and only allowed) within a single country. Currency conversion is out of scope for v1.
 */
export function canSortBySalary(country: CountryCode | null | undefined): boolean {
  return !!country;
}

/** The sort to actually send: a salary sort without a country filter falls back to the default. */
export function effectiveSort(sort: EmployeeSort, country: CountryCode | null | undefined): EmployeeSort {
  return sort.field === 'annualSalary' && !canSortBySalary(country) ? DEFAULT_SORT : sort;
}
