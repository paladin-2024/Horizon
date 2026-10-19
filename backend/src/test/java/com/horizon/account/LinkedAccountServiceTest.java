package com.horizon.account;

import static com.horizon.account.AccountTestFixtures.draft;
import static com.horizon.account.AccountTestFixtures.seededInstitutionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.error.ApiException;
import com.horizon.common.money.Money;
import com.horizon.linking.InstitutionService;
import com.horizon.linking.LinkedAccountDraft;
import com.horizon.linking.ProviderType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LinkedAccountServiceTest {

    @Autowired
    private LinkedAccountService accountService;

    @Autowired
    private InstitutionService institutionService;

    private UUID stanbic() {
        return seededInstitutionId(institutionService, "UG", "stanbic-ug");
    }

    private UUID mtn() {
        return seededInstitutionId(institutionService, "UG", "mtn-momo-ug");
    }

    @Test
    void createsAnAccountFromADraft() {
        UUID userId = UUID.randomUUID();

        LinkedAccountView view = accountService.create(userId, draft(stanbic(), "4821", 250_000L, "UGX"));

        assertThat(view.id()).isNotNull();
        assertThat(view.userId()).isEqualTo(userId);
        assertThat(view.institutionId()).isEqualTo(stanbic());
        assertThat(view.provider()).isEqualTo("MANUAL");
        assertThat(view.displayName()).isEqualTo("Salary account");
        assertThat(view.accountMask()).isEqualTo("4821");
        assertThat(view.currency()).isEqualTo("UGX");
        assertThat(view.currentBalanceMinor()).isEqualTo(250_000L);
        assertThat(view.balanceAsOf()).isNotNull();
        assertThat(view.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getsTheOwnersAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = accountService.create(userId, draft(stanbic(), "4821", 100L, "UGX")).id();

        assertThat(accountService.get(userId, accountId).id()).isEqualTo(accountId);
    }

    @Test
    void anotherUsersAccountIsNotFound() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID accountId = accountService.create(owner, draft(stanbic(), "4821", 100L, "UGX")).id();

        assertThatThrownBy(() -> accountService.get(intruder, accountId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(api.getCode()).isEqualTo("account_not_found");
                });
    }

    @Test
    void aMissingAccountIsNotFound() {
        assertThatThrownBy(() -> accountService.get(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("account_not_found"));
    }

    @Test
    void listsOnlyTheUsersOwnAccountsOldestFirst() {
        UUID userId = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID first = accountService.create(userId, draft(stanbic(), "1111", 100L, "UGX")).id();
        UUID second = accountService.create(userId, draft(mtn(), "2222", 200L, "UGX")).id();
        accountService.create(other, draft(stanbic(), "3333", 300L, "UGX"));

        List<LinkedAccountView> accounts = accountService.list(userId);

        assertThat(accounts).extracting(LinkedAccountView::id).containsExactly(first, second);
    }

    @Test
    void listIsEmptyForAUserWithNoAccounts() {
        assertThat(accountService.list(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsLinkingTheSameInstitutionAndMaskTwice() {
        UUID userId = UUID.randomUUID();
        accountService.create(userId, draft(stanbic(), "4821", 100L, "UGX"));

        assertThatThrownBy(() -> accountService.create(userId, draft(stanbic(), "4821", 900L, "UGX")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("account_already_linked");
                });
    }

    @Test
    void rejectsAnUnknownInstitution() {
        assertThatThrownBy(() -> accountService.create(UUID.randomUUID(),
                draft(UUID.randomUUID(), "4821", 100L, "UGX")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(api.getCode()).isEqualTo("institution_not_found");
                });
    }

    @Test
    void rejectsACurrencyHorizonDoesNotSupport() {
        LinkedAccountDraft kenyanShillings = new LinkedAccountDraft(stanbic(), ProviderType.MANUAL,
                "Salary account", "4821", Money.of(100L, "KES"));

        assertThatThrownBy(() -> accountService.create(UUID.randomUUID(), kenyanShillings))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("currency_unsupported"));
    }

    @Test
    void acceptsANegativeOpeningBalance() {
        UUID userId = UUID.randomUUID();

        LinkedAccountView view = accountService.create(userId, draft(stanbic(), "4821", -5_000L, "CDF"));

        assertThat(view.currentBalanceMinor()).isEqualTo(-5_000L);
        assertThat(view.currency()).isEqualTo("CDF");
    }
}
