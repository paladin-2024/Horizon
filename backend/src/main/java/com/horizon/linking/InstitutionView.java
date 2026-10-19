package com.horizon.linking;

import java.util.UUID;

/** Public read model for an institution. Serialised directly as the API's Institution object. */
public record InstitutionView(UUID id, String name, InstitutionType type, String country, String code) {
}
