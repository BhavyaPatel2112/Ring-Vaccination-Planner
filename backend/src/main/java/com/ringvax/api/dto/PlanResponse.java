package com.ringvax.api.dto;

import java.util.List;

import com.ringvax.engine.followup.ContactDefinition;

/**
 * A vaccination plan ready to display.
 *
 * @param people           doses in priority order, each with the reason (feature D)
 * @param guarantee        plain-language statement of how good the plan is
 * @param provenRatioBound plan size divided by the proven lower bound, or null if unknown
 * @param contactFilter how many contacts the contact settings left out, or null when none were given
 */
public record PlanResponse(String method, int doses, int dosesForFullPlan, Integer budget, boolean complete,
                           boolean budgetLimited, int targetLinks, List<Dose> people, List<LinkRef> unprotectedLinks,
                           List<LinkRef> unprotectableLinks, List<String> ring1NotVaccinated, int lowerBound,
                           Double provenRatioBound, String guarantee, double runtimeMs,
                           ContactDefinition.Summary contactFilter) {

    public record Dose(String personId, String name, String reason, String explanation) {
    }
}
