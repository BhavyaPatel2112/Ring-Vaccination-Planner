package com.ringvax.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of an exposure self-check. Every visit gets two answers: the Bloom
 * filter's quick answer ("possibly exposed" or "not exposed") and the confirmed
 * answer from the exact check-in records. Only the confirmed answer is final.
 * No case is ever named.
 *
 * @param quickCheck "POSSIBLY_EXPOSED" if any visit hit the filter, otherwise "NOT_EXPOSED"
 * @param result     confirmed result: "EXPOSED" or "NOT_EXPOSED"
 */
public record ExposureCheckResponse(String quickCheck, String result, String message, List<VisitResult> visits,
                                    int quickCheckHits, int confirmedExposures, int falseAlarms, FilterStats filter) {

    /**
     * @param quickCheck      "POSSIBLY_EXPOSED" or "NOT_EXPOSED" (Bloom filter)
     * @param hoursChecked    hour slots looked up in the filter
     * @param hoursFlagged    hour slots the filter answered "possibly" for
     * @param confirmed       true if a confirmed case was at the same venue at the same time
     * @param exposureDate    day of the overlap (only when confirmed)
     * @param overlapMinutes  minutes of overlap (only when confirmed)
     */
    public record VisitResult(String venueId, LocalDateTime start, LocalDateTime end, String quickCheck,
                              int hoursChecked, int hoursFlagged, boolean confirmed, LocalDate exposureDate,
                              Long overlapMinutes, String explanation) {
    }
}
