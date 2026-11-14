package com.ringvax.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** People to check against the vaccination registry before giving a dose. */
public record RegistryCheckRequest(
        @NotBlank(message = "networkId is required") String networkId,
        @NotEmpty(message = "at least one person id is required")
        @Size(max = 5000, message = "at most 5000 people can be checked at once") List<@NotBlank String> personIds) {
}
