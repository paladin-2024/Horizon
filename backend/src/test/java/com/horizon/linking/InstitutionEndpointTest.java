package com.horizon.linking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class InstitutionEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/institutions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsUgandanInstitutions() throws Exception {
        mockMvc.perform(get("/api/v1/institutions").param("country", "UG").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.items", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.items[?(@.code == 'mtn-momo-ug')].name").value("MTN Mobile Money"))
                .andExpect(jsonPath("$.items[?(@.code == 'mtn-momo-ug')].type").value("MOBILE_MONEY"))
                .andExpect(jsonPath("$.items[?(@.code == 'stanbic-ug')].type").value("BANK"))
                .andExpect(jsonPath("$.items[*].country", Matchers.everyItem(Matchers.is("UG"))));
    }

    @Test
    void acceptsALowercaseCountry() throws Exception {
        mockMvc.perform(get("/api/v1/institutions").param("country", "cd").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].country", Matchers.everyItem(Matchers.is("CD"))));
    }

    @Test
    void rejectsACountryWeDoNotServe() throws Exception {
        mockMvc.perform(get("/api/v1/institutions").param("country", "KE").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_country"));
    }

    @Test
    void withoutACountryReturnsBothCountries() throws Exception {
        mockMvc.perform(get("/api/v1/institutions").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == 'stanbic-ug')]", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[?(@.code == 'rawbank-cd')]", Matchers.hasSize(1)));
    }
}
