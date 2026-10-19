package com.horizon.linking;

import com.horizon.account.LinkedAccountView;
import java.util.Set;
import java.util.UUID;

/**
 * How Horizon talks to a source of account data. v1 ships {@link ManualProvider}; a future mobile money or
 * bank API provider implements {@link #sync} for real without any other module changing.
 */
public interface BankProvider {

    ProviderType type();

    Set<ProviderCapability> capabilities();

    /** Turns a user's link request into the account Horizon should create. Never persists anything. */
    LinkedAccountDraft link(UUID userId, LinkRequest request);

    /** Pulls fresh balance and transaction data. Providers without that capability return a no-op result. */
    SyncResult sync(LinkedAccountView account);
}
