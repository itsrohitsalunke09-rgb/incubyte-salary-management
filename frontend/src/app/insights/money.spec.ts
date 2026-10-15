import { formatMoney } from './money';

describe('formatMoney', () => {
  it('shows whole amounts with grouping in the local currency', () => {
    expect(formatMoney(1877700, 'INR')).toBe('₹1,877,700');
    expect(formatMoney(147500, 'USD')).toBe('$147,500');
    expect(formatMoney(72100.4, 'GBP')).toBe('£72,100');
  });

  it('never shows a bare dollar sign for non-US dollars', () => {
    expect(formatMoney(110000, 'SGD')).toContain('SGD');
    expect(formatMoney(300000, 'AED')).toContain('AED');
  });

  it('compacts large headline figures', () => {
    expect(formatMoney(7747378600, 'INR', true)).toBe('₹7.75B');
    expect(formatMoney(375100300, 'USD', true)).toBe('$375.1M');
  });
});
