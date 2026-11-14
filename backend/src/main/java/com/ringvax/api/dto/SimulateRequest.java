package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Outbreak simulation settings. Defaults: transmission 0.08 per contact per
 * step, infectious for 4 steps, 200 trials, efficacy 1 (full protection), seed 2026.
 */
public record SimulateRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth,
        @DecimalMin(value = "0.0", message = "transmission must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "transmission must be between 0 and 1") Double transmission,
        @Min(value = 1, message = "recoverySteps must be between 1 and 60")
        @Max(value = 60, message = "recoverySteps must be between 1 and 60") Integer recoverySteps,
        @Min(value = 1, message = "trials must be between 1 and 2000")
        @Max(value = 2000, message = "trials must be between 1 and 2000") Integer trials,
        @DecimalMin(value = "0.0", message = "efficacy must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "efficacy must be between 0 and 1") Double efficacy,
        @Min(value = 0, message = "budget must be 0 or more") Integer budget,
        Long seed) {
}
