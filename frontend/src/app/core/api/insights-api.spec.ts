import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { InsightsApi } from './insights-api';
import { MetaApi } from './meta-api';

describe('InsightsApi and MetaApi', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls the three insight endpoints', () => {
    const api = TestBed.inject(InsightsApi);

    api.byCountry().subscribe();
    api.byJobTitle('IN').subscribe();
    api.byDepartment('US').subscribe();

    http.expectOne({ method: 'GET', url: '/api/insights/countries' }).flush([]);
    http.expectOne({ method: 'GET', url: '/api/insights/countries/IN/job-titles' }).flush({ groups: [] });
    http.expectOne({ method: 'GET', url: '/api/insights/countries/US/departments' }).flush({ groups: [] });
  });

  it('loads filter options', () => {
    TestBed.inject(MetaApi).filterOptions().subscribe();

    http.expectOne({ method: 'GET', url: '/api/meta/filters' }).flush({ countries: [], departments: [], jobTitles: [] });
  });
});
