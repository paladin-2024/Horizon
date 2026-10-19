package com.horizon.account;

import static com.horizon.account.AccountTestFixtures.draft;
import static com.horizon.account.AccountTestFixtures.seededInstitutionId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.linking.InstitutionService;
import com.horizon.support.TestAuth;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountReadEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private LinkedAccountService accountService;

    private UUID institution(String country, String code) {
        return seededInstitutionId(institutionService, country, code);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void listsAnEmptyPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totals", Matchers.hasSize(0)));
    }

    @Test
    void listsAccountsWithOneTotalPerCurrencyAndNoCrossCurrencySum() throws Exception {
        UUID userId = UUID.randomUUID();
        accountService.create(userId, draft(institution("UG", "stanbic-ug"), "1111", 250_000L, "UGX"));
        accountService.create(userId, draft(institution("UG", "mtn-momo-ug"), "2222", 50_000L, "UGX"));
        accountService.create(userId, draft(institution("CD", "rawbank-cd"), "3333", 120_00L, "CDF"));
        accountService.create(userId, draft(institution("UG", "absa-ug"), "4444", 1_000L, "USD"));

        mockMvc.perform(get("/api/v1/accounts").with(TestAuth.asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", Matchers.hasSize(4)))
                .andExpect(jsonPath("$.items[0].accountMask").value("1111"))
                .andExpect(jsonPath("$.items[0].institution.code").value("stanbic-ug"))
                .andExpect(jsonPath("$.items[3].accountMask").value("4444"))
                .andExpect(jsonPath("$.totals", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.totals[0].currency").value("CDF"))
                .andExpect(jsonPath("$.totals[0].amountMinor").value(12_000))
                .andExpect(jsonPath("$.totals[1].currency").value("UGX"))
                .andExpect(jsonPath("$.totals[1].amountMinor").value(300_000))
                .andExpect(jsonPath("$.totals[2].currency").value("USD"))
                .andExpect(jsonPath("$.totals[2].amountMinor").value(1_000));
    }

    @Test
    void listShowsOnlyTheCallersAccounts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        accountService.create(userId, draft(institution("UG", "stanbic-ug"), "1111", 100L, "UGX"));
        accountService.create(stranger, draft(institution("UG", "stanbic-ug"), "9999", 999L, "UGX"));

        mockMvc.perform(get("/api/v1/accounts").with(TestAuth.asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].accountMask").value("1111"));
    }

    @Test
    void getsOneAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = accountService.create(userId,
                draft(institution("UG", "stanbic-ug"), "4821", 250_000L, "UGX")).id();

        mockMvc.perform(get("/api/v1/accounts/" + accountId).with(TestAuth.asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.institution.code").value("stanbic-ug"))
                .andExpect(jsonPath("$.balance.amountMinor").value(250_000))
                .andExpect(jsonPath("$.balance.currency").value("UGX"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anotherUsersAccountReturns404() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID accountId = accountService.create(owner,
                draft(institution("UG", "stanbic-ug"), "4821", 250_000L, "UGX")).id();

        mockMvc.perform(get("/api/v1/accounts/" + accountId).with(TestAuth.asUser(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("account_not_found"));
    }

    @Test
    void aMissingAccountReturns404WithTheSameBody() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID()).with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("account_not_found"));
    }
}
