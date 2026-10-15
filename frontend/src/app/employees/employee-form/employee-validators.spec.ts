import { FormControl } from '@angular/forms';

import { errorMessage, notBlank, notInFuture, salary } from './employee-validators';

describe('employee validators', () => {
  it('treats whitespace-only text as missing', () => {
    expect(notBlank(new FormControl('   '))).toEqual({ required: true });
    expect(notBlank(new FormControl(' Asha '))).toBeNull();
  });

  it.each([
    [1800000, null],
    ['76600.5', null],
    ['76600.55', null],
    [0, { positive: true }],
    [-5, { positive: true }],
    ['abc', { positive: true }],
    ['100.555', { decimals: true }],
    [null, null],
    ['', null],
  ])('salary %s -> %j', (value, expected) => {
    expect(salary(new FormControl(value))).toEqual(expected);
  });

  it('rejects hire dates after today but allows today', () => {
    const validator = notInFuture(() => '2026-10-15');

    expect(validator(new FormControl('2026-10-16'))).toEqual({ future: true });
    expect(validator(new FormControl('2026-10-15'))).toBeNull();
    expect(validator(new FormControl('2019-01-01'))).toBeNull();
  });

  it('turns errors into short messages, preferring the server message', () => {
    expect(errorMessage({ server: 'Employee code EMP-1 is already in use', required: true })).toBe(
      'Employee code EMP-1 is already in use',
    );
    expect(errorMessage({ maxlength: { requiredLength: 20, actualLength: 25 } })).toBe('At most 20 characters');
    expect(errorMessage(null)).toBeNull();
  });
});
