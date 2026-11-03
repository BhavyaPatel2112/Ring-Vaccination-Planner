package com.ringvax.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ringvax.data.QualityWarning;
import com.ringvax.data.ValidationIssue;

/**
 * What the frontend needs to know about a loaded network.
 *
 * @param warnings        rows rejected or merged while reading the files
 * @param qualityWarnings signs of incomplete contact tracing (feature G)
 */
public record NetworkSummary(String networkId, String name, String source, int people, int contacts,
                             List<String> caseIds, int checkIns, List<ValidationIssue> warnings,
                             List<QualityWarning> qualityWarnings, LocalDateTime createdAt) {
}
