package com.ringvax.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** A new contact report. Date and duration are optional. */
public record LiveContactRequest(
        @NotBlank(message = "personA is required") String personA,
        @NotBlank(message = "personB is required") String personB,
        LocalDate date,
        @Min(value = 0, message = "durationMinutes must be 0 or more")
        @Max(value = 10080, message = "durationMinutes must be at most one week") Integer durationMinutes) {
}
