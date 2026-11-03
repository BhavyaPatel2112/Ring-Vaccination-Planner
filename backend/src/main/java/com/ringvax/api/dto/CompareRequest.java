package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** Cases and depth to compare every method on. */
public record CompareRequest(
        @NotEmpty(message = "at least one confirmed case is required") List<@NotBlank String> caseIds,
        @Min(1) @Max(5) Integer depth) {
}
