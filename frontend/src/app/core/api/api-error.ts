import { HttpErrorResponse } from '@angular/common/http';

/** What the UI needs from a failed call: a message for a snackbar and per-field messages for forms. */
export interface ApiError {
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
}

/** Reads the backend's RFC 7807 problem details, with sensible fallbacks for network failures. */
export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, message: 'Something went wrong. Please try again.', fieldErrors: {} };
  }
  if (error.status === 0) {
    return { status: 0, message: 'Cannot reach the server. Check your connection and try again.', fieldErrors: {} };
  }
  const body = typeof error.error === 'object' && error.error !== null ? error.error : {};
  return {
    status: error.status,
    message: body.detail ?? body.title ?? `Request failed (${error.status})`,
    fieldErrors: body.errors ?? {},
  };
}
