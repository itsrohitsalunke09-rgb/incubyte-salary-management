import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { of } from 'rxjs';

import { ApiError, toApiError } from '../../core/api/api-error';
import { EmployeeApi } from '../../core/api/employee-api';
import { MetaApi } from '../../core/api/meta-api';
import {
  CountryCode,
  Employee,
  EMPLOYMENT_TYPES,
  EmploymentType,
  UpdateEmployeeRequest,
} from '../../core/api/models';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog';
import { EMPLOYEE_CODE_PATTERN, errorMessage, localToday, notBlank, notInFuture, salary } from './employee-validators';

/** Create (`/employees/new`) and edit (`/employees/:id/edit`) in one component. */
@Component({
  selector: 'app-employee-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss',
})
export class EmployeeForm {
  private readonly employeeApi = inject(EmployeeApi);
  private readonly metaApi = inject(MetaApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  /** Route parameter; absent when creating. */
  readonly id = input<string>();

  protected readonly isEdit = computed(() => !!this.id());
  protected readonly employmentTypes = EMPLOYMENT_TYPES;
  protected readonly today = localToday();
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    employeeCode: ['', [Validators.required, notBlank, Validators.maxLength(20), Validators.pattern(EMPLOYEE_CODE_PATTERN)]],
    fullName: ['', [Validators.required, notBlank, Validators.maxLength(120)]],
    jobTitle: ['', [Validators.required, notBlank, Validators.maxLength(80)]],
    department: ['', [Validators.required, notBlank, Validators.maxLength(80)]],
    country: ['' as CountryCode | '', Validators.required],
    annualSalary: [null as number | null, [Validators.required, salary]],
    employmentType: ['FULL_TIME' as EmploymentType, Validators.required],
    hireDate: ['', [Validators.required, notInFuture()]],
  });

  protected readonly filterOptions = rxResource({ stream: () => this.metaApi.filterOptions() });
  protected readonly employee = rxResource({
    params: () => (this.id() ? Number(this.id()) : undefined),
    stream: ({ params: id }) => (id === undefined ? of(undefined) : this.employeeApi.get(id)),
  });
  protected readonly loadError = computed(() => {
    const error = this.employee.error();
    if (!error) return null;
    const apiError = toApiError(error);
    return apiError.status === 404 ? 'This employee no longer exists.' : apiError.message;
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  private readonly selectedCountry = computed(() => this.formValue().country);
  protected readonly currency = computed(
    () => this.filterOptions.value()?.countries.find((c) => c.code === this.selectedCountry())?.currency ?? null,
  );
  protected readonly jobTitleSuggestions = computed(() =>
    suggest(this.filterOptions.value()?.jobTitles, this.formValue().jobTitle),
  );
  protected readonly departmentSuggestions = computed(() =>
    suggest(this.filterOptions.value()?.departments, this.formValue().department),
  );

  constructor() {
    effect(() => {
      const employee = this.employee.hasValue() ? this.employee.value() : undefined;
      if (employee) {
        this.fill(employee);
      }
    });
  }

  protected error(name: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[name];
    return control.touched || control.dirty ? errorMessage(control.errors) : null;
  }

  protected employmentTypeLabel(type: EmploymentType): string {
    return type.replace('_', '-').toLowerCase().replace(/^./, (c) => c.toUpperCase());
  }

  protected save(): void {
    this.formError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: UpdateEmployeeRequest = {
      fullName: value.fullName.trim(),
      jobTitle: value.jobTitle.trim(),
      department: value.department.trim(),
      country: value.country as CountryCode,
      annualSalary: Number(value.annualSalary),
      employmentType: value.employmentType,
      hireDate: value.hireDate,
    };
    const id = this.id();
    const call = id
      ? this.employeeApi.update(Number(id), request)
      : this.employeeApi.create({ ...request, employeeCode: value.employeeCode.trim() });

    this.saving.set(true);
    call.subscribe({
      next: (saved) => this.done(id ? `Saved ${saved.fullName}` : `Added ${saved.fullName}`),
      error: (error) => {
        this.saving.set(false);
        this.showServerErrors(toApiError(error));
      },
    });
  }

  protected confirmDelete(): void {
    const employee = this.employee.hasValue() ? this.employee.value() : undefined;
    if (!employee) return;
    const data: ConfirmDialogData = {
      title: 'Delete employee?',
      message: `${employee.fullName} (${employee.employeeCode}) and their salary record will be removed. This cannot be undone.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open(ConfirmDialog, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.saving.set(true);
        this.employeeApi.delete(employee.id).subscribe({
          next: () => this.done(`Deleted ${employee.fullName}`),
          error: (error) => {
            this.saving.set(false);
            this.formError.set(toApiError(error).message);
          },
        });
      });
  }

  private fill(employee: Employee): void {
    this.form.reset({
      employeeCode: employee.employeeCode,
      fullName: employee.fullName,
      jobTitle: employee.jobTitle,
      department: employee.department,
      country: employee.country,
      annualSalary: employee.annualSalary,
      employmentType: employee.employmentType,
      hireDate: employee.hireDate,
    });
    this.form.controls.employeeCode.disable(); // immutable once created
  }

  private showServerErrors(error: ApiError): void {
    if (error.status === 409) {
      this.setServerError('employeeCode', error.message);
      return;
    }
    let shownInline = false;
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      shownInline = this.setServerError(field, message) || shownInline;
    }
    if (!shownInline) {
      this.formError.set(error.message);
    }
  }

  private setServerError(field: string, message: string): boolean {
    const control = this.form.get(field);
    if (!control) return false;
    control.setErrors({ ...control.errors, server: message });
    control.markAsTouched();
    return true;
  }

  private done(message: string): void {
    this.snackBar.open(message, undefined, { duration: 3000 });
    void this.router.navigateByUrl('/employees');
  }
}

function suggest(options: string[] | undefined, typed: string | undefined): string[] {
  const needle = (typed ?? '').trim().toLowerCase();
  return (options ?? []).filter((option) => option.toLowerCase().includes(needle)).slice(0, 8);
}
