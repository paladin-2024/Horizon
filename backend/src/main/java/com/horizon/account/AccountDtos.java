package com.horizon.account;

import com.horizon.common.money.Money;
import com.horizon.linking.InstitutionView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Wire shapes for the account endpoints. Package-private: no other module sends or reads these. */
final class AccountDtos {

    record CreateAccountRequest(
            @NotNull UUID institutionId,
            @NotBlank String provider,
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String accountMask,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull Long openingBalanceMinor) {
    }

    record AccountResponse(
            UUID id,
            InstitutionView institution,
            String provider,
            String displayName,
            String accountMask,
            Money balance,
            Instant balanceAsOf,
            String status) {
    }

    /** One total per currency, sorted by currency code. Never a cross-currency sum. */
    record AccountListResponse(List<AccountResponse> items, List<Money> totals) {
    }

    private AccountDtos() {
    }
}
