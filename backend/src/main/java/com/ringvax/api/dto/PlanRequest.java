package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * A plan request. Method defaults to APPROX, depth to 2 and seed to 2026;
 * leave budget out for no limit.
 */
public record PlanRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth,
        String method,
        @Min(value = 0, message = "budget must be 0 or more") Integer budget,
        Long seed) {
}
