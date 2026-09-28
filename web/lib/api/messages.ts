import { ApiError } from "./client";

export const GENERIC_ERROR = "Something went wrong. Please try again.";

function formatWait(seconds: number): string {
  if (seconds < 60) return `${Math.ceil(seconds)} seconds`;
  const minutes = Math.ceil(seconds / 60);
  return minutes === 1 ? "1 minute" : `${minutes} minutes`;
}

/**
 * Text to show under a form. 429 and server or network failures get fixed
 * wording; any other 4xx is a rejection specific to the form, so the caller
 * passes the wording it wants (for example "Incorrect email or password.").
 */
export function errorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return GENERIC_ERROR;
  if (error.status === 429) {
    return error.retryAfterSeconds
      ? `Too many attempts. Try again in ${formatWait(error.retryAfterSeconds)}.`
      : "Too many attempts. Please wait a moment and try again.";
  }
  if (error.status >= 500) return GENERIC_ERROR;
  return fallback;
}

/**
 * Bean-validation errors from the API (400 validation_failed) that belong to a
 * form field. `fieldMap` maps API field names to form field names; errors for
 * fields that are not in the map are left out.
 */
export function formFieldErrors(
  error: unknown,
  fieldMap: Record<string, string>
): { field: string; message: string }[] {
  if (!(error instanceof ApiError) || error.code !== "validation_failed") return [];
  return error.errors
    .filter((e) => e.field in fieldMap)
    .map((e) => ({ field: fieldMap[e.field], message: e.message }));
}
