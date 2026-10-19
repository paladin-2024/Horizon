package com.horizon.account;

import static com.horizon.account.AccountTestFixtures.seededInstitutionId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreateAccountEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionService institutionService;

    private String body(UUID institutionId, String provider, String mask, String currency, long opening) {
        return """
                {"institutionId":"%s","provider":"%s","displayName":"Salary account","accountMask":"%s",\
                "currency":"%s","openingBalanceMinor":%d}""".formatted(institutionId, provider, mask, currency, opening);
    }

    private MockHttpServletRequestBuilder createAs(UUID userId, String json) {
        return post("/api/v1/accounts")
                .with(TestAuth.asUser(userId))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Horizon-Client", "web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private UUID stanbic() {
        return seededInstitutionId(institutionService, "UG", "stanbic-ug");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-Horizon-Client", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(stanbic(), "MANUAL", "4821", "UGX", 250_000L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linksAManualAccount() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(stanbic(), "MANUAL", "4821", "UGX", 250_000L)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/v1/accounts/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.institution.code").value("stanbic-ug"))
                .andExpect(jsonPath("$.institution.name").value("Stanbic Bank Uganda"))
                .andExpect(jsonPath("$.institution.type").value("BANK"))
                .andExpect(jsonPath("$.institution.country").value("UG"))
                .andExpect(jsonPath("$.provider").value("MANUAL"))
                .andExpect(jsonPath("$.displayName").value("Salary account"))
                .andExpect(jsonPath("$.accountMask").value("4821"))
                .andExpect(jsonPath("$.balance.amountMinor").value(250_000))
                .andExpect(jsonPath("$.balance.currency").value("UGX"))
                .andExpect(jsonPath("$.balanceAsOf").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void requiresAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(TestAuth.asUser(UUID.randomUUID()))
                        .header("X-Horizon-Client", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(stanbic(), "MANUAL", "4821", "UGX", 250_000L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("idempotency_key_required"));
    }

    @Test
    void replaysTheSameKeyAndBody() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        String json = body(stanbic(), "MANUAL", "4821", "UGX", 250_000L);

        String first = mockMvc.perform(post("/api/v1/accounts").with(TestAuth.asUser(userId))
                        .header("Idempotency-Key", key).header("X-Horizon-Client", "web")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replay = mockMvc.perform(post("/api/v1/accounts").with(TestAuth.asUser(userId))
                        .header("Idempotency-Key", key).header("X-Horizon-Client", "web")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(replay).isEqualTo(first);
    }

    @Test
    void rejectsADuplicateLink() throws Exception {
        UUID userId = UUID.randomUUID();
        String json = body(stanbic(), "MANUAL", "4821", "UGX", 250_000L);
        mockMvc.perform(createAs(userId, json)).andExpect(status().isCreated());

        mockMvc.perform(createAs(userId, json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_already_linked"));
    }

    @Test
    void rejectsAnUnknownInstitution() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(UUID.randomUUID(), "MANUAL", "4821", "UGX", 1L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("institution_not_found"));
    }

    @Test
    void rejectsAProviderWithNoImplementation() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(stanbic(), "CSV", "4821", "UGX", 1L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("provider_unsupported"));
    }

    @Test
    void rejectsAnUnknownProviderName() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(stanbic(), "PLAID", "4821", "UGX", 1L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("provider_unsupported"));
    }

    @Test
    void rejectsACurrencyHorizonDoesNotSupport() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(stanbic(), "MANUAL", "4821", "KES", 1L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("currency_unsupported"));
    }

    @Test
    void rejectsAMaskThatIsNotFourDigits() throws Exception {
        mockMvc.perform(createAs(UUID.randomUUID(), body(stanbic(), "MANUAL", "48", "UGX", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[*].field").value(Matchers.hasItem("accountMask")));
    }

    @Test
    void rejectsABlankDisplayName() throws Exception {
        String json = """
                {"institutionId":"%s","provider":"MANUAL","displayName":"   ","accountMask":"4821",\
                "currency":"UGX","openingBalanceMinor":1}""".formatted(stanbic());

        mockMvc.perform(createAs(UUID.randomUUID(), json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }
}
