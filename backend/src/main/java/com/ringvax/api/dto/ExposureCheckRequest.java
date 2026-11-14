package com.ringvax.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A public exposure self-check: either the visits typed in by the person, or a
 * person id whose recorded check-ins are used. Cases default to the network's
 * cases.csv; the infectious window defaults to 14 days before confirmation.
 */
public record ExposureCheckRequest(
        @NotBlank(message = "networkId is required") String networkId,
        String personId,
        @Valid @Size(max = 50, message = "at most 50 visits can be checked at once") List<Visit> visits,
        List<String> caseIds,
        @Min(value = 1, message = "infectiousDays must be at least 1")
        @Max(value = 60, message = "infectiousDays must be at most 60") Integer infectiousDays) {

    public record Visit(
            @NotBlank(message = "venueId is required") String venueId,
            @NotNull(message = "start is required") LocalDateTime start,
            @NotNull(message = "end is required") LocalDateTime end) {
    }
}
