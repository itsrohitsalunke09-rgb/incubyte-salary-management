import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { FilterOptions } from './models';

@Injectable({ providedIn: 'root' })
export class MetaApi {
  private readonly http = inject(HttpClient);

  filterOptions(): Observable<FilterOptions> {
    return this.http.get<FilterOptions>('/api/meta/filters');
  }
}
