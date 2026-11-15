package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A tiny Bloom filter to draw: a few keys in up to 128 bits, then one query. */
public record BloomDemoRequest(
        @NotNull(message = "m is required")
        @Min(value = 8, message = "m must be between 8 and 128 for the drawing")
        @Max(value = 128, message = "m must be between 8 and 128 for the drawing") Integer m,
        @NotNull(message = "k is required")
        @Min(value = 1, message = "k must be between 1 and 8 for the drawing")
        @Max(value = 8, message = "k must be between 1 and 8 for the drawing") Integer k,
        @NotNull(message = "keys are required")
        @Size(max = 20, message = "at most 20 keys") List<@NotBlank String> keys,
        @NotBlank(message = "query is required") String query) {
}
