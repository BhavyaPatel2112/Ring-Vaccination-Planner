package com.ringvax.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Settings for a synthetic network. A missing seed defaults to 2026 and cases (confirmed cases to mark) to 2. */
public record GenerateRequest(
        @NotNull @Min(2) @Max(2_000_000) Integer population,
        @NotNull @Positive @DecimalMax("200") Double avgContacts,
        Long seed,
        String name,
        @Min(value = 0, message = "cases must be between 0 and 20")
        @Max(value = 20, message = "cases must be between 0 and 20") Integer cases) {
}
