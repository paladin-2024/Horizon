export type CountryCode = "UG" | "CD";

// International format for the two supported countries: +256 (Uganda) or +243
// (DR Congo) followed by nine digits.
const SUPPORTED_PHONE = /^\+(256|243)\d{9}$/;

/** Removes spaces, dashes, dots and parentheses: "+256 771-234 567" -> "+256771234567". */
export function normalizePhone(input: string): string {
  return input.replace(/[\s\-().]/g, "");
}

export function isValidPhone(input: string): boolean {
  return SUPPORTED_PHONE.test(normalizePhone(input));
}

/** Returns "UG" for +256 numbers, "CD" for +243 numbers, null for anything else. */
export function countryFromPhone(input: string): CountryCode | null {
  const phone = normalizePhone(input);
  if (!SUPPORTED_PHONE.test(phone)) return null;
  return phone.startsWith("+256") ? "UG" : "CD";
}
