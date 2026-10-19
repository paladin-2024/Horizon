package com.horizon.linking;

import com.horizon.account.LinkedAccountView;
import com.horizon.common.money.Money;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** The user types the account in themselves and keeps it up to date with CSV imports. Sync is a no-op. */
@Component
class ManualProvider implements BankProvider {

    @Override
    public ProviderType type() {
        return ProviderType.MANUAL;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.LINK, ProviderCapability.STATEMENT_IMPORT);
    }

    @Override
    public LinkedAccountDraft link(UUID userId, LinkRequest request) {
        return new LinkedAccountDraft(
                request.institutionId(),
                ProviderType.MANUAL,
                request.displayName().trim(),
                request.accountMask(),
                Money.of(request.openingBalanceMinor(), request.currency()));
    }

    @Override
    public SyncResult sync(LinkedAccountView account) {
        return SyncResult.notPerformed();
    }
}
