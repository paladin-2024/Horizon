package com.horizon.linking;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    Optional<Institution> findByCountryAndCode(String country, String code);

    List<Institution> findAllByCountryOrderByTypeAscNameAsc(String country);

    List<Institution> findAllByOrderByCountryAscTypeAscNameAsc();

    List<Institution> findAllByIdIn(Collection<UUID> ids);
}
