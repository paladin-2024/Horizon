import { countryFromPhone, normalizePhone } from "@/lib/phone";
import { apiFetch, refreshSession } from "./client";

export const register = (request: RegisterRequest) =>
  apiFetch<void>("/auth/register", { method: "POST", body: request });

export const verifyOtp = (request: VerifyOtpRequest) =>
  apiFetch<void>("/auth/verify-otp", { method: "POST", body: request });

export const resendOtp = (request: ResendOtpRequest) =>
  apiFetch<void>("/auth/resend-otp", { method: "POST", body: request });

export const login = (request: LoginRequest) =>
  apiFetch<void>("/auth/login", { method: "POST", body: request });

/** Rotates the cookies. Resolves true on success, false on failure, never throws. */
export const refresh = refreshSession;

export const logout = () => apiFetch<void>("/auth/logout", { method: "POST" });

export const me = () => apiFetch<ApiUser>("/auth/me");

export type SignUpFormValues = {
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  nationalId?: string;
  password: string;
};

/**
 * Turns the sign-up form values into the API request. The country is not a
 * form field: it comes from the phone number's calling code (+256 UG, +243 CD).
 * The address, city, state, postal code and date of birth fields are not sent
 * because the API does not store them.
 */
export function buildRegisterRequest(values: SignUpFormValues): RegisterRequest {
  const phone = normalizePhone(values.phone ?? "");
  const country = countryFromPhone(phone);
  const firstName = values.firstName?.trim();
  const lastName = values.lastName?.trim();
  if (!country || !firstName || !lastName) {
    throw new Error("Sign-up values are incomplete");
  }

  const request: RegisterRequest = {
    phone,
    password: values.password,
    firstName,
    lastName,
    country,
  };
  const email = values.email?.trim();
  if (email) request.email = email;
  const nationalId = values.nationalId?.trim();
  if (nationalId) request.nationalId = nationalId;
  return request;
}

export function buildLoginRequest(values: { email: string; password: string }): LoginRequest {
  return { identifier: values.email.trim(), password: values.password };
}
