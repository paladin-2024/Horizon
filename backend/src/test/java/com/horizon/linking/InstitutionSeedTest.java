package com.horizon.linking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InstitutionSeedTest {

    @Autowired
    private InstitutionService institutionService;

    @Test
    void seedRanOnStartup() {
        assertThat(institutionService.list("UG")).isNotEmpty();
        assertThat(institutionService.list("CD")).isNotEmpty();
    }

    @Test
    void seedIsIdempotent() {
        int before = institutionService.list(null).size();

        int writtenOnSecondRun = institutionService.seed();

        assertThat(writtenOnSecondRun).isZero();
        assertThat(institutionService.list(null)).hasSize(before);
    }

    @Test
    void seedDataHasNoDuplicatesAndWellFormedCodes() {
        List<String> keys = InstitutionSeedData.ALL.stream().map(s -> s.country() + "/" + s.code()).toList();

        assertThat(keys).doesNotHaveDuplicates();
        assertThat(InstitutionSeedData.ALL).allSatisfy(seed -> {
            assertThat(seed.code()).matches("^[a-z0-9-]+$");
            assertThat(seed.country()).isIn("UG", "CD");
            assertThat(seed.name()).isNotBlank();
            assertThat(seed.type()).isNotNull();
        });
    }

    @Test
    void containsTheMobileMoneyWalletsWeCareAbout() {
        assertThat(institutionService.list("UG"))
                .filteredOn(i -> i.type() == InstitutionType.MOBILE_MONEY)
                .extracting(InstitutionView::code)
                .containsExactlyInAnyOrder("mtn-momo-ug", "airtel-money-ug");

        assertThat(institutionService.list("CD"))
                .filteredOn(i -> i.type() == InstitutionType.MOBILE_MONEY)
                .extracting(InstitutionView::code)
                .containsExactlyInAnyOrder("mpesa-cd", "orange-money-cd", "airtel-money-cd", "africell-money-cd");
    }

    @Test
    void containsTheAnchorBanks() {
        assertThat(institutionService.list("UG")).extracting(InstitutionView::code)
                .contains("stanbic-ug", "centenary-ug", "absa-ug", "dfcu-ug", "equity-ug");
        assertThat(institutionService.list("CD")).extracting(InstitutionView::code)
                .contains("rawbank-cd", "tmb-cd", "equity-bcdc-cd", "ecobank-cd");
    }

    @Test
    void listWithoutACountryReturnsEverySeededRow() {
        assertThat(institutionService.list(null)).hasSizeGreaterThanOrEqualTo(InstitutionSeedData.ALL.size());
    }
}
