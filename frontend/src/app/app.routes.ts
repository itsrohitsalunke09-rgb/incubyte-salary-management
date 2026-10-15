import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    title: 'Employees · ACME Salary',
    loadComponent: () => import('./employees/employee-list/employee-list').then((m) => m.EmployeeList),
  },
  {
    path: 'employees/new',
    title: 'Add employee · ACME Salary',
    loadComponent: () => import('./employees/employee-form/employee-form').then((m) => m.EmployeeForm),
  },
  {
    path: 'employees/:id/edit',
    title: 'Edit employee · ACME Salary',
    loadComponent: () => import('./employees/employee-form/employee-form').then((m) => m.EmployeeForm),
  },
  { path: '**', redirectTo: 'employees' },
];
