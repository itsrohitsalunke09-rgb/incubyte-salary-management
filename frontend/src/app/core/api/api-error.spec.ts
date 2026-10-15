import { HttpErrorResponse } from '@angular/common/http';

import { toApiError } from './api-error';

describe('toApiError', () => {
  it('reads detail and per-field errors from a problem-details body', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        title: 'Validation failed',
        detail: 'One or more fields are invalid',
        errors: { annualSalary: 'must be greater than 0' },
      },
    });

    expect(toApiError(error)).toEqual({
      status: 400,
      message: 'One or more fields are invalid',
      fieldErrors: { annualSalary: 'must be greater than 0' },
    });
  });

  it('falls back to the title, then to the status code', () => {
    expect(toApiError(new HttpErrorResponse({ status: 409, error: { title: 'Conflict' } })).message).toBe('Conflict');
    expect(toApiError(new HttpErrorResponse({ status: 502, error: 'Bad gateway' })).message).toBe(
      'Request failed (502)',
    );
  });

  it('explains a network failure in plain words', () => {
    const offline = new HttpErrorResponse({ status: 0 });

    expect(toApiError(offline).message).toContain('Cannot reach the server');
  });

  it('handles non-HTTP errors', () => {
    expect(toApiError(new Error('boom'))).toEqual({
      status: 0,
      message: 'Something went wrong. Please try again.',
      fieldErrors: {},
    });
  });
});
