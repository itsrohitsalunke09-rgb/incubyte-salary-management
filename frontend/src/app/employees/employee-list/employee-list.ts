import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { toApiError } from '../../core/api/api-error';
import { EmployeeApi } from '../../core/api/employee-api';
import { MetaApi } from '../../core/api/meta-api';
import { CountryCode, Employee, EmployeeQuery, Page, SortableField } from '../../core/api/models';
import { canSortBySalary, DEFAULT_SORT, effectiveSort, EmployeeSort } from '../salary-sort';

@Component({
  selector: 'app-employee-list',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList {
  private readonly employeeApi = inject(EmployeeApi);
  private readonly metaApi = inject(MetaApi);

  protected readonly columns = [
    'employeeCode',
    'fullName',
    'jobTitle',
    'department',
    'country',
    'annualSalary',
    'employmentType',
    'hireDate',
    'actions',
  ];
  protected readonly pageSizes = [20, 50, 100];

  // --- filter state ---
  protected readonly searchText = signal('');
  protected readonly country = signal<CountryCode | null>(null);
  protected readonly department = signal<string | null>(null);
  protected readonly jobTitle = signal<string | null>(null);
  protected readonly sort = signal<EmployeeSort>(DEFAULT_SORT);
  protected readonly pageSize = signal(20);

  /** Typing is debounced so we query once the HR manager pauses, not on every keystroke. */
  private readonly search = toSignal(toObservable(this.searchText).pipe(debounceTime(300), distinctUntilChanged()), {
    initialValue: '',
  });

  protected readonly salarySortAllowed = computed(() => canSortBySalary(this.country()));
  protected readonly activeSort = computed(() => effectiveSort(this.sort(), this.country()));

  /** Back to the first page whenever the filters, sort or page size change; the paginator can still move it. */
  protected readonly pageIndex = linkedSignal({
    source: () => [this.search(), this.country(), this.department(), this.jobTitle(), this.activeSort(), this.pageSize()],
    computation: () => 0,
  });
  protected readonly hasFilters = computed(
    () => !!(this.searchText() || this.country() || this.department() || this.jobTitle()),
  );

  private readonly query = computed<EmployeeQuery>(() => ({
    search: this.search(),
    country: this.country(),
    department: this.department(),
    jobTitle: this.jobTitle(),
    page: this.pageIndex(),
    size: this.pageSize(),
    sort: this.activeSort(),
  }));

  protected readonly employees = rxResource({
    params: () => this.query(),
    stream: ({ params }) => this.employeeApi.list(params),
  });
  /**
   * The page on screen. A resource drops its value while new params load, which would make the
   * table vanish and flicker on every filter, sort or page change; keep the last page until the next arrives.
   */
  protected readonly page = linkedSignal<Page<Employee> | undefined, Page<Employee> | undefined>({
    source: () => (this.employees.hasValue() ? this.employees.value() : undefined),
    computation: (latest, previous) => latest ?? previous?.value,
  });
  protected readonly filterOptions = rxResource({ stream: () => this.metaApi.filterOptions() });
  protected readonly errorMessage = computed(() => {
    const error = this.employees.error();
    return error ? toApiError(error).message : null;
  });

  protected onSortChange(sort: Sort): void {
    this.sort.set(sort.direction ? { field: sort.active as SortableField, direction: sort.direction } : DEFAULT_SORT);
  }

  protected onPage(event: PageEvent): void {
    if (event.pageSize !== this.pageSize()) {
      this.pageSize.set(event.pageSize); // resets to page 0 via pageIndex's linkedSignal
    } else {
      this.pageIndex.set(event.pageIndex);
    }
  }

  protected clearFilters(): void {
    this.searchText.set('');
    this.country.set(null);
    this.department.set(null);
    this.jobTitle.set(null);
  }

  protected employmentTypeLabel(type: string): string {
    return type.replace('_', '-').toLowerCase().replace(/^./, (c) => c.toUpperCase());
  }
}
