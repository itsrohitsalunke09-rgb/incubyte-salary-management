import { HttpErrorResponse } from '@angular/common/http';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatSortHarness } from '@angular/material/sort/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { InsightsApi } from '../core/api/insights-api';
import { CountryCode, CountryGroupInsights, CountryInsight, GroupInsight } from '../core/api/models';
import { InsightsPage } from './insights-page';

const INDIA: CountryInsight = {
  country: 'IN',
  countryName: 'India',
  currency: 'INR',
  headcount: 3993,
  min: 380500,
  max: 4698400,
  average: 1940240.07,
  median: 1877700,
  totalPayroll: 7747378600,
};
const SINGAPORE: CountryInsight = {
  country: 'SG',
  countryName: 'Singapore',
  currency: 'SGD',
  headcount: 811,
  min: 30000,
  max: 260000,
  average: 118000,
  median: 113500,
  totalPayroll: 97130000,
};

function group(name: string, median: number, headcount = 10): GroupInsight {
  return { name, headcount, min: median * 0.8, max: median * 1.2, average: median, median, totalPayroll: median * headcount };
}

class FakeInsightsApi {
  countries: Observable<CountryInsight[]> = of([INDIA, SINGAPORE]);
  readonly calls: string[] = [];

  byCountry(): Observable<CountryInsight[]> {
    return this.countries;
  }
  byJobTitle(country: CountryCode): Observable<CountryGroupInsights> {
    this.calls.push(`${country}/job-titles`);
    return of(this.breakdown(country, [group('Software Engineer', 1800000), group('Staff Engineer', 3700000), group('Recruiter', 1100000)]));
  }
  byDepartment(country: CountryCode): Observable<CountryGroupInsights> {
    this.calls.push(`${country}/departments`);
    return of(this.breakdown(country, [group('Engineering', 2000000), group('People', 1000000)]));
  }
  private breakdown(country: CountryCode, groups: GroupInsight[]): CountryGroupInsights {
    const c = [INDIA, SINGAPORE].find((x) => x.country === country)!;
    return { country, countryName: c.countryName, currency: c.currency, groups };
  }
}

describe('InsightsPage', () => {
  let fixture: ComponentFixture<InsightsPage>;
  let loader: HarnessLoader;
  let api: FakeInsightsApi;
  let navigations: Record<string, unknown>[];

  async function render(queryParams: { country?: string; by?: string } = {}): Promise<void> {
    fixture = TestBed.createComponent(InsightsPage);
    for (const [key, value] of Object.entries(queryParams)) {
      fixture.componentRef.setInput(key, value);
    }
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  /** What the router would do: feed the new query params back in as inputs. */
  async function followNavigation(): Promise<void> {
    const params = navigations[navigations.length - 1] as { country?: string; by?: string };
    fixture.componentRef.setInput('country', params.country);
    fixture.componentRef.setInput('by', params.by);
    await fixture.whenStable();
  }

  function element(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function tableRows(): string[] {
    return Array.from(element().querySelectorAll('.drilldown tr.mat-mdc-row')).map(
      (row) => row.querySelector('td')?.textContent?.trim() ?? '',
    );
  }

  beforeEach(() => {
    api = new FakeInsightsApi();
    navigations = [];
    TestBed.configureTestingModule({
      imports: [InsightsPage],
      providers: [provideRouter([]), { provide: InsightsApi, useValue: api }],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation((_commands, extras) => {
      navigations.push(extras?.queryParams as Record<string, unknown>);
      return Promise.resolve(true);
    });
  });

  it('shows a card per country with headcount, median and payroll in local currency', async () => {
    await render();

    const cards = Array.from(element().querySelectorAll('.country-card')).map((card) =>
      card.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(cards).toHaveLength(2);
    expect(cards[0]).toContain('India');
    expect(cards[0]).toContain('3,993');
    expect(cards[0]).toContain('₹1,877,700');
    expect(cards[0]).toContain('₹7.75B');
    expect(cards[1]).toContain('SGD 113,500'); // not "$113,500"
  });

  it('opens the first country by job title, highest median first', async () => {
    await render();

    expect(element().querySelector('.country-card.selected')?.textContent).toContain('India');
    expect(api.calls).toEqual(['IN/job-titles']);
    expect(tableRows()).toEqual(['Staff Engineer', 'Software Engineer', 'Recruiter']);
  });

  it('drills into another country when its card is chosen', async () => {
    await render();

    (element().querySelectorAll('.country-card')[1] as HTMLButtonElement).click();
    expect(navigations).toEqual([{ country: 'SG', by: 'job-titles' }]);
    await followNavigation();

    expect(api.calls).toEqual(['IN/job-titles', 'SG/job-titles']);
    expect(element().querySelector('.drilldown h2')?.textContent).toContain('Singapore');
  });

  it('switches the breakdown to departments', async () => {
    await render({ country: 'IN' });

    await (await loader.getHarness(MatButtonToggleHarness.with({ text: 'By department' }))).check();
    expect(navigations).toEqual([{ country: 'IN', by: 'departments' }]);
    await followNavigation();

    expect(api.calls).toEqual(['IN/job-titles', 'IN/departments']);
    expect(element().querySelector('.drilldown th')?.textContent).toContain('Department');
    expect(tableRows()).toEqual(['Engineering', 'People']);
  });

  it('restores a shared view from the URL and ignores an unknown country', async () => {
    await render({ country: 'SG', by: 'departments' });
    expect(api.calls).toEqual(['SG/departments']);

    TestBed.resetTestingModule();
    api = new FakeInsightsApi();
    TestBed.configureTestingModule({
      imports: [InsightsPage],
      providers: [provideRouter([]), { provide: InsightsApi, useValue: api }],
    });
    await render({ country: 'XX' });
    expect(api.calls).toEqual(['IN/job-titles']);
  });

  it('sorts the breakdown by any column', async () => {
    await render();
    const sort = await loader.getHarness(MatSortHarness);
    const nameHeader = (await sort.getSortHeaders({ label: 'Job title' }))[0];

    await nameHeader.click();

    expect(tableRows()).toEqual(['Recruiter', 'Software Engineer', 'Staff Engineer']);
  });

  it('explains an empty organisation instead of showing zeros', async () => {
    api.countries = of([]);

    await render();

    expect(element().textContent).toContain('No employees yet');
    expect(api.calls).toEqual([]);
  });

  it('shows an error with a retry', async () => {
    api.countries = throwError(() => new HttpErrorResponse({ status: 500, error: { detail: 'Database unavailable' } }));
    await render();

    expect(element().textContent).toContain('Database unavailable');

    api.countries = of([INDIA]);
    await (await loader.getHarness(MatButtonHarness.with({ text: 'Try again' }))).click();
    await fixture.whenStable();
    expect(element().querySelectorAll('.country-card')).toHaveLength(1);
  });
});
