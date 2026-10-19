package com.horizon.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InstitutionPersistenceTest {

    @Autowired
    private InstitutionRepository repository;

    @Test
    void savesAndReadsBackAnInstitution() {
        String code = "test-bank-" + UUID.randomUUID();
        Institution saved = repository.saveAndFlush(new Institution("UG", code, "Test Bank Uganda", InstitutionType.BANK));

        assertThat(saved.getId()).isNotNull();

        Institution found = repository.findByCountryAndCode("UG", code).orElseThrow();
        assertThat(found.getName()).isEqualTo("Test Bank Uganda");
        assertThat(found.getType()).isEqualTo(InstitutionType.BANK);
        assertThat(found.getCountry()).isEqualTo("UG");
        assertThat(found.getCode()).isEqualTo(code);
    }

    @Test
    void rejectsADuplicateCountryAndCode() {
        String code = "dup-bank-" + UUID.randomUUID();
        repository.saveAndFlush(new Institution("CD", code, "Dup Bank", InstitutionType.BANK));

        assertThatThrownBy(() -> repository.saveAndFlush(new Institution("CD", code, "Dup Bank Again", InstitutionType.BANK)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTheSameCodeInAnotherCountry() {
        // Deliberately BANK, not MOBILE_MONEY: the seed tests assert the exact set of seeded wallets,
        // and rows this test leaves behind live in the same database for the rest of the run.
        String code = "shared-code-" + UUID.randomUUID();
        repository.saveAndFlush(new Institution("UG", code, "Shared UG", InstitutionType.BANK));
        repository.saveAndFlush(new Institution("CD", code, "Shared CD", InstitutionType.BANK));

        List<Institution> both = List.of(
                repository.findByCountryAndCode("UG", code).orElseThrow(),
                repository.findByCountryAndCode("CD", code).orElseThrow());

        assertThat(both).extracting(Institution::getCountry).containsExactly("UG", "CD");
    }
}
