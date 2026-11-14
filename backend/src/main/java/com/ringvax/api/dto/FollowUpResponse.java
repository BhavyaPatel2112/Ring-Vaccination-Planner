package com.ringvax.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ringvax.engine.followup.ContactDefinition;

/**
 * The plan as field work: doses ordered by "vaccinate by" deadline (B), and the
 * symptom monitoring list (C).
 */
public record FollowUpResponse(LocalDateTime asOf, int windowHours, int incubationDays, String method,
                               List<Dose> doses, int urgent, int onTime, int pastWindow, int dateUnknown,
                               List<Monitor> monitoring, ContactDefinition.Summary contactFilter) {

    /**
     * @param status    DATE_UNKNOWN, URGENT (24 hours or less), ON_TIME or PAST_WINDOW (monitor instead)
     * @param hoursLeft hours until the deadline (negative when past it), null when the date is unknown
     * @param reason    why the person is in the plan (RING_1, MATCHED, ...)
     */
    public record Dose(int order, String personId, String name, int ring, LocalDate lastExposure,
                       LocalDateTime deadline, Long hoursLeft, String status, String reason, String advice) {
    }

    /** @param reason PAST_WINDOW, CANNOT_BE_VACCINATED, NOT_IN_PLAN or LEFT_OUT_BY_BUDGET */
    public record Monitor(String personId, String name, int ring, LocalDate lastExposure, LocalDate monitorUntil,
                          String reason, String explanation) {
    }
}
