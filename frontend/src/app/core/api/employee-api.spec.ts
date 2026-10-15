import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EmployeeApi } from './employee-api';
import { CreateEmployeeRequest, Employee, Page } from './models';

describe('EmployeeApi', () => {
  let api: EmployeeApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(EmployeeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends paging, filters and sort in the format the backend expects', () => {
    const emptyPage: Page<Employee> = {
      content: [],
      page: { size: 10, number: 2, totalElements: 0, totalPages: 0 },
    };
    let received: Page<Employee> | undefined;

    api
      .list({
        search: '  asha ',
        country: 'IN',
        department: 'Engineering',
        jobTitle: 'Software Engineer',
        page: 2,
        size: 10,
        sort: { field: 'annualSalary', direction: 'desc' },
      })
      .subscribe((page) => (received = page));

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('asha');
    expect(request.request.params.get('country')).toBe('IN');
    expect(request.request.params.get('department')).toBe('Engineering');
    expect(request.request.params.get('jobTitle')).toBe('Software Engineer');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sort')).toBe('annualSalary,desc');
    request.flush(emptyPage);
    expect(received).toEqual(emptyPage);
  });

  it('omits blank filters and sort entirely', () => {
    api.list({ search: '   ', country: null, department: '', page: 0, size: 20 }).subscribe();

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
  });

  it('creates with POST and the full body including the employee code', () => {
    const body: CreateEmployeeRequest = {
      employeeCode: 'EMP-1',
      fullName: 'Asha Patel',
      jobTitle: 'Software Engineer',
      department: 'Engineering',
      country: 'IN',
      annualSalary: 1800000,
      employmentType: 'FULL_TIME',
      hireDate: '2021-01-04',
    };

    api.create(body).subscribe();

    const request = http.expectOne('/api/employees');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({});
  });

  it('gets, updates and deletes by id', () => {
    api.get(7).subscribe();
    http.expectOne({ method: 'GET', url: '/api/employees/7' }).flush({});

    const { employeeCode: _code, ...update } = {
      employeeCode: 'ignored',
      fullName: 'Asha Patel',
      jobTitle: 'Staff Engineer',
      department: 'Engineering',
      country: 'SG' as const,
      annualSalary: 150000,
      employmentType: 'FULL_TIME' as const,
      hireDate: '2021-01-04',
    };
    api.update(7, update).subscribe();
    const put = http.expectOne({ method: 'PUT', url: '/api/employees/7' });
    expect(put.request.body).not.toHaveProperty('employeeCode');
    put.flush({});

    api.delete(7).subscribe();
    http.expectOne({ method: 'DELETE', url: '/api/employees/7' }).flush(null, { status: 204, statusText: 'No Content' });
  });
});
