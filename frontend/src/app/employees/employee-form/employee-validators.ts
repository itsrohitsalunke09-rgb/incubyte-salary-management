import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Client-side copies of the backend rules (CreateEmployeeRequest / UpdateEmployeeRequest) so the
// HR manager gets instant feedback. The backend stays the source of truth: its field errors are
// shown inline too, in case these ever drift.

export const EMPLOYEE_CODE_PATTERN = /^[A-Za-z0-9-]+$/;

/** Like Validators.required, but "   " counts as empty (the backend's @NotBlank). */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim() === '' ? { required: true } : null;

/** Annual salary must be greater than 0 with at most 2 decimals (@Positive @Digits(fraction = 2)). */
export const salary: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (value === null || value === undefined || value === '') {
    return null; // "required" reports missing values
  }
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount <= 0) {
    return { positive: true };
  }
  if (!/^\d+(\.\d{1,2})?$/.test(String(value))) {
    return { decimals: true };
  }
  return null;
};

/** Hire date (yyyy-MM-dd) may not be in the future (@PastOrPresent). */
export function notInFuture(today: () => string = localToday): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    typeof control.value === 'string' && control.value > today() ? { future: true } : null;
}

/** Today's date as yyyy-MM-dd in the user's time zone. */
export function localToday(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** Human message for the first error on a control. */
export function errorMessage(errors: ValidationErrors | null | undefined): string | null {
  if (!errors) {
    return null;
  }
  if (errors['server']) return errors['server'];
  if (errors['required']) return 'Required';
  if (errors['maxlength']) return `At most ${errors['maxlength'].requiredLength} characters`;
  if (errors['pattern']) return "Only letters, digits and '-'";
  if (errors['positive']) return 'Must be greater than 0';
  if (errors['decimals']) return 'At most 2 decimal places';
  if (errors['future']) return 'Cannot be in the future';
  return 'Invalid value';
}
