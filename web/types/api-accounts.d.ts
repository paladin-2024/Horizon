// Request and response shapes of the accounts and institutions endpoints under
// /api/v1/accounts and /api/v1/institutions.
// Source of truth: backend/src/main/java/com/horizon/account/AccountDtos.java,
// backend/src/main/java/com/horizon/linking/InstitutionView.java and InstitutionController.java.

declare type InstitutionCountry = "UG" | "CD";
declare type InstitutionType = "BANK" | "MOBILE_MONEY";
declare type AccountProvider = "MANUAL" | "CSV";
declare type AccountStatus = "ACTIVE" | "ARCHIVED";
declare type SupportedCurrency = "UGX" | "CDF" | "USD";

/** Whole-currency amount, stored by the API as integer minor units (e.g. cents). */
declare type Money = {
  amountMinor: number;
  currency: string;
};

declare type ApiInstitution = {
  id: string;
  name: string;
  type: InstitutionType;
  country: InstitutionCountry;
  code: string;
};

/** GET /institutions */
declare type InstitutionListResponse = {
  items: ApiInstitution[];
  nextCursor: string | null;
};

/** POST /accounts request body. */
declare type CreateAccountRequest = {
  institutionId: string;
  provider: AccountProvider;
  displayName: string;
  accountMask: string;
  currency: SupportedCurrency;
  openingBalanceMinor: number;
};

/** A single linked account, as returned by POST/GET /accounts and GET /accounts/{id}. */
declare type ApiAccount = {
  id: string;
  institution: ApiInstitution | null;
  provider: AccountProvider;
  displayName: string;
  accountMask: string;
  balance: Money;
  balanceAsOf: string;
  status: AccountStatus;
};

/** GET /accounts. `totals` is one entry per currency in use, sorted by currency code — never a cross-currency sum. */
declare type AccountListResponse = {
  items: ApiAccount[];
  totals: Money[];
};
