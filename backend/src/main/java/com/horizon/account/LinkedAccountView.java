package com.horizon.account;

import java.time.Instant;
import java.util.UUID;

/** Public read model for a linked account. This is the account module's cross-module currency. */
public record LinkedAccountView(
        UUID id,
        UUID userId,
        UUID institutionId,
        String provider,
        String displayName,
        String accountMask,
        String currency,
        long currentBalanceMinor,
        Instant balanceAsOf,
        String status) {
}
