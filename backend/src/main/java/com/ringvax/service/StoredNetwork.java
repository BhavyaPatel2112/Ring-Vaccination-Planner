package com.ringvax.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ringvax.data.CheckIn;
import com.ringvax.data.ConfirmedCase;
import com.ringvax.data.QualityWarning;
import com.ringvax.data.ValidationIssue;
import com.ringvax.engine.graph.ContactGraph;

/**
 * A network loaded into memory with everything read alongside it.
 *
 * @param source "upload", "sample" or "generated"
 */
public record StoredNetwork(String id, String name, String source, ContactGraph graph, List<ConfirmedCase> cases,
                            List<CheckIn> checkIns, List<ValidationIssue> warnings,
                            List<QualityWarning> qualityWarnings, LocalDateTime createdAt) {
}
