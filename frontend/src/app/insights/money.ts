/**
 * Formats an amount in its own currency. `compact` turns 7,747,378,600 INR into "₹7.75B" for
 * headline numbers on cards; tables use full figures. Currency codes are shown where the symbol
 * would be ambiguous (SGD, AED), so a Singapore figure never looks like US dollars.
 */
export function formatMoney(amount: number, currency: string, compact = false): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    currencyDisplay: 'symbol',
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: compact ? 2 : 0,
  }).format(amount);
}
