package com.horizon.linking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institutions")
class InstitutionController {

    private final InstitutionService institutionService;

    InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    InstitutionListResponse list(@RequestParam(name = "country", required = false) String country) {
        return new InstitutionListResponse(institutionService.list(country), null);
    }

    /** The catalogue is small and fixed, so the cursor is always null. */
    record InstitutionListResponse(List<InstitutionView> items, String nextCursor) {
    }
}
