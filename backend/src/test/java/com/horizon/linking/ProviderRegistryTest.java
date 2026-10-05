package com.horizon.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.account.LinkedAccountView;
import com.horizon.common.error.ApiException;
import com.horizon.common.money.Money;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProviderRegistryTest {

    @Autowired
    private ProviderRegistry registry;

    @Test
    void resolvesTheManualProvider() {
        BankProvider provider = registry.get(ProviderType.MANUAL);

        assertThat(provider.type()).isEqualTo(ProviderType.MANUAL);
        assertThat(provider.capabilities())
                .containsExactlyInAnyOrder(ProviderCapability.LINK, ProviderCapability.STATEMENT_IMPORT);
    }

    @Test
    void hasNoProviderForCsvYet() {
        assertThat(registry.find(ProviderType.CSV)).isEmpty();

        assertThatThrownBy(() -> registry.get(ProviderType.CSV))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("CSV");
    }

    @Test
    void manualProviderBuildsADraftFromTheRequest() {
        UUID institutionId = UUID.randomUUID();
        LinkRequest request = new LinkRequest(institutionId, "  Salary account  ", "4821", "UGX", 250_000L);

        LinkedAccountDraft draft = registry.get(ProviderType.MANUAL).link(UUID.randomUUID(), request);

        assertThat(draft.institutionId()).isEqualTo(institutionId);
        assertThat(draft.provider()).isEqualTo(ProviderType.MANUAL);
        assertThat(draft.displayName()).isEqualTo("Salary account");
        assertThat(draft.accountMask()).isEqualTo("4821");
        assertThat(draft.openingBalance()).isEqualTo(Money.of(250_000L, "UGX"));
    }

    @Test
    void manualProviderSyncDoesNothing() {
        LinkedAccountView account = new LinkedAccountView(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "MANUAL", "Salary account", "4821", "UGX", 250_000L, Instant.parse("2026-01-01T00:00:00Z"), "ACTIVE");

        SyncResult result = registry.get(ProviderType.MANUAL).sync(account);

        assertThat(result.performed()).isFalse();
        assertThat(result.transactionsFetched()).isZero();
    }
}
