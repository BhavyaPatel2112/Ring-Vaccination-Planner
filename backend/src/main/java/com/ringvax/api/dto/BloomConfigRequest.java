package com.ringvax.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Bloom filter tuning: bits m, hash functions k, and the number of keys n to load (default 10,000). */
public record BloomConfigRequest(
        @NotNull(message = "m is required")
        @Min(value = 64, message = "m must be at least 64 bits")
        @Max(value = 100_000_000, message = "m must be at most 100,000,000 bits") Long m,
        @NotNull(message = "k is required")
        @Min(value = 1, message = "k must be at least 1")
        @Max(value = 32, message = "k must be at most 32") Integer k,
        @Min(value = 1, message = "n must be at least 1")
        @Max(value = 1_000_000, message = "n must be at most 1,000,000") Integer n,
        Long seed) {
}
