package com.ringvax.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Settings for a synthetic network. A missing seed defaults to 2026. */
public record GenerateRequest(
        @NotNull @Min(2) @Max(2_000_000) Integer population,
        @NotNull @Positive @DecimalMax("200") Double avgContacts,
        Long seed,
        String name) {
}
