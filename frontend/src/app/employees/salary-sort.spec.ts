import { canSortBySalary, DEFAULT_SORT, effectiveSort } from './salary-sort';

describe('salary sort rule', () => {
  it('only allows sorting by salary when a single country is selected', () => {
    expect(canSortBySalary('IN')).toBe(true);
    expect(canSortBySalary(null)).toBe(false);
    expect(canSortBySalary(undefined)).toBe(false);
  });

  it('keeps a salary sort within a country', () => {
    const sort = { field: 'annualSalary', direction: 'desc' } as const;

    expect(effectiveSort(sort, 'US')).toEqual(sort);
  });

  it('falls back to the default sort when salary sorting would mix currencies', () => {
    expect(effectiveSort({ field: 'annualSalary', direction: 'desc' }, null)).toEqual(DEFAULT_SORT);
  });

  it('leaves non-salary sorts alone', () => {
    const sort = { field: 'hireDate', direction: 'desc' } as const;

    expect(effectiveSort(sort, null)).toEqual(sort);
  });
});
