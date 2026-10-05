package com.horizon.linking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds the institution catalogue once the context is up. Idempotent: reruns write nothing. */
@Component
class InstitutionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSeeder.class);

    private final InstitutionService institutionService;

    InstitutionSeeder(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int written = institutionService.seed();
        log.info("Institution seed complete: {} of {} rows written", written, InstitutionSeedData.ALL.size());
    }
}
