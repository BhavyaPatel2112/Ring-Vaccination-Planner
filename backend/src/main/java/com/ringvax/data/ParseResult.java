package com.ringvax.data;

import java.util.List;

/**
 * Successfully parsed value plus the warnings produced on the way (rejected rows,
 * merged duplicates, assumed defaults).
 */
public record ParseResult<T>(T value, List<ValidationIssue> warnings) {

    public ParseResult {
        warnings = List.copyOf(warnings);
    }
}
