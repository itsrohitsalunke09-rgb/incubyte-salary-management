import { HttpErrorResponse } from '@angular/common/http';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { EmployeeApi } from '../../core/api/employee-api';
import { MetaApi } from '../../core/api/meta-api';
import { CreateEmployeeRequest, Employee, FilterOptions, UpdateEmployeeRequest } from '../../core/api/models';
import { EmployeeForm } from './employee-form';

const FILTERS: FilterOptions = {
  countries: [
    { code: 'IN', name: 'India', currency: 'INR' },
    { code: 'SG', name: 'Singapore', currency: 'SGD' },
  ],
  departments: ['Engineering', 'People'],
  jobTitles: ['Software Engineer', 'Staff Engineer'],
};

const ASHA: Employee = {
  id: 7,
  employeeCode: 'EMP-00007',
  fullName: 'Asha Patel',
  jobTitle: 'Software Engineer',
  department: 'Engineering',
  country: 'IN',
  countryName: 'India',
  currency: 'INR',
  annualSalary: 1800000,
  employmentType: 'FULL_TIME',
  hireDate: '2021-01-04',
  updatedAt: '2026-10-15T10:00:00Z',
};

class FakeEmployeeApi {
  created: CreateEmployeeRequest[] = [];
  updated: { id: number; request: UpdateEmployeeRequest }[] = [];
  deleted: number[] = [];
  getResult: Observable<Employee> = of(ASHA);
  saveResult: Observable<Employee> | null = null;

  get(): Observable<Employee> {
    return this.getResult;
  }
  create(request: CreateEmployeeRequest): Observable<Employee> {
    this.created.push(request);
    return this.saveResult ?? of({ ...ASHA, ...request, id: 99 });
  }
  update(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    this.updated.push({ id, request });
    return this.saveResult ?? of({ ...ASHA, ...request });
  }
  delete(id: number): Observable<void> {
    this.deleted.push(id);
    return of(undefined);
  }
}

describe('EmployeeForm', () => {
  let fixture: ComponentFixture<EmployeeForm>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let api: FakeEmployeeApi;
  let navigatedTo: string[];

  async function render(id?: string): Promise<void> {
    fixture = TestBed.createComponent(EmployeeForm);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    await fixture.whenStable();
  }

  async function input(name: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `[name="${name}"]` }));
  }

  async function select(name: string, option: string): Promise<void> {
    await (await loader.getHarness(MatSelectHarness.with({ selector: `[name="${name}"]` }))).clickOptions({
      text: option,
    });
  }

  async function fillValidNewEmployee(): Promise<void> {
    await (await input('employeeCode')).setValue('EMP-12345');
    await (await input('fullName')).setValue('  Ravi Kumar ');
    await (await input('jobTitle')).setValue('Software Engineer');
    await (await input('department')).setValue('Engineering');
    await select('country', 'India (INR)');
    await (await input('annualSalary')).setValue('1950000');
    await (await input('hireDate')).setValue('2024-02-01');
  }

  async function click(text: string | RegExp): Promise<void> {
    await (await loader.getHarness(MatButtonHarness.with({ text }))).click();
    await fixture.whenStable();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(() => {
    api = new FakeEmployeeApi();
    navigatedTo = [];
    TestBed.configureTestingModule({
      imports: [EmployeeForm],
      providers: [
        provideRouter([]),
        { provide: EmployeeApi, useValue: api },
        { provide: MetaApi, useValue: { filterOptions: () => of(FILTERS) } },
        // Dialogs close immediately, so afterClosed() fires within the test step.
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockImplementation((url) => {
      navigatedTo.push(String(url));
      return Promise.resolve(true);
    });
  });

  it('blocks submission and explains every problem when the form is incomplete', async () => {
    await render();
    await (await input('employeeCode')).setValue('bad code!');
    await (await input('annualSalary')).setValue('100.555');
    await (await input('hireDate')).setValue('2999-01-01');

    await click('Add employee');

    expect(api.created).toHaveLength(0);
    const text = pageText();
    expect(text).toContain("Only letters, digits and '-'");
    expect(text).toContain('At most 2 decimal places');
    expect(text).toContain('Cannot be in the future');
    expect(text).toContain('Required'); // full name, job title, ...
  });

  it('shows the currency of the chosen country next to the salary', async () => {
    await render();
    expect(pageText()).toContain('Local currency of the chosen country');

    await select('country', 'Singapore (SGD)');
    await fixture.whenStable();

    expect(pageText()).toContain('Yearly base salary in SGD');
  });

  it('creates a trimmed employee, confirms and returns to the list', async () => {
    await render();
    await fillValidNewEmployee();

    await click('Add employee');

    expect(api.created).toEqual([
      {
        employeeCode: 'EMP-12345',
        fullName: 'Ravi Kumar',
        jobTitle: 'Software Engineer',
        department: 'Engineering',
        country: 'IN',
        annualSalary: 1950000,
        employmentType: 'FULL_TIME',
        hireDate: '2024-02-01',
      },
    ]);
    expect(await (await rootLoader.getHarness(MatSnackBarHarness)).getMessage()).toBe('Added Ravi Kumar');
    expect(navigatedTo).toEqual(['/employees']);
  });

  it('shows a duplicate employee code from the server on the code field', async () => {
    await render();
    await fillValidNewEmployee();
    api.saveResult = throwError(
      () =>
        new HttpErrorResponse({
          status: 409,
          error: { title: 'Duplicate employee code', detail: 'Employee code EMP-12345 is already in use' },
        }),
    );

    await click('Add employee');

    expect(pageText()).toContain('Employee code EMP-12345 is already in use');
    expect(navigatedTo).toEqual([]);
  });

  it('shows server validation errors next to the matching fields', async () => {
    await render();
    await fillValidNewEmployee();
    api.saveResult = throwError(
      () =>
        new HttpErrorResponse({
          status: 400,
          error: { detail: 'One or more fields are invalid', errors: { department: 'size must be between 0 and 80' } },
        }),
    );

    await click('Add employee');

    const department = await loader.getHarness(MatInputHarness.with({ selector: '[name="department"]' }));
    const host = await department.host();
    expect(await host.getAttribute('aria-invalid')).toBe('true');
    expect(pageText()).toContain('size must be between 0 and 80');
  });

  it('edits an existing employee without sending the immutable code', async () => {
    await render('7');

    const code = await input('employeeCode');
    expect(await code.getValue()).toBe('EMP-00007');
    expect(await code.isDisabled()).toBe(true);
    expect(pageText()).toContain("Employee codes can't be changed");

    await (await input('annualSalary')).setValue('2100000');
    await click('Save changes');

    expect(api.updated).toHaveLength(1);
    expect(api.updated[0].id).toBe(7);
    expect(api.updated[0].request.annualSalary).toBe(2100000);
    expect(api.updated[0].request).not.toHaveProperty('employeeCode');
    expect(navigatedTo).toEqual(['/employees']);
  });

  it('tells the user when the employee no longer exists', async () => {
    api.getResult = throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Employee 7 does not exist' } }));

    await render('7');

    expect(pageText()).toContain('This employee no longer exists.');
  });

  it('asks before deleting and does nothing when cancelled', async () => {
    await render('7');

    await click(/Delete/);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    expect(await dialog.getContentText()).toContain('Asha Patel (EMP-00007)');
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Cancel' }))).click();
    await fixture.whenStable();

    expect(api.deleted).toEqual([]);
    expect(navigatedTo).toEqual([]);
  });

  it('deletes after confirmation and returns to the list', async () => {
    await render('7');

    await click(/Delete/);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Delete' }))).click();
    await fixture.whenStable();

    expect(api.deleted).toEqual([7]);
    expect(navigatedTo).toEqual(['/employees']);
  });
});
