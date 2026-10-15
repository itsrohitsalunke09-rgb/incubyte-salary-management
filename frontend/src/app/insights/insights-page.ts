import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, input, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../core/api/api-error';
import { InsightsApi } from '../core/api/insights-api';
import { CountryCode, CountryInsight, GroupInsight } from '../core/api/models';
import { formatMoney } from './money';

export type Breakdown = 'job-titles' | 'departments';
type GroupColumn = keyof GroupInsight;

/**
 * "How does the org pay people?" — one card per country, then a drill-down of the chosen country
 * by job title or department. Selection lives in the URL (?country=IN&by=departments) so a view
 * can be bookmarked or shared. All amounts are in the country's own currency.
 */
@Component({
  selector: 'app-insights-page',
  imports: [
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './insights-page.html',
  styleUrl: './insights-page.scss',
})
export class InsightsPage {
  private readonly api = inject(InsightsApi);
  private readonly router = inject(Router);

  /** Query parameters (bound by the router). */
  readonly country = input<string>();
  readonly by = input<string>();

  protected readonly columns: GroupColumn[] = ['name', 'headcount', 'min', 'median', 'average', 'max', 'totalPayroll'];
  protected readonly money = formatMoney;

  protected readonly countries = rxResource({ stream: () => this.api.byCountry() });
  protected readonly countriesError = computed(() => {
    const error = this.countries.error();
    return error ? toApiError(error).message : null;
  });

  /** The chosen country: from the URL if valid, otherwise the first country with employees. */
  protected readonly selected = computed<CountryInsight | undefined>(() => {
    const list = this.countries.hasValue() ? this.countries.value() : [];
    return list.find((c) => c.country === this.country()) ?? list[0];
  });
  protected readonly breakdown = computed<Breakdown>(() => (this.by() === 'departments' ? 'departments' : 'job-titles'));

  protected readonly groups = rxResource({
    params: () => {
      const country = this.selected()?.country;
      return country ? { country, breakdown: this.breakdown() } : undefined;
    },
    stream: ({ params }) =>
      params.breakdown === 'departments' ? this.api.byDepartment(params.country) : this.api.byJobTitle(params.country),
  });

  /** Client-side sort of the (small) breakdown table; highest median first by default. */
  protected readonly sort = linkedSignal<Breakdown, Sort>({
    source: this.breakdown,
    computation: () => ({ active: 'median', direction: 'desc' }),
  });
  protected readonly rows = computed<GroupInsight[]>(() => {
    const groups = this.groups.hasValue() ? (this.groups.value()?.groups ?? []) : [];
    const { active, direction } = this.sort();
    if (!direction) return groups;
    const key = active as GroupColumn;
    const factor = direction === 'asc' ? 1 : -1;
    return [...groups].sort((a, b) =>
      typeof a[key] === 'string'
        ? factor * String(a[key]).localeCompare(String(b[key]))
        : factor * (Number(a[key]) - Number(b[key])),
    );
  });

  /** Upper bound for the pay-range bars, so rows are comparable within the table. */
  protected readonly scaleMax = computed(() => Math.max(1, ...this.rows().map((row) => row.max)));

  protected select(country: CountryCode): void {
    this.navigate({ country, by: this.breakdown() });
  }

  protected showBreakdown(by: Breakdown): void {
    this.navigate({ country: this.selected()?.country, by });
  }

  protected percent(value: number): number {
    return (value / this.scaleMax()) * 100;
  }

  private navigate(queryParams: { country?: CountryCode; by: Breakdown }): void {
    void this.router.navigate([], { queryParams, replaceUrl: true });
  }
}
