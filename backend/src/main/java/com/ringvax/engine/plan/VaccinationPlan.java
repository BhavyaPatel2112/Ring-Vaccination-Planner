package com.ringvax.engine.plan;

import java.util.List;

import com.ringvax.engine.cover.CoverChoice;

/**
 * A complete vaccination plan: ring 1 doses plus the people chosen to protect
 * the remaining links, possibly cut down to a dose budget.
 *
 * @param method                solver used for the links beyond ring 1 ("APPROX", "GREEDY", "EXACT")
 * @param budget                dose budget, or {@code null} for no limit
 * @param dosesForFullPlan      doses the same method needs to protect everything it can
 * @param choices               people to vaccinate, in priority order, each with a reason
 * @param unprotectedContacts   target links left unprotected because the budget ran out
 * @param unprotectableContacts target links no vaccination can protect (both people excluded)
 * @param ring1NotVaccinated    ring 1 members who should be vaccinated but did not fit in the budget
 * @param lowerBound            proven lower bound on the smallest full plan, or -1 if unknown
 * @param runtimeNanos          time to build the plan
 */
public record VaccinationPlan(String method, Integer budget, int dosesForFullPlan, List<CoverChoice> choices,
                              int[] unprotectedContacts, int[] unprotectableContacts, int[] ring1NotVaccinated,
                              int lowerBound, long runtimeNanos) {

    public VaccinationPlan {
        choices = List.copyOf(choices);
        unprotectedContacts = unprotectedContacts.clone();
        unprotectableContacts = unprotectableContacts.clone();
        ring1NotVaccinated = ring1NotVaccinated.clone();
    }

    /** Number of doses in this plan. */
    public int doses() {
        return choices.size();
    }

    /** People to vaccinate, in priority order. */
    public int[] people() {
        return choices.stream().mapToInt(CoverChoice::person).toArray();
    }

    /** True when every link that vaccination can protect is protected, and all of ring 1 is vaccinated. */
    public boolean complete() {
        return unprotectedContacts.length == 0 && ring1NotVaccinated.length == 0;
    }

    /** True when the budget was smaller than the full plan, so budget mode was used. */
    public boolean budgetLimited() {
        return budget != null && budget < dosesForFullPlan;
    }

    @Override
    public int[] unprotectedContacts() {
        return unprotectedContacts.clone();
    }

    @Override
    public int[] unprotectableContacts() {
        return unprotectableContacts.clone();
    }

    @Override
    public int[] ring1NotVaccinated() {
        return ring1NotVaccinated.clone();
    }
}
