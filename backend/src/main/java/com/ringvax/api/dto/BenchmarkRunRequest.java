package com.ringvax.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * A quick live benchmark: {@code experiment} is approx-vs-greedy, dsu or bloom;
 * {@code size} is the largest input (contacts or keys), at most 200,000.
 */
public record BenchmarkRunRequest(
        @NotBlank(message = "experiment is required") String experiment,
        @Min(value = 100, message = "size must be at least 100")
        @Max(value = 200_000, message = "size must be at most 200,000 for a live run") Integer size,
        Long seed) {
}
