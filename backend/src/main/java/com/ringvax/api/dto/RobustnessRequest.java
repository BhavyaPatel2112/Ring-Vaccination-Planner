package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * "What if contact tracing missed some contacts?": the planner sees only part of the
 * contacts, the outbreak spreads along all of them. Default shares: 0, 10, ..., 50%.
 */
public record RobustnessRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth,
        @DecimalMin(value = "0.0", message = "transmission must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "transmission must be between 0 and 1") Double transmission,
        @Min(value = 1, message = "recoverySteps must be between 1 and 60")
        @Max(value = 60, message = "recoverySteps must be between 1 and 60") Integer recoverySteps,
        @Min(value = 1, message = "trials must be between 1 and 1000")
        @Max(value = 1000, message = "trials must be between 1 and 1000") Integer trials,
        @DecimalMin(value = "0.0", message = "efficacy must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "efficacy must be between 0 and 1") Double efficacy,
        @Size(max = 10, message = "at most 10 missing shares") List<
                @DecimalMin(value = "0.0", message = "missing shares must be between 0 and 0.9")
                @DecimalMax(value = "0.9", message = "missing shares must be between 0 and 0.9") Double> missingShares,
        Long seed) {
}
