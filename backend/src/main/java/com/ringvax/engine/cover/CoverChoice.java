package com.ringvax.engine.cover;

/**
 * One person in a vaccination plan together with the reason they were chosen,
 * so every dose can be explained to the person and to a supervisor.
 *
 * @param person      person index in the contact graph
 * @param reason      why the person is in the plan
 * @param explanation human-readable sentence, for example "Matched link P011 - P019"
 */
public record CoverChoice(int person, Reason reason, String explanation) {

    public enum Reason {
        /** Direct contact of a confirmed case (ring 1 is always vaccinated). */
        RING_1,
        /** Only person on a link who can be vaccinated; the other cannot. */
        FORCED,
        /** Endpoint of a link picked by the maximal matching (2-approximation). */
        MATCHED,
        /** Picked by the greedy method for covering the most unprotected links. */
        GREEDY,
        /** Part of a minimum cover found by the exact solver. */
        EXACT,
        /** Chosen first under a limited budget because they are among the most connected. */
        BUDGET_PRIORITY
    }
}
