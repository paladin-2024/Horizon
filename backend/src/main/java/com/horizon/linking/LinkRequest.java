package com.horizon.linking;

import java.util.UUID;

/**
 * What the user asked to link. Already validated and normalised by the caller: {@code currency} is an
 * upper-case ISO 4217 code Horizon supports, {@code accountMask} is exactly four digits.
 */
public record LinkRequest(
        UUID institutionId,
        String displayName,
        String accountMask,
        String currency,
        long openingBalanceMinor) {
}
