package com.horizon.account;

import com.horizon.account.AccountDtos.AccountResponse;
import com.horizon.account.AccountDtos.CreateAccountRequest;
import com.horizon.common.error.ApiException;
import com.horizon.common.idempotency.Idempotent;
import com.horizon.common.money.Money;
import com.horizon.common.security.CurrentUser;
import com.horizon.linking.BankProvider;
import com.horizon.linking.InstitutionService;
import com.horizon.linking.InstitutionView;
import com.horizon.linking.LinkRequest;
import com.horizon.linking.LinkedAccountDraft;
import com.horizon.linking.ProviderRegistry;
import com.horizon.linking.ProviderType;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final LinkedAccountService accountService;
    private final InstitutionService institutionService;
    private final ProviderRegistry providerRegistry;

    AccountController(LinkedAccountService accountService, InstitutionService institutionService,
            ProviderRegistry providerRegistry) {
        this.accountService = accountService;
        this.institutionService = institutionService;
        this.providerRegistry = providerRegistry;
    }

    @PostMapping
    @Idempotent
    ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        UUID userId = CurrentUser.id();
        ProviderType providerType = parseProvider(request.provider());
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        if (!LinkedAccountService.SUPPORTED_CURRENCIES.contains(currency)) {
            throw ApiException.unprocessable("currency_unsupported", "Currency must be one of UGX, CDF, USD");
        }

        BankProvider provider = providerRegistry.get(providerType);
        LinkedAccountDraft draft = provider.link(userId, new LinkRequest(
                request.institutionId(),
                request.displayName(),
                request.accountMask(),
                currency,
                request.openingBalanceMinor()));

        LinkedAccountView view = accountService.create(userId, draft);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + view.id())).body(toResponse(view));
    }

    private static ProviderType parseProvider(String raw) {
        try {
            return ProviderType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessable("provider_unsupported", "Unsupported provider: " + raw);
        }
    }

    private AccountResponse toResponse(LinkedAccountView view) {
        return toResponse(view, institutionService.findById(view.institutionId()).orElse(null));
    }

    static AccountResponse toResponse(LinkedAccountView view, InstitutionView institution) {
        return new AccountResponse(
                view.id(),
                institution,
                view.provider(),
                view.displayName(),
                view.accountMask(),
                Money.of(view.currentBalanceMinor(), view.currency()),
                view.balanceAsOf(),
                view.status());
    }
}
