import { describe, expect, it } from "vitest";
import { ApiError } from "./client";
import { GENERIC_ERROR, errorMessage, formFieldErrors } from "./messages";

describe("errorMessage", () => {
  it("returns the caller's wording for other 4xx errors", () => {
    const error = new ApiError(401, "invalid_credentials", "bad");
    expect(errorMessage(error, "Incorrect email or password.")).toBe("Incorrect email or password.");
  });

  it("explains rate limiting with the wait time", () => {
    expect(errorMessage(new ApiError(429, "rate_limited", "x", { retryAfterSeconds: 30 }), "n/a")).toBe(
      "Too many attempts. Try again in 30 seconds."
    );
    expect(errorMessage(new ApiError(429, "rate_limited", "x", { retryAfterSeconds: 90 }), "n/a")).toBe(
      "Too many attempts. Try again in 2 minutes."
    );
    expect(errorMessage(new ApiError(429, "rate_limited", "x"), "n/a")).toBe(
      "Too many attempts. Please wait a moment and try again."
    );
  });

  it("uses the generic message for server errors and non-API errors", () => {
    expect(errorMessage(new ApiError(503, "unknown_error", "x"), "n/a")).toBe(GENERIC_ERROR);
    expect(errorMessage(new TypeError("Failed to fetch"), "n/a")).toBe(GENERIC_ERROR);
  });
});

describe("formFieldErrors", () => {
  const error = new ApiError(400, "validation_failed", "Invalid", {
    errors: [
      { field: "phone", message: "must be a valid phone number" },
      { field: "identifier", message: "must not be blank" },
      { field: "other", message: "ignored" },
    ],
  });

  it("maps API fields to form fields and drops unknown ones", () => {
    expect(formFieldErrors(error, { phone: "phone", identifier: "email" })).toEqual([
      { field: "phone", message: "must be a valid phone number" },
      { field: "email", message: "must not be blank" },
    ]);
  });

  it("returns nothing for other errors", () => {
    expect(formFieldErrors(new ApiError(400, "other", "x"), { phone: "phone" })).toEqual([]);
    expect(formFieldErrors(new Error("x"), { phone: "phone" })).toEqual([]);
  });
});
