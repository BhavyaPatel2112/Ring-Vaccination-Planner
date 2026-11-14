package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * A plan request. Method defaults to APPROX, depth to 2 and seed to 2026;
 * leave budget out for no limit.
 * Optional contact settings (feature E): minDurationMinutes and windowDays leave out short or old contacts.
 */
public record PlanRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth,
        String method,
        @Min(value = 0, message = "budget must be 0 or more") Integer budget,
        Long seed,
        @Min(value = 0, message = "minDurationMinutes must be between 0 and 1440")
        @Max(value = 1440, message = "minDurationMinutes must be between 0 and 1440") Integer minDurationMinutes,
        @Min(value = 1, message = "windowDays must be between 1 and 365")
        @Max(value = 365, message = "windowDays must be between 1 and 365") Integer windowDays) {
}
