import { HttpErrorResponse } from '@angular/common/http';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSortHarness } from '@angular/material/sort/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { EmployeeApi } from '../../core/api/employee-api';
import { MetaApi } from '../../core/api/meta-api';
import { Employee, EmployeeQuery, FilterOptions, Page } from '../../core/api/models';
import { EmployeeList } from './employee-list';

// The HTTP mapping is covered by employee-api.spec.ts. Here a fake API answers immediately and
// records every query, so the component's behaviour can be tested through real Material components.

const FILTERS: FilterOptions = {
  countries: [
    { code: 'IN', name: 'India', currency: 'INR' },
    { code: 'US', name: 'United States', currency: 'USD' },
  ],
  departments: ['Engineering', 'People'],
  jobTitles: ['HR Generalist', 'Software Engineer'],
};

function employee(id: number, overrides: Partial<Employee> = {}): Employee {
  return {
    id,
    employeeCode: `EMP-${String(id).padStart(5, '0')}`,
    fullName: `Person ${id}`,
    jobTitle: 'Software Engineer',
    department: 'Engineering',
    country: 'IN',
    countryName: 'India',
    currency: 'INR',
    annualSalary: 1800000,
    employmentType: 'FULL_TIME',
    hireDate: '2021-01-04',
    updatedAt: '2026-10-15T10:00:00Z',
    ...overrides,
  };
}

function page(content: Employee[], totalElements = content.length): Page<Employee> {
  return { content, page: { number: 0, size: 20, totalElements, totalPages: Math.ceil(totalElements / 20) } };
}

class FakeEmployeeApi {
  readonly queries: EmployeeQuery[] = [];
  respond: (query: EmployeeQuery) => Observable<Page<Employee>> = () => of(page([employee(1)]));

  list(query: EmployeeQuery): Observable<Page<Employee>> {
    this.queries.push(query);
    return this.respond(query);
  }

  get lastQuery(): EmployeeQuery {
    return this.queries[this.queries.length - 1];
  }
}

describe('EmployeeList', () => {
  let fixture: ComponentFixture<EmployeeList>;
  let loader: HarnessLoader;
  let api: FakeEmployeeApi;

  async function render(): Promise<void> {
    fixture = TestBed.createComponent(EmployeeList);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  async function select(name: string, option: string): Promise<void> {
    const harness = await loader.getHarness(MatSelectHarness.with({ selector: `[name="${name}"]` }));
    await harness.clickOptions({ text: option });
    await fixture.whenStable();
  }

  async function salaryHeader() {
    const sort = await loader.getHarness(MatSortHarness);
    return (await sort.getSortHeaders({ label: 'Annual salary' }))[0];
  }

  beforeEach(() => {
    api = new FakeEmployeeApi();
    TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [
        provideRouter([]),
        { provide: EmployeeApi, useValue: api },
        { provide: MetaApi, useValue: { filterOptions: () => of(FILTERS) } },
      ],
    });
  });

  it('loads the first page sorted by name and shows each salary in its own currency', async () => {
    api.respond = () =>
      of(
        page([
          employee(1),
          employee(2, { country: 'SG', countryName: 'Singapore', currency: 'SGD', annualSalary: 76600 }),
        ]),
      );

    await render();

    expect(api.queries).toHaveLength(1);
    expect(api.lastQuery).toMatchObject({ page: 0, size: 20, sort: { field: 'fullName', direction: 'asc' } });
    expect(text()).toContain('2 employees');
    expect(text()).toContain('₹1,800,000');
    expect(text()).toContain('SGD'); // never a bare "$" that could be read as USD
    expect(text()).not.toContain('$76,600');
  });

  it('filters by country and job title', async () => {
    await render();

    await select('country', 'India (INR)');
    await select('jobTitle', 'Software Engineer');

    expect(api.lastQuery).toMatchObject({ country: 'IN', jobTitle: 'Software Engineer', page: 0 });
  });

  it('debounces search so a typed word sends one request', async () => {
    await render();
    const search = await loader.getHarness(MatInputHarness.with({ selector: '[name="search"]' }));

    await search.setValue('sharma');
    await new Promise((resolve) => setTimeout(resolve, 350));
    await fixture.whenStable();

    const searches = api.queries.filter((query) => query.search);
    expect(searches).toHaveLength(1);
    expect(searches[0].search).toBe('sharma');
  });

  it('shows an empty state when nothing matches', async () => {
    api.respond = () => of(page([]));

    await render();

    expect(text()).toContain('No employees match these filters.');
  });

  it('only allows sorting by salary once a country is chosen', async () => {
    await render();

    expect(await (await salaryHeader()).isDisabled()).toBe(true);
    expect(text()).toContain('Pick a country to sort or compare by salary');

    await select('country', 'India (INR)');
    const header = await salaryHeader();
    expect(await header.isDisabled()).toBe(false);
    expect(text()).not.toContain('Pick a country to sort or compare by salary');

    await header.click();
    await fixture.whenStable();
    expect(api.lastQuery.sort).toEqual({ field: 'annualSalary', direction: 'asc' });
  });

  it('falls back to name order when the country is cleared during a salary sort', async () => {
    await render();
    await select('country', 'India (INR)');
    await (await salaryHeader()).click();
    await fixture.whenStable();
    expect(api.lastQuery.sort?.field).toBe('annualSalary');

    await (await loader.getHarness(MatButtonHarness.with({ text: 'Clear filters' }))).click();
    await fixture.whenStable();

    expect(api.lastQuery.country).toBeNull();
    expect(api.lastQuery.sort).toEqual({ field: 'fullName', direction: 'asc' });
  });

  it('pages on the server and goes back to page one when a filter changes', async () => {
    api.respond = (query) => of({ ...page([employee(1)], 45), page: { ...page([], 45).page, number: query.page } });
    await render();

    await (await loader.getHarness(MatPaginatorHarness)).goToNextPage();
    await fixture.whenStable();
    expect(api.lastQuery.page).toBe(1);

    await select('department', 'People');
    expect(api.lastQuery).toMatchObject({ department: 'People', page: 0 });
  });

  it('shows a readable error with a retry when loading fails', async () => {
    api.respond = () => throwError(() => new HttpErrorResponse({ status: 0 }));
    await render();

    expect(text()).toContain('Cannot reach the server');

    api.respond = () => of(page([employee(7)]));
    await (await loader.getHarness(MatButtonHarness.with({ text: 'Try again' }))).click();
    await fixture.whenStable();

    expect(text()).toContain('Person 7');
  });
});
