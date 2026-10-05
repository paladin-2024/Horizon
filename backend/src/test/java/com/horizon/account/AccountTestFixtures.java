package com.horizon.account;

import com.horizon.common.money.Money;
import com.horizon.linking.InstitutionService;
import com.horizon.linking.InstitutionView;
import com.horizon.linking.LinkedAccountDraft;
import com.horizon.linking.ProviderType;
import java.util.UUID;

/** Shared builders so account tests stay short. Test scope only. */
final class AccountTestFixtures {

    static LinkedAccountDraft draft(UUID institutionId, String mask, long amountMinor, String currency) {
        return new LinkedAccountDraft(institutionId, ProviderType.MANUAL, "Salary account", mask,
                Money.of(amountMinor, currency));
    }

    /** The id of a seeded institution, looked up by Horizon's own slug. */
    static UUID seededInstitutionId(InstitutionService institutionService, String country, String code) {
        return institutionService.list(country).stream()
                .filter(institution -> institution.code().equals(code))
                .map(InstitutionView::id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data is missing " + country + "/" + code));
    }

    private AccountTestFixtures() {
    }
}
