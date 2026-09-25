package com.horizon.common.error;

/** One entry of the {@code errors} array in a {@code validation_failed} response. */
public record FieldIssue(String field, String message) {
}
