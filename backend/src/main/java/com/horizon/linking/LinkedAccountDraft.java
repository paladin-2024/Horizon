package com.horizon.linking;

import com.horizon.common.money.Money;
import java.util.UUID;

/** What a provider decided the new account should look like, before the account module persists it. */
public record LinkedAccountDraft(
        UUID institutionId,
        ProviderType provider,
        String displayName,
        String accountMask,
        Money openingBalance) {
}
