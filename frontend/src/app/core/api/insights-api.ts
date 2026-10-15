import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CountryCode, CountryGroupInsights, CountryInsight } from './models';

@Injectable({ providedIn: 'root' })
export class InsightsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/insights/countries';

  byCountry(): Observable<CountryInsight[]> {
    return this.http.get<CountryInsight[]>(this.baseUrl);
  }

  byJobTitle(country: CountryCode): Observable<CountryGroupInsights> {
    return this.http.get<CountryGroupInsights>(`${this.baseUrl}/${country}/job-titles`);
  }

  byDepartment(country: CountryCode): Observable<CountryGroupInsights> {
    return this.http.get<CountryGroupInsights>(`${this.baseUrl}/${country}/departments`);
  }
}
