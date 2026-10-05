import { apiFetch } from "./client";

export const listAccounts = () => apiFetch<AccountListResponse>("/accounts");

export const getAccount = (id: string) => apiFetch<ApiAccount>(`/accounts/${id}`);

export const createAccount = (request: CreateAccountRequest) =>
  apiFetch<ApiAccount>("/accounts", { method: "POST", body: request });

/** `country` narrows to "UG" or "CD"; omit it to list every seeded institution. */
export const listInstitutions = (country?: InstitutionCountry) =>
  apiFetch<InstitutionListResponse>(
    country ? `/institutions?country=${country}` : "/institutions"
  );
