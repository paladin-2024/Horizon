package com.horizon.account;

import com.horizon.common.audit.AuditLog;
import com.horizon.common.error.ApiException;
import com.horizon.common.money.Money;
import com.horizon.linking.InstitutionService;
import com.horizon.linking.LinkedAccountDraft;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The account module's public API.
 *
 * <p>Other modules use only {@link #get(UUID, UUID)} and {@code applyBalanceDelta}; {@link #create} and
 * {@link #list} exist for this module's own controller.
 */
@Service
public class LinkedAccountService {

    /** One account, one currency. Horizon serves Uganda and DR Congo, plus USD accounts in both. */
    static final Set<String> SUPPORTED_CURRENCIES = Set.of("UGX", "CDF", "USD");

    private final LinkedAccountRepository repository;
    private final InstitutionService institutionService;
    private final AuditLog auditLog;

    LinkedAccountService(LinkedAccountRepository repository, InstitutionService institutionService, AuditLog auditLog) {
        this.repository = repository;
        this.institutionService = institutionService;
        this.auditLog = auditLog;
    }

    /**
     * @throws ApiException 404 {@code account_not_found} if the account does not exist or belongs to another user
     */
    @Transactional(readOnly = true)
    public LinkedAccountView get(UUID userId, UUID accountId) {
        return repository.findByIdAndUserId(accountId, userId)
                .map(LinkedAccountService::toView)
                .orElseThrow(() -> ApiException.notFound("account_not_found", "Account not found"));
    }

    /** Every account the user linked, oldest first. */
    @Transactional(readOnly = true)
    public List<LinkedAccountView> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(LinkedAccountService::toView)
                .toList();
    }

    /**
     * @throws ApiException 422 {@code institution_not_found}, 422 {@code currency_unsupported},
     *                      409 {@code account_already_linked}
     */
    @Transactional
    public LinkedAccountView create(UUID userId, LinkedAccountDraft draft) {
        if (institutionService.findById(draft.institutionId()).isEmpty()) {
            throw ApiException.unprocessable("institution_not_found", "Unknown institution");
        }
        if (!SUPPORTED_CURRENCIES.contains(draft.openingBalance().currency())) {
            throw ApiException.unprocessable("currency_unsupported", "Currency must be one of UGX, CDF, USD");
        }
        if (repository.existsByUserIdAndInstitutionIdAndAccountMask(
                userId, draft.institutionId(), draft.accountMask())) {
            throw ApiException.conflict("account_already_linked", "This account is already linked");
        }

        LinkedAccount account = new LinkedAccount(userId, draft.institutionId(), draft.provider(),
                draft.displayName(), draft.accountMask(), draft.openingBalance(), Instant.now());
        try {
            repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent links raced past the exists() check; the unique constraint decided.
            throw ApiException.conflict("account_already_linked", "This account is already linked");
        }

        auditLog.record(userId, "account.linked", Map.<String, Object>of(
                "accountId", account.getId().toString(),
                "institutionId", draft.institutionId().toString(),
                "provider", draft.provider().name(),
                "currency", draft.openingBalance().currency()));

        return toView(account);
    }

    private static LinkedAccountView toView(LinkedAccount account) {
        Money balance = account.getBalance();
        return new LinkedAccountView(
                account.getId(),
                account.getUserId(),
                account.getInstitutionId(),
                account.getProvider().name(),
                account.getDisplayName(),
                account.getAccountMask(),
                balance.currency(),
                balance.amountMinor(),
                account.getBalanceAsOf(),
                account.getStatus().name());
    }
}
