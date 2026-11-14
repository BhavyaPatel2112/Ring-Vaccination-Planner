package com.ringvax.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Deadlines and monitoring for a plan (features B and C), with the same plan
 * settings as {@link PlanRequest}. Defaults: a 72-hour vaccination window, a
 * 21-day incubation period, and "now" = the start of the latest confirmation day.
 */
public record FollowUpRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth,
        String method,
        @Min(value = 0, message = "budget must be 0 or more") Integer budget,
        Long seed,
        @Min(value = 0, message = "minDurationMinutes must be between 0 and 1440")
        @Max(value = 1440, message = "minDurationMinutes must be between 0 and 1440") Integer minDurationMinutes,
        @Min(value = 1, message = "windowDays must be between 1 and 365")
        @Max(value = 365, message = "windowDays must be between 1 and 365") Integer windowDays,
        @Min(value = 1, message = "windowHours must be between 1 and 720")
        @Max(value = 720, message = "windowHours must be between 1 and 720") Integer windowHours,
        @Min(value = 1, message = "incubationDays must be between 1 and 60")
        @Max(value = 60, message = "incubationDays must be between 1 and 60") Integer incubationDays,
        LocalDateTime asOf) {
}
