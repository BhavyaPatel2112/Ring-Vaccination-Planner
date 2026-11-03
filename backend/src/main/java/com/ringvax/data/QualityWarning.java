package com.ringvax.data;

/**
 * A sign that the contact tracing data may be incomplete or wrong. These are
 * warnings for the outbreak team to look into, not errors: the plan is still built.
 *
 * @param code     short machine-readable code such as {@code CASE_WITHOUT_CONTACTS}
 * @param personId person the warning is about, or {@code null} for a summary warning
 * @param message  human-readable explanation
 */
public record QualityWarning(String code, String personId, String message) {
}
