import { describe, expect, it } from "vitest";
import { countryFromPhone, isValidPhone, normalizePhone } from "./phone";

describe("normalizePhone", () => {
  it("removes spaces, dashes, dots and parentheses", () => {
    expect(normalizePhone(" +256 (771) 234-567 ")).toBe("+256771234567");
    expect(normalizePhone("+243.812.345.678")).toBe("+243812345678");
  });
});

describe("isValidPhone", () => {
  it("accepts Uganda and DR Congo numbers in international format", () => {
    expect(isValidPhone("+256771234567")).toBe(true);
    expect(isValidPhone("+243 812 345 678")).toBe(true);
  });

  it("rejects local format, other countries and wrong lengths", () => {
    expect(isValidPhone("0771234567")).toBe(false);
    expect(isValidPhone("+254712345678")).toBe(false);
    expect(isValidPhone("+25677123456")).toBe(false);
    expect(isValidPhone("+2567712345678")).toBe(false);
    expect(isValidPhone("+256abc234567")).toBe(false);
    expect(isValidPhone("")).toBe(false);
  });
});

describe("countryFromPhone", () => {
  it("maps the calling code to the API country code", () => {
    expect(countryFromPhone("+256771234567")).toBe("UG");
    expect(countryFromPhone("+243 812 345 678")).toBe("CD");
  });

  it("returns null for unsupported numbers", () => {
    expect(countryFromPhone("+254712345678")).toBeNull();
    expect(countryFromPhone("nonsense")).toBeNull();
  });
});
