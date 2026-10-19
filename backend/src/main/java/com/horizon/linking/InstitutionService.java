package com.horizon.linking;

import com.horizon.common.error.ApiException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The institution catalogue. This is the public API of the linking module for institution reads. */
@Service
public class InstitutionService {

    private static final List<String> COUNTRIES = List.of("UG", "CD");

    private final InstitutionRepository repository;

    InstitutionService(InstitutionRepository repository) {
        this.repository = repository;
    }

    /**
     * @param country ISO 3166-1 alpha-2, case-insensitive; null or blank returns every country
     * @throws ApiException 400 {@code invalid_country} when the country is not one Horizon serves
     */
    @Transactional(readOnly = true)
    public List<InstitutionView> list(String country) {
        if (country == null || country.isBlank()) {
            return repository.findAllByOrderByCountryAscTypeAscNameAsc().stream()
                    .map(InstitutionService::toView)
                    .toList();
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRIES.contains(normalized)) {
            throw ApiException.badRequest("invalid_country", "country must be one of UG, CD");
        }
        return repository.findAllByCountryOrderByTypeAscNameAsc(normalized).stream()
                .map(InstitutionService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<InstitutionView> findById(UUID id) {
        return repository.findById(id).map(InstitutionService::toView);
    }

    @Transactional(readOnly = true)
    public Map<UUID, InstitutionView> findAllByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findAllByIdIn(ids).stream()
                .map(InstitutionService::toView)
                .collect(Collectors.toMap(InstitutionView::id, Function.identity()));
    }

    /**
     * Inserts missing seed rows and corrects the name or type of existing ones. Safe to run on every startup.
     *
     * @return how many rows were inserted or updated; zero when the catalogue already matches the seed data
     */
    @Transactional
    public int seed() {
        int written = 0;
        for (InstitutionSeedData.Seed seed : InstitutionSeedData.ALL) {
            Optional<Institution> existing = repository.findByCountryAndCode(seed.country(), seed.code());
            if (existing.isEmpty()) {
                repository.save(new Institution(seed.country(), seed.code(), seed.name(), seed.type()));
                written++;
            } else {
                Institution institution = existing.get();
                if (!institution.getName().equals(seed.name()) || institution.getType() != seed.type()) {
                    institution.rename(seed.name());
                    institution.retype(seed.type());
                    written++;
                }
            }
        }
        return written;
    }

    private static InstitutionView toView(Institution institution) {
        return new InstitutionView(institution.getId(), institution.getName(), institution.getType(),
                institution.getCountry(), institution.getCode());
    }
}
