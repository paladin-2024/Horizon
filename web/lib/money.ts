// Formatting for the API's integer-minor-unit Money shape ({ amountMinor, currency }).
// Every supported currency (UGX, CDF, USD) uses 2 decimal minor units, matching
// how the backend's Money record stores every currency uniformly.

const CURRENCY_LOCALE: Record<string, string> = {
  UGX: "en-UG",
  CDF: "fr-CD",
  USD: "en-US",
};

/** "UGX 300,000.00". Falls back to a generic locale for an unrecognized currency. */
export function formatMoney(amountMinor: number, currency: string): string {
  const formatter = new Intl.NumberFormat(CURRENCY_LOCALE[currency] ?? "en-US", {
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
