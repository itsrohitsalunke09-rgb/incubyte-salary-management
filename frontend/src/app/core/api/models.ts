// Mirrors the backend contract (see docs/DESIGN.md §3). Money arrives as JSON numbers in the
// country's local currency; the UI only displays it, it never does salary arithmetic.

export type CountryCode = 'IN' | 'US' | 'GB' | 'DE' | 'SG' | 'AE';
export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT';

export const EMPLOYMENT_TYPES: readonly EmploymentType[] = ['FULL_TIME', 'PART_TIME', 'CONTRACT'];

export interface Employee {
  id: number;
  employeeCode: string;
  fullName: string;
  jobTitle: string;
  department: string;
  country: CountryCode;
  countryName: string;
  currency: string;
  annualSalary: number;
  employmentType: EmploymentType;
  hireDate: string; // ISO date, yyyy-MM-dd
  updatedAt: string;
}

export interface UpdateEmployeeRequest {
  fullName: string;
  jobTitle: string;
  department: string;
  country: CountryCode;
  annualSalary: number;
  employmentType: EmploymentType;
  hireDate: string;
}

/** The employee code is only sent on create; it is immutable afterwards. */
export interface CreateEmployeeRequest extends UpdateEmployeeRequest {
  employeeCode: string;
}

export type SortableField =
  | 'fullName'
  | 'employeeCode'
  | 'jobTitle'
  | 'department'
  | 'country'
  | 'annualSalary'
  | 'hireDate';

export interface EmployeeQuery {
  search?: string;
  country?: CountryCode | null;
  department?: string | null;
  jobTitle?: string | null;
  page: number;
  size: number;
  sort?: { field: SortableField; direction: 'asc' | 'desc' };
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface CountryOption {
  code: CountryCode;
  name: string;
  currency: string;
}

export interface FilterOptions {
  countries: CountryOption[];
  departments: string[];
  jobTitles: string[];
}

export interface SalaryFigures {
  headcount: number;
  min: number;
  max: number;
  average: number;
  median: number;
  totalPayroll: number;
}

export interface CountryInsight extends SalaryFigures {
  country: CountryCode;
  countryName: string;
  currency: string;
}

export interface GroupInsight extends SalaryFigures {
  name: string;
}

export interface CountryGroupInsights {
  country: CountryCode;
  countryName: string;
  currency: string;
  groups: GroupInsight[];
}
