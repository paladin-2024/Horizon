// Request and response shapes of the auth endpoints under /api/v1/auth.
// Source of truth: .superpowers/plans-draft/00-contracts.md (REST JSON shapes).

declare type AuthCountry = "UG" | "CD";

declare type RegisterRequest = {
  phone: string;
  password: string;
  firstName: string;
  lastName: string;
  country: AuthCountry;
  email?: string;
  nationalId?: string;
};

declare type VerifyOtpRequest = {
  phone: string;
  code: string;
};

declare type ResendOtpRequest = {
  phone: string;
};

declare type LoginRequest = {
  /** Phone number (E.164) or email. */
  identifier: string;
  password: string;
};

/** GET /auth/me */
declare type ApiUser = {
  id: string;
  phone: string;
  email: string | null;
  firstName: string;
  lastName: string;
  country: AuthCountry;
  language: "en" | "fr";
};
