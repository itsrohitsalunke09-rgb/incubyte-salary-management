import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CreateEmployeeRequest, Employee, EmployeeQuery, Page, UpdateEmployeeRequest } from './models';

@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/employees';

  list(query: EmployeeQuery): Observable<Page<Employee>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    const filters = {
      search: query.search?.trim(),
      country: query.country,
      department: query.department,
      jobTitle: query.jobTitle,
    };
    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        params = params.set(key, value); // blank filters are simply not sent
      }
    }
    if (query.sort) {
      params = params.set('sort', `${query.sort.field},${query.sort.direction}`);
    }
    return this.http.get<Page<Employee>>(this.baseUrl, { params });
  }

  get(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, request);
  }

  update(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
