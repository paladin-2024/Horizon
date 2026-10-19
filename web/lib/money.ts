// Formatting for the API's integer-minor-unit Money shape ({ amountMinor, currency }).
// Every supported currency (UGX, CDF, USD) uses 2 decimal minor units, matching
// how the backend's Money record stores every currency uniformly.

// One locale for every currency, deliberately: the UI's language is English
// throughout, so a per-currency locale (e.g. fr-CD for CDF) produces
// space-grouped, comma-decimal numbers that look inconsistent and broken
// next to UGX/USD's comma-grouped ones on the same screen — not more correct,
// just mismatched.
const NUMBER_LOCALE = "en-US";

/** "UGX 300,000.00". */
export function formatMoney(amountMinor: number, currency: string): string {
  const formatter = new Intl.NumberFormat(NUMBER_LOCALE, {
    style: "currency",
    currency,
    currencyDisplay: "code",
    minimumFractionDigits: 2,
  });
  return formatter.format(amountMinor / 100);
}

/** The bare decimal amount (300000.00), for places that render the currency separately. */
export function toDecimal(amountMinor: number): number {
  return amountMinor / 100;
}

export function toMinorUnits(decimal: number): number {
  return Math.round(decimal * 100);
}
